package com.ucd.urbanflow.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.service.EventSchedulerEvent;
import com.ucd.urbanflow.event.entity.SpecialEventSchedule;
import com.ucd.urbanflow.event.mapper.SpecialEventMapper;
import com.ucd.urbanflow.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件处理服务
 */
@Slf4j
@Service
public class EventProcessingService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private SpecialEventMapper specialEventMapper;
    
    @Autowired
    private EventService eventService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String SIMULATION_TIME_KEY = "sumo:simulation_time";
    
    /**
     * 获取当前需要处理的事件列表
     */
    public List<SpecialEventSchedule> getCurrentEvents() {
        String simTimeStr = redisTemplate.opsForValue().get(SIMULATION_TIME_KEY);
        if (simTimeStr == null) {
            return List.of();
        }
        
        int currentTime = (int) Double.parseDouble(simTimeStr);
        return specialEventMapper.findPendingEventsByTriggerTime(currentTime);
    }
    
    /**
     * 处理所有待触发事件
     */
    public String processAllEvents() {
        List<SpecialEventSchedule> pendingEvents = getCurrentEvents();
        
        if (pendingEvents.isEmpty()) {
            return "No pending events to process";
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (SpecialEventSchedule event : pendingEvents) {
            try {
                // 调用 EventService 触发事件
                EventSchedulerEvent eventDTO = convertToEventDTO(event);
                eventService.triggerEvent(eventDTO);
                
                boolean success = true;
                
                if (success) {
                    specialEventMapper.updateEventStatus(event.getEventId(), "triggered");
                    successCount++;
                    log.info("Event {} processed successfully", event.getEventId());
                } else {
                    failCount++;
                }
                
            } catch (Exception e) {
                log.error("Failed to process event {}: {}", event.getEventId(), e.getMessage());
                failCount++;
            }
        }
        
        return String.format("Processed %d events: %d successful, %d failed", 
                            pendingEvents.size(), successCount, failCount);
    }
    
    /**
     * 将数据库实体转换为 DTO
     */
    private EventSchedulerEvent convertToEventDTO(SpecialEventSchedule schedule) {
        try {
            EventSchedulerEvent event = new EventSchedulerEvent();
            event.setEventID(schedule.getEventId());
            event.setEventType(schedule.getEventType());
            event.setTriggerTime(schedule.getTriggerTime());
            event.setDuration(schedule.getDuration());
            
            // 解析 lane_ids JSON 字符串
            if (schedule.getLaneIds() != null && !schedule.getLaneIds().isEmpty()) {
                List<String> laneIds = objectMapper.readValue(
                    schedule.getLaneIds(), 
                    new TypeReference<List<String>>() {}
                );
                event.setLaneIds(laneIds);
            }
            
            return event;
        } catch (Exception e) {
            log.error("Error converting event entity to DTO: {}", schedule.getEventId(), e);
            throw new RuntimeException("Failed to convert event", e);
        }
    }
}
