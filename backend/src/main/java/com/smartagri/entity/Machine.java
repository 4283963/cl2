package com.smartagri.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public Double getWorkWidth() { return workWidth; }
    public void setWorkWidth(Double workWidth) { this.workWidth = workWidth; }
    public Double getTodayArea() { return todayArea; }
    public void setTodayArea(Double todayArea) { this.todayArea = todayArea; }
    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }
    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }
    public String getCurrentWorkType() { return currentWorkType; }
    public void setCurrentWorkType(String currentWorkType) { this.currentWorkType = currentWorkType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
