package com.smartagri.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "machine")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false, unique = true, length = 64)
    private String machineId;

    @Column(name = "machine_name", length = 128)
    private String machineName;

    @Column(name = "work_width")
    private Double workWidth = 3.0;

    @Column(name = "today_area")
    private Double todayArea = 0.0;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "current_work_type", length = 32)
    private String currentWorkType;

    @Column(name = "status", length = 16)
    private String status = "OFFLINE";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
