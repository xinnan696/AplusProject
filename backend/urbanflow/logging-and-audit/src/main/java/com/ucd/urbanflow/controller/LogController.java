package com.ucd.urbanflow.controller;


import com.ucd.urbanflow.domain.vo.UserLogVO;
import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.SpecialEventLogDTO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import com.ucd.urbanflow.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/auth")
    public ResponseEntity<Void> logAuth(@RequestBody AuthLogDTO dto) {
        logService.recordAuthLog(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/signal-control")
    public ResponseEntity<Void> logSignalControl(@RequestBody SignalControlLogDTO dto) {
        logService.recordSignalControlLog(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/user-permission")
    public ResponseEntity<Void> logUserPermission(@RequestBody UserPermissionLogDTO dto) {
        logService.recordUserPermissionLog(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/special-event")
    public ResponseEntity<Void> logSpecialEvent(@RequestBody SpecialEventLogDTO dto) {
        logService.recordSpecialEventLog(dto);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/userLogs")
    public ResponseEntity<Map<String, Object>> getUserLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<UserLogVO> logs = logService.getAggregatedUserLogs(startDate, endDate);

        Map<LocalDate, List<UserLogVO>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Format the response structure for the frontend
        List<Map<String, Object>> responseData = groupedLogs.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> dateEntry = new LinkedHashMap<>();
                    dateEntry.put("date", entry.getKey().toString());
                    dateEntry.put("logs", entry.getValue());
                    return dateEntry;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("data", responseData);

        return ResponseEntity.ok(response);
    }
}