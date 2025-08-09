package com.ucd.urbanflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto;
import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.service.EmergencyVehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

// 引入 MockMvc 的静态方法，简化代码
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;


/**
 * EmergencyVehicleController 的接口测试
 * 目的: 验证所有紧急车辆相关的API端点的行为。
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // 自动配置MockMvc
class EmergencyVehicleApiTest {

    @Autowired
    private MockMvc mockMvc; // 用于执行HTTP请求

    @Autowired
    private ObjectMapper objectMapper; // 用于对象和JSON之间的转换

    @MockBean // 创建一个Service的模拟实现，并替换掉Spring上下文中的真实Bean
    private EmergencyVehicleService emergencyVehicleService;

    // --- 测试 GET /api/emergency-vehicles/check ---
    @Test
    void testCheckEmergencyEvents_ShouldReturnListOfEvents() throws Exception {
        // Arrange: 准备 - 模拟Service层返回一个包含一个事件的列表
        EmergencyVehicleEvent event = new EmergencyVehicleEvent();
        event.setEventId("evt-check-123");
        event.setOrganization("Dublin Fire Brigade");
        when(emergencyVehicleService.getPendingEmergencyEvents()).thenReturn(List.of(event));

        // Act & Assert: 执行GET请求并验证响应
        mockMvc.perform(get("/api/emergency-vehicles/check"))
                .andExpect(status().isOk()) // 期望 HTTP 状态码为 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 期望内容类型为 JSON
                .andExpect(jsonPath("$", hasSize(1))) // 期望返回的 JSON 数组大小为 1
                .andExpect(jsonPath("$[0].eventId", is("evt-check-123"))); // 期望数组中第一个对象的 eventId 字段值正确
    }

    // --- 测试 POST /api/emergency-vehicles/process ---
    @Test
    void testProcessAllEmergencyEvents_ShouldReturnProcessingSummary() throws Exception {
        // Arrange: 准备 - 模拟Service层返回一个处理结果字符串
        String summary = "Processed 5 emergency vehicle events: 5 successful, 0 failed";
        when(emergencyVehicleService.processAllEmergencyEvents()).thenReturn(summary);

        // Act & Assert: 执行POST请求并验证响应
        mockMvc.perform(post("/api/emergency-vehicles/process"))
                .andExpect(status().isOk())
                .andExpect(content().string(summary)); // 期望响应体内容与模拟的完全一致
    }

    // --- 测试 POST /api/emergency-vehicles/{eventId}/ignore ---
    @Test
    void testIgnoreEvent_WhenEventExists_ShouldReturnOk() throws Exception {
        // Arrange: 准备 - 模拟Service层成功更新了事件状态
        String eventId = "evt-to-ignore";
        when(emergencyVehicleService.updateEventStatus(eventId, "ignored")).thenReturn(true);

        // Act & Assert: 执行POST请求并验证响应
        mockMvc.perform(post("/api/emergency-vehicles/{eventId}/ignore", eventId))
                .andExpect(status().isOk())
                .andExpect(content().string("Event " + eventId + " has been successfully ignored."));
    }

    @Test
    void testIgnoreEvent_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange: 准备 - 模拟Service层因事件不存在而更新失败
        String eventId = "evt-not-found";
        when(emergencyVehicleService.updateEventStatus(eventId, "ignored")).thenReturn(false);

        // Act & Assert: 执行POST请求并验证响应
        mockMvc.perform(post("/api/emergency-vehicles/{eventId}/ignore", eventId))
                .andExpect(status().isNotFound()); // 期望 HTTP 状态码为 404
    }

    // --- 测试 POST /api/emergency-vehicles/{eventId}/complete ---
    @Test
    void testCompleteEvent_WhenEventExists_ShouldReturnOk() throws Exception {
        // Arrange: 准备 - 模拟Service层成功更新了事件状态
        String eventId = "evt-to-complete";
        when(emergencyVehicleService.updateEventStatus(eventId, "completed")).thenReturn(true);

        // Act & Assert: 执行POST请求并验证响应
        mockMvc.perform(post("/api/emergency-vehicles/{eventId}/complete", eventId))
                .andExpect(status().isOk())
                .andExpect(content().string("Event " + eventId + " has been successfully marked as completed."));
    }

    @Test
    void testCompleteEvent_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange: 准备 - 模拟Service层因事件不存在而更新失败
        String eventId = "evt-not-found";
        when(emergencyVehicleService.updateEventStatus(eventId, "completed")).thenReturn(false);

        // Act & Assert: 执行POST请求并验证响应
        mockMvc.perform(post("/api/emergency-vehicles/{eventId}/complete", eventId))
                .andExpect(status().isNotFound()); // 期望 HTTP 状态码为 404
    }

    // --- 测试 GET /api/emergency-vehicles/{eventId} ---
    @Test
    void testGetEventDetails_WhenEventExists_ShouldReturnDetails() throws Exception {
        // Arrange: 准备 - 模拟Service层返回一个包含静态数据的DTO
        String eventId = "evt-details-456";
        EmergencyVehicleStaticDataDto dto = new EmergencyVehicleStaticDataDto();
        dto.setOrganization("An Garda Síochána"); // National Police of Ireland
        dto.setSignalizedJunctions(List.of("junction1", "junction2"));
        when(emergencyVehicleService.getEventDetails(eventId)).thenReturn(Optional.of(dto));

        // Act & Assert: 执行GET请求并验证响应
        mockMvc.perform(get("/api/emergency-vehicles/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.organization", is("An Garda Síochána")))
                .andExpect(jsonPath("$.signalized_junctions", hasSize(2)));
    }

    @Test
    void testGetEventDetails_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange: 准备 - 模拟Service层返回一个空的Optional
        String eventId = "evt-not-found";
        when(emergencyVehicleService.getEventDetails(eventId)).thenReturn(Optional.empty());

        // Act & Assert: 执行GET请求并验证响应
        mockMvc.perform(get("/api/emergency-vehicles/{eventId}", eventId))
                .andExpect(status().isNotFound()); // 期望 HTTP 状态码为 404
    }
}