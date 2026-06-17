package com.smartagri.controller;

import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.HourWorkStatsVO;
import com.smartagri.dto.MachineVO;
import com.smartagri.dto.TrackDataDTO;
import com.smartagri.entity.TrackPoint;
import com.smartagri.service.MachineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BeidouController {

    private static final Logger log = LoggerFactory.getLogger(BeidouController.class);

    private final MachineService machineService;

    public BeidouController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping("/beidou/track")
    public ApiResponse<String> receiveTrackData(@Valid @RequestBody TrackDataDTO dto) {
        boolean enqueued = machineService.enqueueTrackData(dto);
        if (enqueued) {
            return ApiResponse.success("已接收");
        } else {
            return ApiResponse.error(503, "系统繁忙,稍后重试");
        }
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("queueSize", machineService.queueSize());
        info.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(info);
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

    @GetMapping("/machines/{machineId}/work-stats")
    public ApiResponse<List<HourWorkStatsVO>> getTodayWorkStats(@PathVariable String machineId) {
        return ApiResponse.success(machineService.getTodayWorkStats(machineId));
    }
}

