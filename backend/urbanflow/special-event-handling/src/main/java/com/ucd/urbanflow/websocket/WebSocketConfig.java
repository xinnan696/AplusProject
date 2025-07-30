package com.ucd.urbanflow.websocket;

import org.java_websocket.WebSocket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TrackingWebSocketHandler trackingWebSocketHandler;
    private final EventResultHandler eventResultHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(eventResultHandler, "/ws/result").setAllowedOrigins("*");
        // Register the endpoint for pushing real-time tracking data to the frontend
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
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
