package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供与紧急车辆事件相关的API接口。
 * 与 EventController 职责分离，专门负责紧急车辆。
 */
@Slf4j
@RestController
@RequestMapping("/api/emergency-vehicles") // 使用专属的API路径
@RequiredArgsConstructor
public class EmergencyVehicleController {

    private final EmergencyVehicleService emergencyVehicleService;

    /**
     * 检查当前有哪些待处理的紧急车辆事件。
     * 这个接口对于调试和监控非常有用。
     * @return 待处理紧急车辆事件的列表
     */
    @GetMapping("/check")
    public List<EmergencyVehicleEvent> checkEmergencyEvents() {
        log.info("API call: 检查待处理的紧急车辆事件");
        // 复用 Service 中已有的逻辑
        return emergencyVehicleService.getPendingEmergencyEvents();
    }

    /**
     * 手动触发一次对所有待处理紧急车辆事件的处理。
     * 这个接口对于测试或在调度器失效时手动干预非常有用。
     * @return 处理结果的摘要信息
     */
    @PostMapping("/process")
    public String processAllEmergencyEvents() {
        log.info("API call: 手动触发处理所有紧急车辆事件");
        // 复用 Service 中已有的逻辑
        return emergencyVehicleService.processAllEmergencyEvents();
    }
}
