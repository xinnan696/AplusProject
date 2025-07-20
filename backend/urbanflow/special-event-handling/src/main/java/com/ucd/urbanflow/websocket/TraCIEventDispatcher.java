package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.EventSchedulerEvent;
import jakarta.annotation.PostConstruct;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class TraCIEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(TraCIEventDispatcher.class);

    private WebSocketClient client;

    @Autowired
    private final ObjectMapper objectMapper=new ObjectMapper();

    private boolean connected = false;


    @PostConstruct
    public void init() {
        try {
            URI uri = new URI("ws://0.0.0.0:8000/ws/events");
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    connected = true;
                    logger.info("WebSocket connected to TracI!");
                }

                @Override
                public void onMessage(String message) {
                    logger.info("Received message from TracI: {}",message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    logger.warn("WebSocked closed: {}", reason);
                }

                @Override
                public void onError(Exception e) {
                    connected = false;
                    logger.error("Websocket error", e);
                }
            };
            client.connect();
        } catch (Exception e) {
            logger.error("Websocket init error", e);
        }
    }

    public boolean isConnected() {
        return connected && client.isOpen();
    }

    public void sendEvent(Object event) {
        if (!isConnected()) {
            logger.warn("WebSocket not connected!");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            client.send(json);
            logger.info("Sent Event: {}", json);
        } catch (Exception e) {
            logger.error("Failed to sent event", e);
        }
    }

}
