package com.ucd.urbanflow.controller;


import com.ucd.urbanflow.service.TopCongestedSegmentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RestController("/api/congestedtimes")
@RequestMapping
public class TopCongestedSegmentsController {
    @Autowired
    private TopCongestedSegmentsService topCongestedSegmentsService;

    @GetMapping("/dashboard")
    public Map<String, Object> getTopCongestedSegments(
            @RequestParam String timeRange
    ) {
        return topCongestedSegmentsService.buildDashboardData(timeRange);
    }
}
