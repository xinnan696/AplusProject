/*
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
            // === 原始 edge 和 tls 数据 ===
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
                    System.err.println("[WebSocket] 解析 TLS JSON 出错: " + jsonStr);
                    e.printStackTrace();
                }
            }

            // === 拥堵数据处理 ===
            List<Map<String, Object>> congestedResults = new ArrayList<>();
            try {
                String congestedJson = redisTemplate.opsForValue().get("traffic:cache:top6_congested_junctions");
                if (congestedJson != null) {
                    List<Map<String, Object>> congestedList = objectMapper.readValue(
                            congestedJson,
                            new TypeReference<List<Map<String, Object>>>() {}
                    );

                    for (Map<String, Object> item : congestedList) {
                        String junctionId = item.get("junctionId").toString();       // 原样使用
                        int count = Integer.parseInt(item.get("congestionCount").toString());

                        // name 查不到时 fallback 为 id 自己
                        String name = junctionIdToName.getOrDefault(junctionId, junctionId);

                        Map<String, Object> resultItem = new HashMap<>();
                        resultItem.put("j", name);
                        resultItem.put("q", count);
                        congestedResults.add(resultItem);
                    }
                }
            } catch (Exception e) {
                System.err.println("[WebSocket] 处理 congested 数据出错");
                e.printStackTrace();
            }

            // === 构造广播内容 ===
            Map<String, Object> message = new HashMap<>();
            message.put("edges", edgeData);
            message.put("trafficLights", tlsData);
            message.put("congested", congestedResults);

            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }

        } catch (Exception e) {
            System.err.println("[WebSocket] 广播异常：");
            e.printStackTrace();
        }
    }
}
*/
