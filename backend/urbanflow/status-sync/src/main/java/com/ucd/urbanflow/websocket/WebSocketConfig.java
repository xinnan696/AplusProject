package com.ucd.urbanflow.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StatusWebSocketHandler statusHandler;

    public WebSocketConfig(StatusWebSocketHandler statusHandler) {
        this.statusHandler = statusHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 原生 WebSocket 连接
        registry.addHandler(statusHandler, "/api/status/ws")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");

        registry.addHandler(statusHandler, "/api/status/sockjs")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

