package com.ucd.urbanflow.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * EmergencyVehicleScheduler 的单元测试
 * 目的: 验证调度器是否会按预期调用业务服务的方法。
 */
@ExtendWith(MockitoExtension.class)
class EmergencyVehicleSchedulerTest {

    @Mock
    private EmergencyVehicleService emergencyVehicleService;

    @InjectMocks
    private EmergencyVehicleScheduler emergencyVehicleScheduler;

    @Test
    void testScheduleEmergencyEventProcessing() {
        // Arrange: 准备 - 模拟 processAllEmergencyEvents 方法的行为
        when(emergencyVehicleService.processAllEmergencyEvents()).thenReturn("Processed 1 event");

        // Act: 手动调用调度方法
        emergencyVehicleScheduler.scheduleEmergencyEventProcessing();

        // Assert: 验证核心业务方法 processAllEmergencyEvents 是否被调用了 1 次
        verify(emergencyVehicleService, times(1)).processAllEmergencyEvents();
    }

    @Test
    void testScheduleEmergencyEventProcessing_HandlesException() {
        // Arrange: 准备 - 模拟业务方法抛出异常
        when(emergencyVehicleService.processAllEmergencyEvents()).thenThrow(new RuntimeException("DB connection failed"));

        // Act & Assert: 调用方法，并断言没有异常被抛出到方法之外（因为内部已经try-catch了）
        assertDoesNotThrow(() -> emergencyVehicleScheduler.scheduleEmergencyEventProcessing());

        // 验证业务方法依然被调用了
        verify(emergencyVehicleService, times(1)).processAllEmergencyEvents();
    }
}