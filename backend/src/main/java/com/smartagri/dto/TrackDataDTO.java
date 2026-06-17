package com.smartagri.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrackDataDTO {

    @NotBlank(message = "农机ID不能为空")
    private String machineId;

    @NotNull(message = "纬度不能为空")
    private Double latitude;

    @NotNull(message = "经度不能为空")
    private Double longitude;

    private String workType;

    private Double speed;

    private Double heading;
}
