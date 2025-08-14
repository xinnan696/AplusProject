package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.EmergencyVehicleEventDto;
import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.mapper.EmergencyVehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ucd.urbanflow.cache.EventStatusCache;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyVehicleService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmergencyVehicleMapper emergencyVehicleMapper;
    private final EventService eventService;

    private static final String SIMULATION_TIME_KEY = "sumo:simulation_time";
    private final EventStatusCache eventStatusCache;


    public List<EmergencyVehicleEvent> getPendingEmergencyEvents() {
        String simTimeStr = redisTemplate.opsForValue().get(SIMULATION_TIME_KEY);
        if (simTimeStr == null) {
            return List.of(); // Return an empty list to avoid NullPointerException
        }
        long currentTime = (long) Double.parseDouble(simTimeStr);
        log.info("[Service] Retrieved current simulation time from Redis: {}", currentTime);
        return emergencyVehicleMapper.findPendingEventsByTriggerTime(currentTime);
    }

    public String processAllEmergencyEvents() {
        log.info("[Service] Starting to process emergency vehicle events...");
        List<EmergencyVehicleEvent> pendingEvents = getPendingEmergencyEvents();
        log.info("[Service] Queried the database and found {} pending events with 'pending' status.", pendingEvents.size());

        if (pendingEvents.isEmpty()) {
            return "No pending events to process";
        }

        int successCount = 0;
        int failCount = 0;

        for (EmergencyVehicleEvent event : pendingEvents) {
            log.info("[Service] Processing event ID: {}, Trigger time: {}", event.getEventId(), event.getTriggerTime());
            try {
                EmergencyVehicleEventDto eventDTO = convertToDto(event);
                boolean sentSuccessfully = eventService.triggerEmergencyVehicleEvent(eventDTO);

                if (sentSuccessfully) {
                    // Only update the status to "triggering" after the command has been sent successfully.
                    updateEventStatus(event.getEventId(), "triggering");
                    successCount++;
                    log.info("Successfully requested processing for emergency vehicle event {}", event.getEventId());
                } else {
                    // If sending fails, do not update the status; the task will be automatically retried in the next cycle.
                    failCount++;
                    log.warn("Failed to send command for event {}, will retry in the next cycle", event.getEventId());
                }

            } catch (Exception e) {
                log.error("Failed to process emergency vehicle event {}: {}", event.getEventId(), e.getMessage(), e);
                // If an exception occurs before conversion or sending, update the status to "failed".
                emergencyVehicleMapper.updateEventStatus(event.getEventId(), "failed");
                failCount++;
            }
        }

        return String.format("Processed %d emergency vehicle events: %d successful, %d failed",
                pendingEvents.size(), successCount, failCount);
    }

    /**
     * A method to update the status of an event.
     * @param eventId The ID of the event to update.
     * @param status The new status string (e.g., "ignored", "completed").
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateEventStatus(String eventId, String status) {
        int updatedRows = emergencyVehicleMapper.updateEventStatus(eventId, status);
        if (updatedRows > 0) {
            log.info("Updated database status for event {} to: {}", eventId, status);
            // Synchronously update the cache
            eventStatusCache.setStatus(eventId, status);
            log.info("Updated cache status for event {} to: {}", eventId, status);
            return true;
        } else {
            log.warn("Failed to update status for event {}, it might not exist", eventId);
            return false;
        }
    }

    public void handleTriggerResult(String eventId, boolean success) {
        String newStatus = success ? "triggered" : "failed";
        int updatedRows = emergencyVehicleMapper.updateEventStatus(eventId, newStatus);
        if (updatedRows > 0) {
            log.info("Updated status for emergency event {} to: {} based on Traci receipt", eventId, newStatus);
        } else {
            log.warn("Failed to update status for emergency event {}, it might not exist", eventId);
        }
    }

    private EmergencyVehicleEventDto convertToDto(EmergencyVehicleEvent event) {
        EmergencyVehicleEventDto dto = new EmergencyVehicleEventDto();
        dto.setEventId(event.getEventId());
        dto.setVehicleId(event.getVehicleId());
        dto.setVehicleType(event.getVehicleType());
        dto.setEventType(event.getEventType());
        dto.setOrganization(event.getOrganization());
        dto.setRouteEdges(event.getRouteEdges());
        dto.setJunctionsOnPath(event.getJunctionsOnPath());
        dto.setSignalizedJunctions(event.getSignalizedJunctions());
        return dto;
    }

    /**
     * Retrieves the static details (agency and path) for an event by its ID.
     * @param eventId The unique ID of the event.
     * @return an Optional containing the event's static details.
     */
    public Optional<EmergencyVehicleStaticDataDto> getEventDetails(String eventId) {
        return emergencyVehicleMapper.findByEventId(eventId);
    }
}
