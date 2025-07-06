package com.ucd.urbanflow.event.service;

import com.ucd.urbanflow.event.entity.SpecialEventSchedule;
import com.ucd.urbanflow.event.mapper.SpecialEventMapper;
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
                // TODO: 调用下一个服务

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
}
