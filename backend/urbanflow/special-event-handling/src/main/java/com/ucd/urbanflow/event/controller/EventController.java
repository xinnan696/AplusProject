package com.ucd.urbanflow.event.controller;

import com.ucd.urbanflow.event.entity.SpecialEventSchedule;
import com.ucd.urbanflow.event.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/events")
//测试使用这两个接口
public class EventController {
    
    @Autowired
    private EventProcessingService eventProcessingService;
    
    /**
     * 获取当前需要处理的事件列表
     */
    @GetMapping("/check")
    public List<SpecialEventSchedule> checkEvents() {
        return eventProcessingService.getCurrentEvents();
    }
    
    /**
     * 处理所有待触发事件
     */
    @PostMapping("/process")
    public String processAllEvents() {
        return eventProcessingService.processAllEvents();
    }
}
