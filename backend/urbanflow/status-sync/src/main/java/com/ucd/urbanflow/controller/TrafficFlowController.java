package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.service.TrafficFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/trafficflow")
public class TrafficFlowController {
    @Autowired
    private TrafficFlowService trafficFlowService;

    @GetMapping("/dashborad")
    public Map<String, Object> getDashboardData(
            @RequestParam String junctionId,
            @RequestParam String timeRange
    ) {
        return trafficFlowService.buildDashboardData(junctionId, timeRange);
    }
}
