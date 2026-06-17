package com.smartagri.repository;

import com.smartagri.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {

    @Query("SELECT tp FROM TrackPoint tp WHERE tp.machineId = :machineId " +
           "AND tp.recordedAt >= :startOfDay AND tp.recordedAt < :endOfDay " +
           "ORDER BY tp.recordedAt DESC")
    List<TrackPoint> findByMachineIdAndDate(@Param("machineId") String machineId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT tp FROM TrackPoint tp WHERE tp.machineId = :machineId " +
           "AND tp.recordedAt >= :startOfDay AND tp.recordedAt < :endOfDay " +
           "ORDER BY tp.recordedAt DESC LIMIT 1")
    Optional<TrackPoint> findLatestByMachineIdAndDate(@Param("machineId") String machineId,
                                                      @Param("startOfDay") LocalDateTime startOfDay,
                                                      @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT tp FROM TrackPoint tp WHERE tp.machineId = :machineId " +
           "AND tp.recordedAt < :beforeTime " +
           "ORDER BY tp.recordedAt DESC LIMIT 1")
    Optional<TrackPoint> findPreviousPoint(@Param("machineId") String machineId,
                                           @Param("beforeTime") LocalDateTime beforeTime);
}
