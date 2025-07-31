package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.service.CongestedRoadCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class CongestedRoadCountController {
    @Autowired
    private CongestedRoadCountService congestedRoadCountService;

    @GetMapping("/congestioncount")
    public Map<String, Object> getCongestionRoadCount (
            @RequestParam String timeRange
    ) {
        return congestedRoadCountService.buildDashboardData(timeRange);
    }
}