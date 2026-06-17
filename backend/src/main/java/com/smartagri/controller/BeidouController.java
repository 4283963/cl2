package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.MachineVO;
import com.smartagri.dto.TrackDataDTO;
import com.smartagri.entity.TrackPoint;
import com.smartagri.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeidouController {

    private final MachineService machineService;

    @PostMapping("/beidou/track")
    public ApiResponse<String> receiveTrackData(@Valid @RequestBody TrackDataDTO dto) {
        machineService.receiveTrackData(dto);
        return ApiResponse.success("轨迹数据接收成功");
    }

    @GetMapping("/machines")
    public ApiResponse<List<MachineVO>> getAllMachines() {
        return ApiResponse.success(machineService.getAllMachines());
    }

    @GetMapping("/machines/{machineId}")
    public ApiResponse<MachineVO> getMachine(@PathVariable String machineId) {
        return machineService.getMachineByMachineId(machineId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "农机不存在"));
    }

    @GetMapping("/machines/{machineId}/track")
    public ApiResponse<List<TrackPoint>> getTodayTrack(@PathVariable String machineId) {
        return ApiResponse.success(machineService.getTodayTrackPoints(machineId));
    }
}
