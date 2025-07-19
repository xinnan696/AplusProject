package com.ucd.urbanflow.websocket;

import org.java_websocket.WebSocket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // 注入用于处理前端连接的Handler
    private final TrackingWebSocketHandler trackingWebSocketHandler;
    private final EventResultHandler eventResultHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(eventResultHandler, "/ws/result").setAllowedOrigins("*");
        // 注册用于向前端推送实时追踪数据的端点
        registry.addHandler(trackingWebSocketHandler, "/ws/tracking")
                .setAllowedOrigins("*");
    }

//    @Bean
//    public EventResultHandler eventResultHandler() {
//        return new EventResultHandler();
//    }
//    public TrackingWebSocketHandler trackingWebSocketHandler() {
//        return new TrackingWebSocketHandler();
//    }
}
