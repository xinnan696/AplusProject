package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import com.ucd.urbanflow.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * EventResultHandler 的单元测试 (100% 覆盖率版本)
 */
@ExtendWith(MockitoExtension.class)
class EventResultHandlerTest {

    @Mock
    private EventService eventService;
    @Mock
    private EmergencyVehicleService emergencyVehicleService;
    @Mock
    private WebSocketSession mockSession;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EventResultHandler eventResultHandler;

    @Test
    void testHandleTextMessage_EmergencyEventWithAllFields() throws Exception {
        // Arrange: 准备一个包含所有字段的紧急事件JSON
        String jsonPayload = "{\"event_id\":\"evt-123\",\"event_type\":\"emergency_event\",\"status\":\"success\",\"message\":\"Full details\",\"vehicle_ids\":[\"v1\",\"v2\"],\"lane_ids\":[\"l1\",\"l2\"]}";
        TextMessage message = new TextMessage(jsonPayload);

        // Act: 调用消息处理方法
        eventResultHandler.handleTextMessage(mockSession, message);

        // Assert: 验证调用了正确的服务方法，并且 isSuccess 参数为 true
        verify(emergencyVehicleService).handleTriggerResult("evt-123", true);
        verify(eventService, never()).handleEventResult(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testHandleTextMessage_OtherEventWithAllFields() throws Exception {
        // Arrange: 准备一个包含所有字段的“其他”事件JSON
        String jsonPayload = "{\"event_id\":\"evt-456\",\"event_type\":\"special_event_A\",\"status\":\"fail\",\"message\":\"Special details\",\"vehicle_ids\":[\"v3\",\"v4\"],\"lane_ids\":[\"l3\",\"l4\"]}";
        TextMessage message = new TextMessage(jsonPayload);

        // ArgumentCaptor 用于捕获传递给 mock 方法的参数
        ArgumentCaptor<List<String>> vehicleIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> laneIdsCaptor = ArgumentCaptor.forClass(List.class);

        // Act: 调用消息处理方法
        eventResultHandler.handleTextMessage(mockSession, message);

        // Assert: 验证调用了 EventService 的方法
        verify(eventService).handleEventResult(
                eq("fail"),
                eq("special_event_A"),
                eq("Special details"),
                vehicleIdsCaptor.capture(),
                laneIdsCaptor.capture(),
                eq(jsonPayload)
        );
        // 验证捕获到的列表内容是否正确
        assertThat(vehicleIdsCaptor.getValue()).containsExactly("v3", "v4");
        assertThat(laneIdsCaptor.getValue()).containsExactly("l3", "l4");
        // 验证另一个服务未被调用
        verify(emergencyVehicleService, never()).handleTriggerResult(any(), anyBoolean());
    }

    @Test
    void testHandleTextMessage_WithMissingOptionalFields() throws Exception {
        // Arrange: 准备一个只包含 event_type 的JSON，来测试所有三元运算符的 false 分支
        String jsonPayload = "{\"event_type\":\"minimal_event\"}";
        TextMessage message = new TextMessage(jsonPayload);

        // Act: 调用消息处理方法
        eventResultHandler.handleTextMessage(mockSession, message);

        // Assert: 验证 EventService 被以默认值调用
        verify(eventService).handleEventResult(
                eq("fail"),      // status 缺失，应为 "fail"
                eq("minimal_event"),
                eq(""),          // message 缺失，应为 ""
                eq(List.of()),   // vehicle_ids 缺失，应为空列表
                eq(List.of()),   // lane_ids 缺失，应为空列表
                eq(jsonPayload)
        );
        verify(emergencyVehicleService, never()).handleTriggerResult(any(), anyBoolean());
    }

    @Test
    void testHandleTextMessage_WithInvalidJson() {
        // Arrange: 准备一条格式错误的JSON消息
        String invalidJsonPayload = "{\"event_id\": \"evt-123\",";
        TextMessage message = new TextMessage(invalidJsonPayload);

        // Act & Assert: 调用方法，断言它不会抛出异常（因为异常在内部被catch了）
        assertDoesNotThrow(() -> eventResultHandler.handleTextMessage(mockSession, message));
    }

    @Test
    void testAfterConnectionEstablished() {
        // Act & Assert: 调用方法以覆盖
        assertDoesNotThrow(() -> eventResultHandler.afterConnectionEstablished(mockSession));
    }

    @Test
    void testAfterConnectionClosed() {
        // Act & Assert: 调用方法以覆盖
        assertDoesNotThrow(() -> eventResultHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL));
    }
}