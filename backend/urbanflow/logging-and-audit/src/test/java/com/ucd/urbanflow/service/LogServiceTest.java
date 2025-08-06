package com.ucd.urbanflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.SpecialEventLogDTO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import com.ucd.urbanflow.mapper.LogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the LogService.
 * We use Mockito to mock dependencies like the mapper.
 */
@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogMapper logMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LogService logService;

    @Test
    void whenRecordAuthLog_thenMapperIsCalled() {
        // 1. Arrange: Set up the test data
        AuthLogDTO testDto = new AuthLogDTO();
        testDto.setAccountNumber("test_user");

        // 2. Act: Call the method we want to test
        logService.recordAuthLog(testDto);

        // 3. Assert: Verify that the correct method was called on our mock dependency
        // This confirms the service correctly delegates the work to the mapper.
        verify(logMapper).insertAuthLog(testDto);
    }

    @Test
    void whenRecordSignalControlLog_thenMapperIsCalled() {
        // Arrange
        SignalControlLogDTO testDto = new SignalControlLogDTO();
        testDto.setJunctionId("J1");
        testDto.setAccountNumber("test_op");

        // Act
        logService.recordSignalControlLog(testDto);

        // Assert
        verify(logMapper).insertSignalControlLog(testDto);
    }

    @Test
    void whenRecordUserPermissionLog_thenMapperIsCalled() {
        // Arrange
        UserPermissionLogDTO testDto = new UserPermissionLogDTO();
        testDto.setAccountNumber("test_admin");
        testDto.setTargetAccount("test_target");

        // Act
        logService.recordUserPermissionLog(testDto);

        // Assert
        verify(logMapper).insertUserPermissionLog(testDto);
    }

    @Test
    void whenRecordSpecialEventLog_thenMapperIsCalledWithSerializedJson() throws JsonProcessingException {
        // Arrange
        SpecialEventLogDTO testDto = new SpecialEventLogDTO();
        testDto.setEventId(101L);
        List<String> lanes = List.of("lane_01", "lane_02");
        testDto.setLaneIds(lanes);
        String expectedJson = "[\"lane_01\",\"lane_02\"]";

        when(objectMapper.writeValueAsString(lanes)).thenReturn(expectedJson);

        // Act
        logService.recordSpecialEventLog(testDto);

        // Assert
        verify(logMapper).insertSpecialEventLog(testDto, expectedJson);
    }

    @Test
    void whenRecordSpecialEventLogWithNullLanes_thenMapperIsCalledWithNullJson() throws JsonProcessingException {
        // Arrange
        SpecialEventLogDTO testDto = new SpecialEventLogDTO();
        testDto.setEventId(102L);
        testDto.setLaneIds(null); // Test the null case

        // Act
        logService.recordSpecialEventLog(testDto);

        // Assert
        verify(logMapper).insertSpecialEventLog(testDto, null);
        // Also verify that the objectMapper was never used
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void whenRecordSpecialEventLogAndSerializationFails_thenMapperIsNotCalled() throws JsonProcessingException {
        // Arrange
        SpecialEventLogDTO testDto = new SpecialEventLogDTO();
        testDto.setEventId(103L);
        List<String> lanes = List.of("lane_01");
        testDto.setLaneIds(lanes);

        when(objectMapper.writeValueAsString(lanes)).thenThrow(new JsonProcessingException("Test Exception"){});

        // Act
        logService.recordSpecialEventLog(testDto);

        // Assert
        verify(logMapper, never()).insertSpecialEventLog(any(), anyString());
    }

}