package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.vo.ApiResponse;
import com.ucd.urbanflow.service.AreaManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
@Slf4j
//@PreAuthorize("hasRole('ADMIN')")
public class AreaController {
    
    private final AreaManagementService areaManagementService;

    @GetMapping("/available")
    public ApiResponse<List<String>> getAvailableAreas() {
        log.info("Fetching available areas");
        List<String> availableAreas = areaManagementService.getAvailableAreas();
        return ApiResponse.success(availableAreas);
    }

    @GetMapping("/occupied")
    public ApiResponse<List<Map<String, Object>>> getOccupiedAreas() {
        log.info("Fetching occupied areas with manager info");
        List<Map<String, Object>> occupiedAreas = areaManagementService.getOccupiedAreasWithManagerInfo();
        return ApiResponse.success(occupiedAreas);
    }

    @GetMapping("/all")
    public ApiResponse<List<String>> getAllAreas() {
        log.info("Fetching all areas");
        List<String> allAreas = Arrays.asList("Left", "Right");
        return ApiResponse.success(allAreas);
    }
}
