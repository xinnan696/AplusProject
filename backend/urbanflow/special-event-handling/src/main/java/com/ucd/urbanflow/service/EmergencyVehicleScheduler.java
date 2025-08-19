package com.ucd.urbanflow.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for emergency vehicle events.
 * This class is responsible for automatically triggering the check and processing
 * of pending emergency vehicle events in the database at a predefined frequency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyVehicleScheduler {

    // Inject the business service that specifically handles emergency vehicle events.
    private final EmergencyVehicleService emergencyVehicleService;

    /**
     * Scheduled task method.
     * Uses the @Scheduled annotation to have Spring Boot execute it automatically.
     * fixedRate = 1000 means this method will be executed again with a fixed delay of
     * 1000 milliseconds (1 second) after the last task started.
     */
    @Scheduled(fixedRate = 1000)
    public void scheduleEmergencyEventProcessing() {
        // Call the method from the business service layer to execute the actual event processing logic.
        try {
            log.info("--- [Scheduler] Checking for emergency vehicle events... ---");
            String result = emergencyVehicleService.processAllEmergencyEvents();
            if (!result.equals("No pending events to process")) {
                log.info("Event processing result: {}", result);
            }
        } catch (Exception e) {
            log.error("Error in scheduled event processing: {}", e.getMessage());
        }
    }
}
