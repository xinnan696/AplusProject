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
        log.info("[WebSocket Publisher] 新的前端连接已建立: session ID = {}, 当前总连接数: {}", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("🔌 [WebSocket Publisher] 前端连接已关闭: session ID = {}, 当前总连接数: {}", session.getId(), sessions.size());
    }

    @Scheduled(fixedRate = 5000) // 为了方便观察，暂时改为1秒一次
    public void publishTrackingData() {
        log.info("--- [Publisher] 定时任务开始执行 ---");

        if (sessions.isEmpty()) {
            log.info("[Publisher] 检测到 0 个前端连接，跳过本次推送。");
            return;
        }


        Map<Object, Object> vehicleDataMap = redisTemplate.opsForHash().entries(EMERGENCY_VEHICLES_KEY);

        log.info("[Publisher] 从 Redis (key: {}) 读取到 {} 条车辆数据。", EMERGENCY_VEHICLES_KEY, vehicleDataMap.size());

        if (vehicleDataMap.isEmpty()) {
            // 即使没有数据也要推送，以便前端清空
        }

        Map<Object, Object> filteredVehicleDataMap = vehicleDataMap.entrySet().stream()
                .filter(entry -> {
                    try {
                        // 从JSON字符串中解析出 eventID
                        JsonNode node = objectMapper.readTree((String) entry.getValue());
                        String eventId = node.get("eventID").asText();

                        // 从缓存中获取状态
                        String status = eventStatusCache.getStatus(eventId).orElse("pending"); // 如果缓存没有，默认为pending

                        // 只保留那些不处于 "ignored" 或 "completed" 状态的事件
                        return !"ignored".equals(status) && !"completed".equals(status);
                    } catch (Exception e) {
                        log.warn("解析车辆 {} 的追踪数据失败，将暂时不过滤此条目。", entry.getKey(), e);
                        return true; // 解析失败则暂时不过滤
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("[Publisher] 从Redis读取到 {} 条数据, 过滤后剩余 {} 条有效数据准备推送。", vehicleDataMap.size(), filteredVehicleDataMap.size());

        broadcastMessage(filteredVehicleDataMap);
    }

    // 辅助方法，用于广播消息
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
                    log.error("向 session {} 推送数据失败", session.getId(), e);
                }
                return true;
            });
        } catch (IOException e) {
            log.error("序列化或广播追踪数据失败", e);
        }
    }



}
