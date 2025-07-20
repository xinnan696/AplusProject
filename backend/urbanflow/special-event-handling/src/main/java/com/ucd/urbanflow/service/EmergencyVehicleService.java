package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.EmergencyVehicleEventDto;
import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.mapper.EmergencyVehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ucd.urbanflow.cache.EventStatusCache;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyVehicleService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmergencyVehicleMapper emergencyVehicleMapper;
    private final EventService eventService;

    private static final String SIMULATION_TIME_KEY = "sumo:simulation_time";
    private final EventStatusCache eventStatusCache;


    public List<EmergencyVehicleEvent> getPendingEmergencyEvents() {
        String simTimeStr = redisTemplate.opsForValue().get(SIMULATION_TIME_KEY);
        if (simTimeStr == null) {
            return List.of(); // 返回空列表，避免NullPointerException
        }
        long currentTime = (long) Double.parseDouble(simTimeStr);
        log.info("[Service] 从Redis获取到当前仿真时间: {}", currentTime);
        return emergencyVehicleMapper.findPendingEventsByTriggerTime(currentTime);
    }

    public String processAllEmergencyEvents() {
        log.info("[Service] 开始处理紧急车辆事件...");
        List<EmergencyVehicleEvent> pendingEvents = getPendingEmergencyEvents();
        log.info("[Service] 查询数据库，找到 {} 个状态为'pending'的待处理事件。", pendingEvents.size());

        if (pendingEvents.isEmpty()) {
            return "No pending events to process";
        }

        int successCount = 0;
        int failCount = 0;

        for (EmergencyVehicleEvent event : pendingEvents) {
            log.info("[Service] 正在处理事件ID: {}, 触发时间: {}", event.getEventId(), event.getTriggerTime());
            try {
                // 1. 将数据库实体转换为用于发送的DTO
                EmergencyVehicleEventDto eventDTO = convertToDto(event);

                // 2. 调用 EventService 委托发送指令
                boolean sentSuccessfully = eventService.triggerEmergencyVehicleEvent(eventDTO);

                if (sentSuccessfully) {
                    // 只有当指令成功发送后，才更新状态为 "triggering"
                    updateEventStatus(event.getEventId(), "triggering");
                    successCount++;
                    log.info("已成功请求处理紧急车辆事件 {}", event.getEventId());
                } else {
                    // 如果发送失败，不更新状态，任务将在下一周期被自动重试
                    failCount++;
                    log.warn("发送事件 {} 指令失败，将在下一周期重试。", event.getEventId());
                }

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

    /**
     * 【新增方法，支持Controller】
     * 通用的事件状态更新方法。
     * @param eventId 要更新的事件ID
     * @param status 新的状态字符串 (e.g., "ignored", "completed")
     * @return 如果更新成功返回 true，否则返回 false
     */
//    public boolean updateEventStatus(String eventId, String status) {
//        int updatedRows = emergencyVehicleMapper.updateEventStatus(eventId, status);
//        if (updatedRows > 0) {
//            log.info("已将事件 {} 的状态更新为: {}", eventId, status);
//            return true;
//        } else {
//            log.warn("尝试更新事件 {} 状态失败，可能事件ID不存在。", eventId);
//            return false;
//        }
//    }

    public boolean updateEventStatus(String eventId, String status) {
        int updatedRows = emergencyVehicleMapper.updateEventStatus(eventId, status);
        if (updatedRows > 0) {
            log.info("已将事件 {} 的数据库状态更新为: {}", eventId, status);
            // 同步更新缓存
            eventStatusCache.setStatus(eventId, status);
            log.info("已将事件 {} 的缓存状态更新为: {}", eventId, status);
            return true;
        } else {
            log.warn("尝试更新事件 {} 状态失败，可能事件ID不存在。", eventId);
            return false;
        }
    }

    public void handleTriggerResult(String eventId, boolean success) {
        String newStatus = success ? "triggered" : "failed";
        updateEventStatus(eventId, newStatus);
    }

    private EmergencyVehicleEventDto convertToDto(EmergencyVehicleEvent event) {
        EmergencyVehicleEventDto dto = new EmergencyVehicleEventDto();
        dto.setEventId(event.getEventId());
        dto.setVehicleId(event.getVehicleId());
        dto.setVehicleType(event.getVehicleType());
        dto.setEventType(event.getEventType());
        dto.setOrganization(event.getOrganization());
        dto.setRouteEdges(event.getRouteEdges());
        dto.setJunctionsOnPath(event.getJunctionsOnPath());
        dto.setSignalizedJunctions(event.getSignalizedJunctions());
        return dto;
    }

    /**
     * ### 修改：现在返回只包含静态数据的DTO ###
     * 根据事件ID获取事件的静态详细信息 (机构和路径)。
     * @param eventId 事件的唯一ID
     * @return 包含事件静态详情的Optional对象
     */
    public Optional<EmergencyVehicleStaticDataDto> getEventDetails(String eventId) {
        return emergencyVehicleMapper.findByEventId(eventId);
    }
}
