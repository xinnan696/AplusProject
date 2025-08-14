package com.ucd.urbanflow.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handler for the frontend tracking data WebSocket endpoint
 */
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final TrackingDataPublisher publisher;

    /**
     * This method is called after a new WebSocket connection has been successfully established.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        publisher.addSession(session);
    }

    /**
     * This method is called after a WebSocket connection has been closed.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        publisher.removeSession(session);
    }
}
