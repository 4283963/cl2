package com.smartagri.dto;

public class MachineVO {
    private String machineId;
    private String machineName;
    private Double workWidth;
    private Double todayArea;
    private Double currentLat;
    private Double currentLng;
    private String currentWorkType;
    private String status;
    private String updatedAt;

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
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
