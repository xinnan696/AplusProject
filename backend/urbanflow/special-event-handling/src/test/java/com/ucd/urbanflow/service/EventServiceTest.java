package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.EmergencyVehicleEventDto;
import com.ucd.urbanflow.dto.EventSchedulerEvent;
import com.ucd.urbanflow.websocket.TraCIEventDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private TraCIEventDispatcher dispatcher;

    @InjectMocks
    private EventService eventService;

    @Test
    void testTriggerEmergencyVehicleEvent() {
        // Arrange
        EmergencyVehicleEventDto eventDto = new EmergencyVehicleEventDto();
        eventDto.setEventId("evt-test-123");

        // Act
        eventService.triggerEmergencyVehicleEvent(eventDto);

        // Assert
        verify(dispatcher, times(1)).sendEvent(eventDto);
    }

    @Test
    void testTriggerEvent() {
        // Arrange: 准备一个通用的事件对象
        EventSchedulerEvent event = new EventSchedulerEvent();

        // Act: 调用通用的事件触发方法
        eventService.triggerEvent(event);

        // Assert: 验证 dispatcher 被正确调用
        verify(dispatcher, times(1)).sendEvent(event);
    }

    @Test
    void testHandleEventResult() {
        // Arrange: 准备参数
        String status = "success";
        String eventType = "test_event";
        String msg = "Test message";

        // Act & Assert: 调用方法，断言没有异常抛出
        // 因为此方法只是打印到控制台，我们只需验证它能被成功调用即可覆盖
        assertDoesNotThrow(() ->
                eventService.handleEventResult(status, eventType, msg, Collections.emptyList(), Collections.emptyList(), "{}")
        );
    }
}