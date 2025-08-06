package com.ucd.urbanflow.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 紧急车辆事件的定时调度器。
 * <p>
 * 这个类负责按照预定的频率，自动触发对数据库中待处理紧急车辆事件的检查和处理流程。
 * 它与处理特殊事件的 EventScheduler 职责分离，使得系统更易于维护和扩展。
 */
@Slf4j
@Component
@RequiredArgsConstructor // 使用Lombok的构造函数注入，这是Spring推荐的最佳实践
public class EmergencyVehicleScheduler {

    // 注入专门处理紧急车辆事件的业务服务
    private final EmergencyVehicleService emergencyVehicleService;

    /**
     * 定时任务方法。
     * 使用 @Scheduled 注解来让 Spring Boot 自动执行。
     * fixedRate = 1000 表示该方法将在上次任务开始执行后，固定延迟1000毫秒（1秒）再次执行。
     */
    @Scheduled(fixedRate = 1000)
    public void scheduleEmergencyEventProcessing() {
        // 调用业务服务层的方法来执行实际的事件处理逻辑
        try {
            log.info("--- [调度器] 正在检查紧急车辆事件... ---");
            String result = emergencyVehicleService.processAllEmergencyEvents();
            if (!result.equals("No pending events to process")) {
                log.info("Event processing result: {}", result);
            }
        } catch (Exception e) {
            log.error("Error in scheduled event processing: {}", e.getMessage());
        }
    }
}
