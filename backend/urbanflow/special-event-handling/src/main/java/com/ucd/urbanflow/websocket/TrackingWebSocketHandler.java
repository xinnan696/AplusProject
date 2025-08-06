package com.ucd.urbanflow.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 前端追踪数据WebSocket端点的处理器。
 * <p>
 * 它的职责非常单一：管理前端WebSocket连接的生命周期，
 * 并将建立的连接会话(session)注册到TrackingDataPublisher中，
 * 或在连接关闭时将其移除。
 */
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final TrackingDataPublisher publisher;

    /**
     * 当一个新的WebSocket连接成功建立后，此方法被调用。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        publisher.addSession(session);
    }

    /**
     * 当一个WebSocket连接关闭后，此方法被调用。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        publisher.removeSession(session);
    }
}
