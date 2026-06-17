package com.smartagri.dto;

import lombok.Data;

@Data
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
}
