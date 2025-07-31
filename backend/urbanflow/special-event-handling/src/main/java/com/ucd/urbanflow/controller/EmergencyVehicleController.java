package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto; // 导入新的DTO
import java.util.Optional;

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

    /**
     * 【新增API】
     * 用户点击 "Ignore" 时，前端调用此接口。
     * @param eventId 被忽略的事件ID
     * @return 操作结果
     */
    @PostMapping("/{eventId}/ignore")
    public ResponseEntity<String> ignoreEvent(@PathVariable String eventId) {
        log.info("API call: 用户请求忽略事件 {}", eventId);
        boolean success = emergencyVehicleService.updateEventStatus(eventId, "ignored");
        if (success) {
            return ResponseEntity.ok("事件 " + eventId + " 已被成功忽略。");
        }
        // 如果更新失败（例如事件ID不存在），返回404 Not Found
        return ResponseEntity.notFound().build();
    }

    /**
     * 【新增API】
     * 前端完成最后一个路口的干预后，调用此接口来标记事件已完成。
     * @param eventId 已完成追踪的事件ID
     * @return 操作结果
     */
    @PostMapping("/{eventId}/complete")
    public ResponseEntity<String> completeEvent(@PathVariable String eventId) {
        log.info("API call: 用户请求完成事件追踪 {}", eventId);
        boolean success = emergencyVehicleService.updateEventStatus(eventId, "completed");
        if (success) {
            return ResponseEntity.ok("事件 " + eventId + " 已成功标记为完成。");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * ### 修改：现在返回只包含静态数据的DTO ###
     * 根据事件ID获取其静态详细信息 (机构和路径)。
     * @param eventId 事件的唯一ID
     * @return 包含静态详情的JSON对象，如果找不到则返回404
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EmergencyVehicleStaticDataDto> getEventDetails(@PathVariable String eventId) {
        log.info("API call: 获取事件静态详情 {}", eventId);
        Optional<EmergencyVehicleStaticDataDto> eventDetails = emergencyVehicleService.getEventDetails(eventId);
        return eventDetails.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
