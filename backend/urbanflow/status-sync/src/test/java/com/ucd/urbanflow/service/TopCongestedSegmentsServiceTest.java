package com.ucd.urbanflow.service;

import com.ucd.urbanflow.model.TopCongestedSegments;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import com.ucd.urbanflow.mapper.TopCongestedSegmentsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TopCongestedSegmentsService.
 * Focuses on verifying the Top N ranking logic without modifying the service class.
 */
@ExtendWith(MockitoExtension.class)
class TopCongestedSegmentsServiceTest {

    @Mock
    private TopCongestedSegmentsMapper topCongestedSegmentsMapper;
    @Mock
    private JunctionRegionsMapper junctionRegionsMapper;

    @InjectMocks
    private TopCongestedSegmentsService topCongestedSegmentsService;

    @Test
    void buildDashboardData_shouldReturnTopNCorrectly() {
        // 1. Arrange
        TopCongestedSegments j1 = new TopCongestedSegments();
        j1.setJunctionName("Junction C"); // Highest
        j1.setCongestionTimes(200);

        TopCongestedSegments j2 = new TopCongestedSegments();
        j2.setJunctionName("Junction D");
        j2.setCongestionTimes(150);

        TopCongestedSegments j3 = new TopCongestedSegments();
        j3.setJunctionName("Junction A");
        j3.setCongestionTimes(100);

        TopCongestedSegments j4 = new TopCongestedSegments();
        j4.setJunctionName("Junction F");
        j4.setCongestionTimes(90);

        TopCongestedSegments j5 = new TopCongestedSegments();
        j5.setJunctionName("Junction E");
        j5.setCongestionTimes(80);

        TopCongestedSegments j6 = new TopCongestedSegments();
        j6.setJunctionName("Junction G"); // 6th highest
        j6.setCongestionTimes(70);

        TopCongestedSegments j7 = new TopCongestedSegments();
        j7.setJunctionName("Junction B"); // 7th, should be excluded
        j7.setCongestionTimes(50);

        when(topCongestedSegmentsMapper.selectByTimeRange(any(Date.class), any(Date.class), any()))
                .thenReturn(Arrays.asList(j1, j2, j3, j4, j5, j6, j7));

        // 2. Act
        Map<String, Object> result = topCongestedSegmentsService.buildDashboardData("24hours", null);

        // 3. Assert
        assertNotNull(result);
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

        assertEquals(6, data.size());

        assertEquals("Junction C", data.get(0).get("junction_name"));
        assertEquals(200, data.get(0).get("congestion_count"));

        assertEquals("Junction G", data.get(5).get("junction_name"));
        assertEquals(70, data.get(5).get("congestion_count"));

        List<String> returnedJunctionNames = data.stream()
                .map(m -> (String) m.get("junction_name"))
                .collect(Collectors.toList());
        assertFalse(returnedJunctionNames.contains("Junction B"));
    }
}