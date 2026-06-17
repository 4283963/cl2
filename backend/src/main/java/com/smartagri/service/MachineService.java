package com.smartagri.service;

import com.smartagri.common.StripedLock;
import com.smartagri.config.RetryOnDeadlock;
import com.smartagri.dto.HourWorkStatsVO;
import com.smartagri.dto.MachineVO;
import com.smartagri.dto.TrackDataDTO;
import com.smartagri.entity.Machine;
import com.smartagri.entity.TrackPoint;
import com.smartagri.repository.MachineRepository;
import com.smartagri.repository.TrackPointRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class MachineService {

    private static final Logger log = LoggerFactory.getLogger(MachineService.class);

    private static final double EARTH_RADIUS = 6371000.0;
    private static final double SQM_TO_MU = 0.0015;

    private static final int BATCH_SIZE = 200;
    private static final long FLUSH_INTERVAL_MS = 500L;
    private static final int WORKER_THREADS = 4;

    private final MachineRepository machineRepository;
    private final TrackPointRepository trackPointRepository;

    private final BlockingQueue<TrackDataDTO> queue = new LinkedBlockingQueue<>(65536);
    private final StripedLock stripedLock = new StripedLock(128);
    private final Map<String, TrackPoint> lastPointCache = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService workers;
    private ScheduledExecutorService flushScheduler;

    public MachineService(MachineRepository machineRepository,
                          TrackPointRepository trackPointRepository) {
        this.machineRepository = machineRepository;
        this.trackPointRepository = trackPointRepository;
    }

    @PostConstruct
    public void start() {
        workers = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "track-worker");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < WORKER_THREADS; i++) {
            workers.submit(this::consumeLoop);
        }

        flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "track-flush");
            t.setDaemon(true);
            return t;
        });
        flushScheduler.scheduleWithFixedDelay(
                this::forceFlush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("轨迹异步缓冲处理器启动: workers={}, queueCapacity={}",
                WORKER_THREADS, 65536);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        forceFlush();
        if (workers != null) workers.shutdownNow();
        if (flushScheduler != null) flushScheduler.shutdownNow();
    }

    public boolean enqueueTrackData(TrackDataDTO dto) {
        boolean offered = queue.offer(dto);
        if (!offered) {
            log.warn("轨迹队列已满, 丢弃 machineId={}", dto.getMachineId());
        }
        return offered;
    }

    public long queueSize() {
        return queue.size();
    }

    private final List<TrackDataDTO> pendingBatch = new ArrayList<>();

    private void consumeLoop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                TrackDataDTO dto = queue.poll(200L, TimeUnit.MILLISECONDS);
                if (dto != null) {
                    synchronized (pendingBatch) {
                        pendingBatch.add(dto);
                        if (pendingBatch.size() >= BATCH_SIZE) {
                            flushPending();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("轨迹消费异常", e);
            }
        }
    }

    private void forceFlush() {
        synchronized (pendingBatch) {
            if (!pendingBatch.isEmpty()) {
                flushPending();
            }
        }
    }

    private void flushPending() {
        if (pendingBatch.isEmpty()) return;
        List<TrackDataDTO> batch = new ArrayList<>(pendingBatch);
        pendingBatch.clear();

        Map<String, List<TrackDataDTO>> grouped = batch.stream()
                .collect(Collectors.groupingBy(TrackDataDTO::getMachineId));

        List<Machine> machineUpdates = new ArrayList<>();
        List<TrackPoint> trackPoints = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, List<TrackDataDTO>> entry : grouped.entrySet()) {
            String machineId = entry.getKey();
            List<TrackDataDTO> list = entry.getValue();
            TrackDataDTO last = list.get(list.size() - 1);

            boolean locked = stripedLock.tryLock(machineId, 3000L);
            if (!locked) {
                log.warn("获取分段锁超时 machineId={}, 稍后重试", machineId);
                queue.addAll(list);
                continue;
            }
            try {
                processMachineBatch(machineId, list, last, now, machineUpdates, trackPoints);
            } catch (Exception e) {
                log.error("处理农机批次失败 machineId={}", machineId, e);
                queue.addAll(list);
            } finally {
                stripedLock.unlock(machineId);
            }
        }

        if (!trackPoints.isEmpty() || !machineUpdates.isEmpty()) {
            try {
                persistBatch(trackPoints, machineUpdates);
            } catch (Exception e) {
                log.error("批量持久化失败, 回滚到队列: trackPoints={}, machines={}",
                        trackPoints.size(), machineUpdates.size(), e);
            }
        }
    }

    private void processMachineBatch(String machineId,
                                     List<TrackDataDTO> list,
                                     TrackDataDTO last,
                                     LocalDateTime batchTime,
                                     List<Machine> machineUpdates,
                                     List<TrackPoint> trackPoints) {
        Optional<Machine> optMachine = machineRepository.findByMachineId(machineId);
        Machine machine = optMachine.orElseGet(() -> {
            Machine m = new Machine();
            m.setMachineId(machineId);
            m.setMachineName("农机-" + machineId);
            m.setWorkWidth(3.0);
            m.setTodayArea(0.0);
            m.setStatus("ONLINE");
            return machineRepository.save(m);
        });

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        double widthMeters = machine.getWorkWidth() != null ? machine.getWorkWidth() : 3.0;
        double accumulatedMu = isSameDay(machine.getUpdatedAt(), batchTime)
                ? (machine.getTodayArea() != null ? machine.getTodayArea() : 0.0)
                : 0.0;

        TrackPoint prev = lastPointCache.get(machineId);
        if (prev == null) {
            prev = findLatestTrackInDb(machineId, startOfDay).orElse(null);
        }

        for (TrackDataDTO dto : list) {
            LocalDateTime recordedAt = batchTime;
            TrackPoint tp = new TrackPoint();
            tp.setMachineId(dto.getMachineId());
            tp.setLatitude(dto.getLatitude());
            tp.setLongitude(dto.getLongitude());
            tp.setWorkType(dto.getWorkType());
            tp.setSpeed(dto.getSpeed() != null ? dto.getSpeed() : 0.0);
            tp.setHeading(dto.getHeading() != null ? dto.getHeading() : 0.0);
            tp.setRecordedAt(recordedAt);
            trackPoints.add(tp);

            if (prev != null) {
                double dist = haversine(prev.getLatitude(), prev.getLongitude(),
                        dto.getLatitude(), dto.getLongitude());
                if (dist > 0.1 && dist < 500) {
                    accumulatedMu += dist * widthMeters * SQM_TO_MU;
                }
            }
            prev = tp;
        }

        if (prev != null) {
            lastPointCache.put(machineId, prev);
        }

        machine.setTodayArea(Math.round(accumulatedMu * 100.0) / 100.0);
        machine.setCurrentLat(last.getLatitude());
        machine.setCurrentLng(last.getLongitude());
        machine.setCurrentWorkType(last.getWorkType());
        machine.setStatus("ONLINE");
        machine.setUpdatedAt(batchTime);
        machineUpdates.add(machine);
    }

    private Optional<TrackPoint> findLatestTrackInDb(String machineId, LocalDateTime startOfDay) {
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return trackPointRepository.findLatestByMachineIdAndDate(machineId, startOfDay, endOfDay);
    }

    @RetryOnDeadlock(maxRetries = 3, delayMs = 300)
    @Transactional
    public void persistBatch(List<TrackPoint> trackPoints, List<Machine> machineUpdates) {
        if (!trackPoints.isEmpty()) {
            trackPointRepository.saveAll(trackPoints);
        }
        if (!machineUpdates.isEmpty()) {
            machineRepository.saveAll(machineUpdates);
        }
    }

    @Transactional
    public void receiveTrackData(TrackDataDTO dto) {
        enqueueTrackData(dto);
    }

    public List<MachineVO> getAllMachines() {
        return machineRepository.findAll().stream()
                .map(this::toMachineVO)
                .sorted(Comparator.comparingDouble(MachineVO::getTodayArea).reversed())
                .collect(Collectors.toList());
    }

    public Optional<MachineVO> getMachineByMachineId(String machineId) {
        return machineRepository.findByMachineId(machineId).map(this::toMachineVO);
    }

    public List<TrackPoint> getTodayTrackPoints(String machineId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return trackPointRepository.findByMachineIdAndDate(machineId, startOfDay, endOfDay);
    }

    public List<HourWorkStatsVO> getTodayWorkStats(String machineId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<TrackPoint> points = trackPointRepository
                .findByMachineIdAndDate(machineId, startOfDay, endOfDay)
                .stream()
                .sorted(Comparator.comparing(TrackPoint::getRecordedAt))
                .collect(Collectors.toList());

        int[] workMinutes = new int[24];
        int[] idleMinutes = new int[24];
        final double WORK_SPEED_THRESHOLD = 0.1;

        if (points.size() >= 2) {
            for (int i = 1; i < points.size(); i++) {
                TrackPoint prev = points.get(i - 1);
                TrackPoint curr = points.get(i);
                long diffMs = java.time.Duration.between(
                        prev.getRecordedAt(), curr.getRecordedAt()).toMillis();
                if (diffMs <= 0 || diffMs > 10 * 60 * 1000L) continue;

                double avgSpeed = (prev.getSpeed() + curr.getSpeed()) / 2.0;
                double minutes = diffMs / 60000.0;
                int hour = curr.getRecordedAt().getHour();

                if (avgSpeed >= WORK_SPEED_THRESHOLD) {
                    workMinutes[hour] += (int) Math.round(minutes);
                } else {
                    idleMinutes[hour] += (int) Math.round(minutes);
                }
            }
        }

        List<HourWorkStatsVO> result = new ArrayList<>();
        int currentHour = LocalDateTime.now().getHour();
        for (int h = 0; h <= currentHour; h++) {
            result.add(new HourWorkStatsVO(h, workMinutes[h], idleMinutes[h]));
        }
        return result;
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private boolean isSameDay(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null || d2 == null) return false;
        return d1.toLocalDate().equals(d2.toLocalDate());
    }

    private MachineVO toMachineVO(Machine m) {
        MachineVO vo = new MachineVO();
        vo.setMachineId(m.getMachineId());
        vo.setMachineName(m.getMachineName());
        vo.setWorkWidth(m.getWorkWidth());
        vo.setTodayArea(m.getTodayArea() != null ? Math.round(m.getTodayArea() * 100.0) / 100.0 : 0.0);
        vo.setCurrentLat(m.getCurrentLat());
        vo.setCurrentLng(m.getCurrentLng());
        vo.setCurrentWorkType(m.getCurrentWorkType());
        vo.setStatus(m.getStatus());
        vo.setUpdatedAt(m.getUpdatedAt() != null
                ? m.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        return vo;
    }
}
