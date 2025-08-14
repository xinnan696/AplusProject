package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto; // 导入新的DTO
import java.util.Optional;

import java.util.List;

/**
 * Provides API endpoints related to emergency vehicle events,
 * specifically for handling emergency vehicles.
 */
@Slf4j
@RestController
@RequestMapping("/api/emergency-vehicles") // 使用专属的API路径
@RequiredArgsConstructor
public class EmergencyVehicleController {

    private final EmergencyVehicleService emergencyVehicleService;

    /**
     * Checks for any pending emergency vehicle events.
     * This endpoint is useful for debugging and monitoring.
     * @return A list of pending emergency vehicle events.
     */
    @GetMapping("/check")
    public List<EmergencyVehicleEvent> checkEmergencyEvents() {
        log.info("API call: Checking for pending emergency vehicle events");
        // Reuse existing logic from the Service
        return emergencyVehicleService.getPendingEmergencyEvents();
    }

    /**
     * Manually triggers the processing of all pending emergency vehicle events.
     * This endpoint is useful for testing or for manual intervention if the scheduler fails.
     * @return A summary of the processing results.
     */
    @PostMapping("/process")
    public String processAllEmergencyEvents() {
        log.info("API call: Manually triggering processing for all emergency vehicle events");
        // Reuse existing logic from the Service
        return emergencyVehicleService.processAllEmergencyEvents();
    }

    /**
     * The frontend calls this endpoint when the user clicks "Ignore".
     * @param eventId The ID of the event to be ignored.
     * @return The result of the operation.
     */
    @PostMapping("/{eventId}/ignore")
    public ResponseEntity<String> ignoreEvent(@PathVariable String eventId) {
        log.info("API call: User requested to ignore event {}", eventId);
        boolean success = emergencyVehicleService.updateEventStatus(eventId, "ignored");
        if (success) {
            return ResponseEntity.ok("Event " + eventId + " has been successfully ignored.");
        }
        // If the update fails (e.g., the event ID does not exist), return 404 Not Found
        return ResponseEntity.notFound().build();
    }

    /**
     * The frontend calls this endpoint after completing the intervention at the last intersection
     * to mark the event as completed.
     * @param eventId The ID of the event whose tracking is complete.
     * @return The result of the operation.
     */
    @PostMapping("/{eventId}/complete")
    public ResponseEntity<String> completeEvent(@PathVariable String eventId) {
        log.info("API call: User requested to complete event tracking for {}", eventId);
        boolean success = emergencyVehicleService.updateEventStatus(eventId, "completed");
        if (success) {
            return ResponseEntity.ok("Event " + eventId + " has been successfully marked as completed.");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves the static details (agency and path) for an event by its ID.
     * @param eventId The unique ID of the event.
     * @return A JSON object with the static details, or 404 if not found.
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EmergencyVehicleStaticDataDto> getEventDetails(@PathVariable String eventId) {
        log.info("API call: Getting static details for event {}", eventId);
        Optional<EmergencyVehicleStaticDataDto> eventDetails = emergencyVehicleService.getEventDetails(eventId);
        return eventDetails.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
