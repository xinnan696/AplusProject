package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.model.MapRealtimeData;
import com.ucd.urbanflow.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;

@Component
public class StatusWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private RedisService redisService;

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
            MapRealtimeData data = redisService.getRealtimeData();
            String json = objectMapper.writeValueAsString(data);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

