package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;
import com.ucd.urbanflow.service.CongestedDurationRankingService;
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
public class CongestedDurationRankingController {

    @Autowired
    private CongestedDurationRankingService congestedDurationRankingService;

    @GetMapping("/durationranking")
    public Map<String, Object> getCongestedDuration(
            @RequestParam String timeRange
    ) {
        return congestedDurationRankingService.buildDashboardData(timeRange);
    }
}