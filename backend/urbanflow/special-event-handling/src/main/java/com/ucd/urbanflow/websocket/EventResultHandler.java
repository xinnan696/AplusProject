package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.service.EventService;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import org.slf4j.Logger;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;

public class EventResultHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventResultHandler.class);

    @Autowired
    private EventService eventService;
    @Autowired
    private EmergencyVehicleService emergencyVehicleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connected with TraCI, sessionId: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String json = message.getPayload();
        logger.info("Received from TraCI: {}", json);
        try {
            JsonNode node = objectMapper.readTree(json);
            String eventId = node.has("event_id") ? node.get("event_id").asText() : null;
            String status = node.has("status") ? node.get("status").asText() : "fail";
            String eventType = node.has("event_type") ? node.get("event_type").asText() : null;
            String msg = node.has("message") ? node.get("message").asText() : "";

            List<String> vehicleIds = new ArrayList<>();
            if (node.has("vehicle_ids") && node.get("vehicle_ids").isArray()) {
                for (JsonNode idNode : node.get("vehicle_ids")) {
                    vehicleIds.add(idNode.asText());
                }
            }

            List<String> laneIds = new ArrayList<>();
            if (node.has("lane_ids") && node.get("lane_ids").isArray()) {
                for (JsonNode idNode : node.get("lane_ids")) {
                    laneIds.add(idNode.asText());
                }
            }

            boolean isSuccess = "success".equalsIgnoreCase(status);

            // 根据事件类型分发给不同的服务处理
            if ("emergency_event".equalsIgnoreCase(eventType)) {
                emergencyVehicleService.handleTriggerResult(eventId, isSuccess);
            } else {
                // 对于其他特殊事件，调用EventService
                eventService.handleEventResult(status, eventType, msg, vehicleIds, laneIds, json);
            }

        } catch (Exception e) {
            logger.error("Handling Python event result message exceptions", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        logger.info("Connection closed, sessionId: {}", session.getId());
    }
}
