package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.entity.SpecialEventSchedule;
import com.ucd.urbanflow.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/events")

public class EventController {
    
    @Autowired
    private EventProcessingService eventProcessingService;
    

    @GetMapping("/check")
    public List<SpecialEventSchedule> checkEvents() {
        return eventProcessingService.getCurrentEvents();
    }
    

    @PostMapping("/process")
    public String processAllEvents() {
        return eventProcessingService.processAllEvents();
    }}
