package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.cache.EventStatusCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TrackingDataPublisher 的单元测试 (已修复 UnnecessaryStubbingException)
 */
@ExtendWith(MockitoExtension.class)
class TrackingDataPublisherTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private EventStatusCache eventStatusCache;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TrackingDataPublisher publisher;

    // **变更点**: 移除了 @BeforeEach setUp() 方法

    @Test
    void testAddAndRemoveSession() {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");

        // Act & Assert for add
        publisher.addSession(session);
        @SuppressWarnings("unchecked")
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) ReflectionTestUtils.getField(publisher, "sessions");
        assertThat(sessions).hasSize(1);

        // Act & Assert for remove
        publisher.removeSession(session);
        assertThat(sessions).isEmpty();
    }

    @Test
    void testPublishTrackingData_WhenNoSessions() {
        // Act
        publisher.publishTrackingData();
        // Assert
        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    void testPublishTrackingData_WhenRedisIsEmpty() throws IOException {
        // Arrange
        // **变更点**: mock 设置移到了测试方法内部
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        publisher.addSession(session);
        when(hashOperations.entries(anyString())).thenReturn(Collections.emptyMap());

        // Act
        publisher.publishTrackingData();

        // Assert
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{}");
    }

    @Test
    void testPublishTrackingData_WithVariousStatuses() throws IOException {
        // Arrange
        // **变更点**: mock 设置移到了测试方法内部
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        publisher.addSession(session);

        Map<Object, Object> redisData = Map.of(
                "v_active", "{\"eventID\": \"evt-active\"}",
                "v_completed", "{\"eventID\": \"evt-completed\"}",
                "v_pending", "{\"eventID\": \"evt-pending\"}"
        );
        when(hashOperations.entries(anyString())).thenReturn(redisData);

        when(eventStatusCache.getStatus("evt-active")).thenReturn(Optional.of("triggering"));
        when(eventStatusCache.getStatus("evt-completed")).thenReturn(Optional.of("completed"));
        when(eventStatusCache.getStatus("evt-pending")).thenReturn(Optional.empty());

        // Act
        publisher.publishTrackingData();

        // Assert
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("v_active", "v_pending");
        assertThat(payload).doesNotContain("v_completed");
    }

    @Test
    void testPublishTrackingData_HandlesInvalidJsonInRedis() throws IOException {
        // Arrange
        // **变更点**: mock 设置移到了测试方法内部
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        publisher.addSession(session);

        Map<Object, Object> redisData = Map.of(
                "v_valid", "{\"eventID\": \"evt-valid\"}",
                "v_invalid", "this-is-not-json"
        );
        when(hashOperations.entries(anyString())).thenReturn(redisData);
        when(eventStatusCache.getStatus("evt-valid")).thenReturn(Optional.of("triggering"));

        // Act
        publisher.publishTrackingData();

        // Assert
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("v_valid", "v_invalid");
    }

    @Test
    void testBroadcastMessage_HandlesSessionErrors() throws IOException {
        // Arrange
        WebSocketSession openSession = mock(WebSocketSession.class);
        when(openSession.getId()).thenReturn("open");
        when(openSession.isOpen()).thenReturn(true);

        WebSocketSession closedSession = mock(WebSocketSession.class);
        when(closedSession.getId()).thenReturn("closed");
        when(closedSession.isOpen()).thenReturn(false);

        WebSocketSession ioExceptionSession = mock(WebSocketSession.class);
        when(ioExceptionSession.getId()).thenReturn("io_error");
        when(ioExceptionSession.isOpen()).thenReturn(true);
        doThrow(new IOException("Test IO error")).when(ioExceptionSession).sendMessage(any());

        publisher.addSession(openSession);
        publisher.addSession(closedSession);
        publisher.addSession(ioExceptionSession);

        Map<Object, Object> data = Map.of("key", "value");

        // Act
        ReflectionTestUtils.invokeMethod(publisher, "broadcastMessage", data);

        // Assert
        verify(openSession, times(1)).sendMessage(any());
        verify(closedSession, never()).sendMessage(any());
        verify(ioExceptionSession, times(1)).sendMessage(any());

        @SuppressWarnings("unchecked")
        Map<String, WebSocketSession> sessions = (Map<String, WebSocketSession>) ReflectionTestUtils.getField(publisher, "sessions");
        assertThat(sessions).hasSize(1).containsKey("open");
    }

    @Test
    void testBroadcastMessage_HandlesSerializationError() throws JsonProcessingException {
        // Arrange
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        ReflectionTestUtils.setField(publisher, "objectMapper", mockMapper);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test Serialization Error"){});

        // Act & Assert
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(publisher, "broadcastMessage", new Object()));
    }
}