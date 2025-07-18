package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.EmergencyVehicleEventDto;
import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.mapper.EmergencyVehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyVehicleService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmergencyVehicleMapper emergencyVehicleMapper;
    // ### 修改：注入 EventService 而不是 TraCIEventDispatcher ###
    private final EventService eventService;

    private static final String SIMULATION_TIME_KEY = "sumo:simulation_time";


    public List<EmergencyVehicleEvent> getPendingEmergencyEvents() {
        String simTimeStr = redisTemplate.opsForValue().get(SIMULATION_TIME_KEY);
        if (simTimeStr == null) {
            return List.of(); // 返回空列表，避免NullPointerException
        }
        long currentTime = (long) Double.parseDouble(simTimeStr);
        return emergencyVehicleMapper.findPendingEventsByTriggerTime(currentTime);
    }

    public String processAllEmergencyEvents() {
        List<EmergencyVehicleEvent> pendingEvents = getPendingEmergencyEvents();

        if (pendingEvents.isEmpty()) {
            return "No pending events to process";
        }

        int successCount = 0;
        int failCount = 0;

        for (EmergencyVehicleEvent event : pendingEvents) {
            try {
                // 1. 将数据库实体转换为用于发送的DTO
                EmergencyVehicleEventDto eventDTO = convertToDto(event);

                // 2. 调用 EventService 委托发送指令
                eventService.triggerEmergencyVehicleEvent(eventDTO);

                // 3. 乐观地更新数据库状态为 "triggered"，防止重复发送
                //    注意：这里不等待WebSocket回执，与EventProcessingService的行为保持一致
                emergencyVehicleMapper.updateEventStatus(event.getEventId(), "triggered");
                successCount++;
                log.info("Emergency vehicle event {} processed successfully", event.getEventId());

            } catch (Exception e) {
                log.error("Failed to process emergency vehicle event {}: {}", event.getEventId(), e.getMessage(), e);
                // 如果在转换或发送前就发生异常，也可以更新为failed
                emergencyVehicleMapper.updateEventStatus(event.getEventId(), "failed");
                failCount++;
            }
        }

        return String.format("Processed %d emergency vehicle events: %d successful, %d failed",
                pendingEvents.size(), successCount, failCount);
    }

    public void handleTriggerResult(String eventId, boolean success) {
        String newStatus = success ? "triggered" : "failed";
        int updatedRows = emergencyVehicleMapper.updateEventStatus(eventId, newStatus);
        if (updatedRows > 0) {
            log.info("已根据Traci回执更新紧急事件 {} 的状态为: {}", eventId, newStatus);
        } else {
            log.warn("尝试更新紧急事件 {} 状态失败，可能事件ID不存在。", eventId);
        }
    }

    private EmergencyVehicleEventDto convertToDto(EmergencyVehicleEvent event) {
        EmergencyVehicleEventDto dto = new EmergencyVehicleEventDto();
        dto.setEventId(event.getEventId());
        dto.setVehicleId(event.getVehicleId());
        dto.setVehicleType(event.getVehicleType());
        dto.setEventType(event.getEventType());
        dto.setOrganization(event.getOrganization());
        dto.setRouteEdges(event.getRouteEdges());
        dto.setJunctionsOnPath(event.getSignalizedJunctions());
        return dto;
    }
}
