package com.smartagri.service;

import com.smartagri.dto.MachineVO;
import com.smartagri.dto.TrackDataDTO;
import com.smartagri.entity.Machine;
import com.smartagri.entity.TrackPoint;
import com.smartagri.repository.MachineRepository;
import com.smartagri.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineService {

    private static final double EARTH_RADIUS = 6371000.0;
    private static final double SQM_TO_MU = 0.0015;

    private final MachineRepository machineRepository;
    private final TrackPointRepository trackPointRepository;

    @Transactional
    public void receiveTrackData(TrackDataDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Machine machine = machineRepository.findByMachineId(dto.getMachineId())
                .orElseGet(() -> createNewMachine(dto));

        TrackPoint currentPoint = new TrackPoint();
        currentPoint.setMachineId(dto.getMachineId());
        currentPoint.setLatitude(dto.getLatitude());
        currentPoint.setLongitude(dto.getLongitude());
        currentPoint.setWorkType(dto.getWorkType());
        currentPoint.setSpeed(dto.getSpeed() != null ? dto.getSpeed() : 0.0);
        currentPoint.setHeading(dto.getHeading() != null ? dto.getHeading() : 0.0);
        currentPoint.setRecordedAt(now);
        trackPointRepository.save(currentPoint);

        Optional<TrackPoint> previousPointOpt = trackPointRepository
                .findPreviousPoint(dto.getMachineId(), now);

        if (previousPointOpt.isPresent()) {
            TrackPoint prev = previousPointOpt.get();
            double distance = haversine(prev.getLatitude(), prev.getLongitude(),
                    dto.getLatitude(), dto.getLongitude());
            double widthMeters = machine.getWorkWidth() != null ? machine.getWorkWidth() : 3.0;
            double incrementalAreaSqM = distance * widthMeters;
            double incrementalAreaMu = incrementalAreaSqM * SQM_TO_MU;

            if (isSameDay(machine.getUpdatedAt(), now)) {
                machine.setTodayArea((machine.getTodayArea() != null ? machine.getTodayArea() : 0.0)
                        + incrementalAreaMu);
            } else {
                machine.setTodayArea(incrementalAreaMu);
            }
        } else {
            if (!isSameDay(machine.getUpdatedAt(), now)) {
                machine.setTodayArea(0.0);
            }
        }

        machine.setCurrentLat(dto.getLatitude());
        machine.setCurrentLng(dto.getLongitude());
        machine.setCurrentWorkType(dto.getWorkType());
        machine.setStatus("ONLINE");
        machine.setUpdatedAt(now);
        machineRepository.save(machine);

        log.info("接收北斗轨迹: machineId={}, lat={}, lng={}, workType={}, 增量面积={}",
                dto.getMachineId(), dto.getLatitude(), dto.getLongitude(),
                dto.getWorkType(), machine.getTodayArea());
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

    private Machine createNewMachine(TrackDataDTO dto) {
        Machine machine = new Machine();
        machine.setMachineId(dto.getMachineId());
        machine.setMachineName("农机-" + dto.getMachineId());
        machine.setWorkWidth(3.0);
        machine.setTodayArea(0.0);
        machine.setStatus("ONLINE");
        return machine;
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
