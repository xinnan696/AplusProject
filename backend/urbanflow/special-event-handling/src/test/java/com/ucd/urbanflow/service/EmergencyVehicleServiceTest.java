package com.ucd.urbanflow.service;

import com.ucd.urbanflow.cache.EventStatusCache;
import com.ucd.urbanflow.dto.EmergencyVehicleEventDto;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto;
import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.mapper.EmergencyVehicleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EmergencyVehicleService 的单元测试
 * 目的: 隔离测试服务层的业务逻辑，验证其与外部依赖（DB, Redis, Cache）的交互是否正确。
 */
@ExtendWith(MockitoExtension.class) // 启用 Mockito 扩展
class EmergencyVehicleServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private EmergencyVehicleMapper emergencyVehicleMapper;
    @Mock
    private EventService eventService;
    @Mock
    private EventStatusCache eventStatusCache;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EmergencyVehicleService emergencyVehicleService;

    @Test
    void testGetPendingEmergencyEvents_Success() {
        String eventId = "evt-004";
        EmergencyVehicleEvent event = new EmergencyVehicleEvent();
        event.setEventId(eventId);
        List<EmergencyVehicleEvent> expectedEvents = List.of(event);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn("1234.56");
        when(emergencyVehicleMapper.findPendingEventsByTriggerTime(1234L)).thenReturn(expectedEvents);

        List<EmergencyVehicleEvent> actualEvents = emergencyVehicleService.getPendingEmergencyEvents();

        assertFalse(actualEvents.isEmpty());
        assertEquals(1, actualEvents.size());
        verify(emergencyVehicleMapper).findPendingEventsByTriggerTime(1234L);
    }

    @Test
    void testHandleTriggerResult_WhenTriggerFails() {
        String eventId = "evt-005";
        String expectedStatus = "failed";
        when(emergencyVehicleMapper.updateEventStatus(eventId, expectedStatus)).thenReturn(1);

        emergencyVehicleService.handleTriggerResult(eventId, false);

        verify(emergencyVehicleMapper).updateEventStatus(eventId, expectedStatus);
    }

    @Test
    void testHandleTriggerResult_WhenTriggerFails_And_UpdateFails() {
        String eventId = "evt-006";
        String expectedStatus = "failed";
        when(emergencyVehicleMapper.updateEventStatus(eventId, expectedStatus)).thenReturn(0);

        emergencyVehicleService.handleTriggerResult(eventId, false);

        verify(emergencyVehicleMapper).updateEventStatus(eventId, expectedStatus);
    }

    @Test
    void testGetEventDetails_WhenNotFound() {
        String eventId = "evt-not-found";
        when(emergencyVehicleMapper.findByEventId(eventId)).thenReturn(Optional.empty());

        Optional<EmergencyVehicleStaticDataDto> result = emergencyVehicleService.getEventDetails(eventId);

        assertTrue(result.isEmpty());
    }

    // ===================================================================================
    // 原有的测试用例（保持不变）
    // ===================================================================================

    @Test
    void testProcessAllEmergencyEvents_WhenNoPendingEvents() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn("1000.0");
        when(emergencyVehicleMapper.findPendingEventsByTriggerTime(anyLong())).thenReturn(Collections.emptyList());

        String result = emergencyVehicleService.processAllEmergencyEvents();

        assertEquals("No pending events to process", result);
        verify(eventService, never()).triggerEmergencyVehicleEvent(any());
    }

    @Test
    void testProcessAllEmergencyEvents_WithPendingEvents_Success() {
        EmergencyVehicleEvent event = new EmergencyVehicleEvent();
        event.setEventId("evt-001");
        List<EmergencyVehicleEvent> pendingEvents = List.of(event);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn("1000.0");
        when(emergencyVehicleMapper.findPendingEventsByTriggerTime(1000L)).thenReturn(pendingEvents);
        when(eventService.triggerEmergencyVehicleEvent(any(EmergencyVehicleEventDto.class))).thenReturn(true);
        when(emergencyVehicleMapper.updateEventStatus("evt-001", "triggering")).thenReturn(1);

        String result = emergencyVehicleService.processAllEmergencyEvents();

        assertEquals("Processed 1 emergency vehicle events: 1 successful, 0 failed", result);
        verify(emergencyVehicleMapper).updateEventStatus("evt-001", "triggering");
        verify(eventStatusCache).setStatus("evt-001", "triggering");
    }

    @Test
    void testUpdateEventStatus_Success() {
        String eventId = "evt-002";
        String status = "ignored";
        when(emergencyVehicleMapper.updateEventStatus(eventId, status)).thenReturn(1);

        boolean success = emergencyVehicleService.updateEventStatus(eventId, status);

        assertTrue(success);
        verify(emergencyVehicleMapper).updateEventStatus(eventId, status);
        verify(eventStatusCache).setStatus(eventId, status);
    }

    @Test
    void testUpdateEventStatus_Failure_EventNotFound() {
        String eventId = "evt-non-existent";
        String status = "ignored";
        when(emergencyVehicleMapper.updateEventStatus(eventId, status)).thenReturn(0);

        boolean success = emergencyVehicleService.updateEventStatus(eventId, status);

        assertFalse(success);
        verify(eventStatusCache, never()).setStatus(anyString(), anyString());
    }

    @Test
    void testGetEventDetails() {
        String eventId = "evt-003";
        EmergencyVehicleStaticDataDto dto = new EmergencyVehicleStaticDataDto();
        dto.setOrganization("Fire Dept.");
        when(emergencyVehicleMapper.findByEventId(eventId)).thenReturn(Optional.of(dto));

        Optional<EmergencyVehicleStaticDataDto> result = emergencyVehicleService.getEventDetails(eventId);

        assertTrue(result.isPresent());
        assertEquals("Fire Dept.", result.get().getOrganization());
    }

    @Test
    void testGetPendingEmergencyEvents_WhenSimTimeIsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn(null);

        List<EmergencyVehicleEvent> events = emergencyVehicleService.getPendingEmergencyEvents();

        assertTrue(events.isEmpty());
        verify(emergencyVehicleMapper, never()).findPendingEventsByTriggerTime(anyLong());
    }

    @Test
    void testProcessAllEmergencyEvents_WhenSendFails() {
        EmergencyVehicleEvent event = new EmergencyVehicleEvent();
        event.setEventId("evt-001");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn("1000.0");
        when(emergencyVehicleMapper.findPendingEventsByTriggerTime(1000L)).thenReturn(List.of(event));
        when(eventService.triggerEmergencyVehicleEvent(any(EmergencyVehicleEventDto.class))).thenReturn(false);

        String result = emergencyVehicleService.processAllEmergencyEvents();

        assertEquals("Processed 1 emergency vehicle events: 0 successful, 1 failed", result);
        verify(emergencyVehicleMapper, never()).updateEventStatus(anyString(), anyString());
    }

    @Test
    void testProcessAllEmergencyEvents_CatchesException() {
        EmergencyVehicleEvent event = new EmergencyVehicleEvent();
        event.setEventId("evt-001");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sumo:simulation_time")).thenReturn("1000.0");
        when(emergencyVehicleMapper.findPendingEventsByTriggerTime(1000L)).thenReturn(List.of(event));
        when(eventService.triggerEmergencyVehicleEvent(any(EmergencyVehicleEventDto.class))).thenThrow(new RuntimeException("Network Error"));

        String result = emergencyVehicleService.processAllEmergencyEvents();

        assertEquals("Processed 1 emergency vehicle events: 0 successful, 1 failed", result);
        verify(emergencyVehicleMapper).updateEventStatus("evt-001", "failed");
    }

    @Test
    void testHandleTriggerResult_WhenUpdateFails() {
        String eventId = "evt-non-existent";
        when(emergencyVehicleMapper.updateEventStatus(eventId, "triggered")).thenReturn(0);

        emergencyVehicleService.handleTriggerResult(eventId, true);

        verify(emergencyVehicleMapper).updateEventStatus(eventId, "triggered");
    }

    @Test
    void testHandleTriggerResult_SuccessPath() {
        String eventId = "evt-existent";
        String expectedStatus = "triggered";
        when(emergencyVehicleMapper.updateEventStatus(eventId, expectedStatus)).thenReturn(1);

        emergencyVehicleService.handleTriggerResult(eventId, true);

        verify(emergencyVehicleMapper).updateEventStatus(eventId, expectedStatus);
    }
}