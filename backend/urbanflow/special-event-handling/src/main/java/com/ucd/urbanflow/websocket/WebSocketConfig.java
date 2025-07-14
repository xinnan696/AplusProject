package com.ucd.urbanflow.websocket;

import org.java_websocket.WebSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(eventResultHandler(), "/ws/result").setAllowedOrigins("*");
    }

    @Bean
    public EventResultHandler eventResultHandler() {
        return new EventResultHandler();
    }
}
