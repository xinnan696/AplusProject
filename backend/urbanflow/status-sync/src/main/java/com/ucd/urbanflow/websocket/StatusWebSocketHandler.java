package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.*;

@Component
public class StatusWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public StatusWebSocketHandler() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::broadcastStatus, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    private void broadcastStatus() {
        try {
            if (redisTemplate == null) {
                return;
            }

            Map<String, String> edgeData = new HashMap<>();
            Map<String, String> tlsData = new HashMap<>();

            Map<Object, Object> edgeMap = redisTemplate.opsForHash().entries("sumo:edge");
            Map<Object, Object> tlsMap = redisTemplate.opsForHash().entries("sumo:tls");

            for (Map.Entry<Object, Object> entry : edgeMap.entrySet()) {
                edgeData.put(entry.getKey().toString(), entry.getValue().toString());
            }

            for (Map.Entry<Object, Object> entry : tlsMap.entrySet()) {
                tlsData.put(entry.getKey().toString(), entry.getValue().toString());
            }

            // === 构建 junctionId → junctionName 映射（保留原始 ID） ===
            Map<String, String> junctionIdToName = new HashMap<>();
            for (Map.Entry<Object, Object> entry : tlsMap.entrySet()) {
                String jsonStr = entry.getValue().toString();
                try {
                    JsonNode node = objectMapper.readTree(jsonStr);
                    String junctionId = node.get("junction_id").asText();     // 原样保留
                    String junctionName = node.get("junction_name").asText();
                    junctionIdToName.put(junctionId, junctionName);
                } catch (Exception e) {
                }
            }

            List<Map<String, Object>> congestedResults = new ArrayList<>();
            try {
                String congestedJson = redisTemplate.opsForValue().get("traffic:cache:top6_congested_junctions");
                if (congestedJson != null) {
                    List<Map<String, Object>> congestedList = objectMapper.readValue(
                            congestedJson,
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    for (Map<String, Object> item : congestedList) {
                        String junctionId = item.get("junctionId").toString();
                        int count = Integer.parseInt(item.get("congestionCount").toString());


                        String name = junctionIdToName.getOrDefault(junctionId, junctionId);

                        Map<String, Object> resultItem = new HashMap<>();
                        resultItem.put("j", name);
                        resultItem.put("q", count);
                        congestedResults.add(resultItem);
                    }
                }
            } catch (Exception e) {
            }

            Map<String, Object> emergencyVehicles = new HashMap<>();
            try {
                Map<Object, Object> emergencyMap = redisTemplate.opsForHash().entries("sumo:emergency_vehicles");
                
                for (Map.Entry<Object, Object> entry : emergencyMap.entrySet()) {
                    String vehicleId = entry.getKey().toString();
                    String vehicleDataJson = entry.getValue().toString();
                    
                    try {
                        JsonNode vehicleNode = objectMapper.readTree(vehicleDataJson);

                        Map<String, Object> vehicleInfo = new HashMap<>();
                        vehicleInfo.put("eventID", vehicleNode.get("eventID").asText());
                        vehicleInfo.put("vehicleID", vehicleNode.get("vehicleID").asText());
                        vehicleInfo.put("organization", vehicleNode.get("organization").asText());
                        vehicleInfo.put("currentEdgeID", vehicleNode.get("currentEdgeID").asText());
                        vehicleInfo.put("upcomingJunctionID", vehicleNode.get("upcomingJunctionID").asText(""));
                        vehicleInfo.put("nextEdgeID", vehicleNode.get("nextEdgeID").asText(""));
                        vehicleInfo.put("upcomingTlsID", vehicleNode.get("upcomingTlsID").asText(""));
                        vehicleInfo.put("upcomingTlsState", vehicleNode.get("upcomingTlsState").asText(""));
                        vehicleInfo.put("upcomingTlsCountdown", vehicleNode.get("upcomingTlsCountdown").asDouble(0.0));

                        JsonNode positionNode = vehicleNode.get("position");
                        if (positionNode != null) {
                            Map<String, Object> position = new HashMap<>();
                            position.put("x", positionNode.get("x").asDouble());
                            position.put("y", positionNode.get("y").asDouble());
                            position.put("timestamp", positionNode.get("timestamp").asDouble());
                            vehicleInfo.put("position", position);
                        }
                        
                        emergencyVehicles.put(vehicleId, vehicleInfo);
                        
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }

            Map<String, Object> message = new HashMap<>();
            message.put("edges", edgeData);
            message.put("trafficLights", tlsData);
            message.put("congested", congestedResults);
            message.put("emergencyVehicles", emergencyVehicles);

            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }

        } catch (Exception e) {
        }
    }
}
