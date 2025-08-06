package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.ucd.urbanflow.cache.EventStatusCache;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingDataPublisher {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventStatusCache eventStatusCache;

    private static final String EMERGENCY_VEHICLES_KEY = "sumo:emergency_vehicles";

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("[WebSocket Publisher] New frontend connection established: session ID = {}, current total connections: {}", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("🔌 [WebSocket Publisher] Frontend connection closed: session ID = {}, current total connections: {}", session.getId(), sessions.size());
    }

    @Scheduled(fixedRate = 5000)
    public void publishTrackingData() {
        log.info("--- [Publisher] Scheduled task started ---");

        if (sessions.isEmpty()) {
            log.info("[Publisher] Detected 0 frontend connections, skipping this push");
            return;
        }


        Map<Object, Object> vehicleDataMap = redisTemplate.opsForHash().entries(EMERGENCY_VEHICLES_KEY);

        log.info("[Publisher] Read {} vehicle data entries from Redis (key: {})", EMERGENCY_VEHICLES_KEY, vehicleDataMap.size());

        if (vehicleDataMap.isEmpty()) {
            // Push even if there's no data, so the frontend can clear its display.
        }

        Map<Object, Object> filteredVehicleDataMap = vehicleDataMap.entrySet().stream()
                .filter(entry -> {
                    try {
                        // Parse the eventID from the JSON string
                        JsonNode node = objectMapper.readTree((String) entry.getValue());
                        String eventId = node.get("eventID").asText();
                        String status = eventStatusCache.getStatus(eventId).orElse("pending");
                        // Only keep events that are not in "ignored" or "completed" status
                        return !"ignored".equals(status) && !"completed".equals(status);
                    } catch (Exception e) {
                        log.warn("Failed to parse tracking data for vehicle {}, this entry will not be filtered for now", entry.getKey(), e);
                        return true;
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("[Publisher] Read {} entries from Redis, after filtering, {} valid entries are ready to be pushed.", vehicleDataMap.size(), filteredVehicleDataMap.size());

        broadcastMessage(filteredVehicleDataMap);
    }

    // Helper method for broadcasting messages
    private void broadcastMessage(Object data) {
        try {
            String payload = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(payload);

            sessions.values().removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                        return false;
                    }
                } catch (IOException e) {
                    log.error("Failed to push data to session {}", session.getId(), e);
                }
                return true;
            });
        } catch (IOException e) {
            log.error("Failed to serialize or broadcast tracking data", e);
        }
    }



}
