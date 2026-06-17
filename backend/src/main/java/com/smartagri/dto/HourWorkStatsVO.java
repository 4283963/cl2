package com.smartagri.dto;

public class HourWorkStatsVO {
    private int hour;
    private int workMinutes;
    private int idleMinutes;

    public HourWorkStatsVO() {}

    public HourWorkStatsVO(int hour, int workMinutes, int idleMinutes) {
        this.hour = hour;
        this.workMinutes = workMinutes;
        this.idleMinutes = idleMinutes;
    }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public int getWorkMinutes() { return workMinutes; }
    public void setWorkMinutes(int workMinutes) { this.workMinutes = workMinutes; }
    public int getIdleMinutes() { return idleMinutes; }
    public void setIdleMinutes(int idleMinutes) { this.idleMinutes = idleMinutes; }
}
