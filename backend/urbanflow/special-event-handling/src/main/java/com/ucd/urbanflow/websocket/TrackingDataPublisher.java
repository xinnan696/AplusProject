package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingDataPublisher {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMERGENCY_VEHICLES_KEY = "sumo:emergency_vehicles";

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("✅ [WebSocket Publisher] 新的前端连接已建立: session ID = {}, 当前总连接数: {}", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("🔌 [WebSocket Publisher] 前端连接已关闭: session ID = {}, 当前总连接数: {}", session.getId(), sessions.size());
    }

    @Scheduled(fixedRate = 1000) // 为了方便观察，暂时改为1秒一次
    public void publishTrackingData() {
        // ### 新增日志 1 ###
        log.info("--- [Publisher] 定时任务开始执行 ---");

        if (sessions.isEmpty()) {
            // ### 新增日志 2 ###
            log.info("[Publisher] 检测到 0 个前端连接，跳过本次推送。");
            return;
        }

        try {
            Map<Object, Object> vehicleDataMap = redisTemplate.opsForHash().entries(EMERGENCY_VEHICLES_KEY);

            // ### 新增日志 3 ###
            log.info("[Publisher] 从 Redis (key: {}) 读取到 {} 条车辆数据。", EMERGENCY_VEHICLES_KEY, vehicleDataMap.size());

            if (vehicleDataMap.isEmpty()) {
                // 即使没有数据也要推送，以便前端清空
            }

            String payload = objectMapper.writeValueAsString(vehicleDataMap);
            TextMessage message = new TextMessage(payload);

            // ### 新增日志 4 ###
            log.info("[Publisher] 准备向 {} 个前端连接推送数据: {}", sessions.size(), payload);

            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("向 session {} 推送数据时发生IO异常", session.getId(), e);
                }
            });
        } catch (Exception e) {
            log.error("[Publisher] 推送数据时发生未知异常", e);
        }
    }
}
