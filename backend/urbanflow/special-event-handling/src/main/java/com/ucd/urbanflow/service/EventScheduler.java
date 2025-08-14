package com.ucd.urbanflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Event Scheduling Service
 */
@Slf4j
@Service
public class EventScheduler {
    
    @Autowired
    private EventProcessingService eventProcessingService;
    
    /**
     * Periodically checks for events - executes once per second
     */
    @Scheduled(fixedRate = 1000)
    public void checkAndProcessEvents() {
        try {
            String result = eventProcessingService.processAllEvents();
            if (!result.equals("No pending events to process")) {
                log.info("Event processing result: {}", result);
            }
        } catch (Exception e) {
            log.error("Error in scheduled event processing: {}", e.getMessage());
        }
    }
}