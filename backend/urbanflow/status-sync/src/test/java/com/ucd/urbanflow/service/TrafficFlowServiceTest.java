package com.ucd.urbanflow.service;

import com.ucd.urbanflow.model.TrafficFlow;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import com.ucd.urbanflow.mapper.TrafficFlowMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
// [FIX] Import Mockito's argument matchers
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TrafficFlowService.
 * Focuses on verifying the time-based aggregation logic (e.g., by hour).
 */
@ExtendWith(MockitoExtension.class)
class TrafficFlowServiceTest {

    @Mock
    private TrafficFlowMapper trafficFlowMapper;
    @Mock
    private JunctionRegionsMapper junctionRegionsMapper;

    @InjectMocks
    private TrafficFlowService trafficFlowService;

    @Test
    void buildDashboardData_for24Hours_shouldAggregateByHourCorrectly() {
        // 1. Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
        Date time1 = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 9); // 9 AM (same 8-10 bucket)
        Date time2 = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, 14); // 2 PM (14-16 bucket)
        Date time3 = cal.getTime();

        TrafficFlow record1 = new TrafficFlow();
        record1.setTimeBucket(time1);
        record1.setFlowRateHourly(100);

        TrafficFlow record2 = new TrafficFlow();
        record2.setTimeBucket(time2);
        record2.setFlowRateHourly(50);

        TrafficFlow record3 = new TrafficFlow();
        record3.setTimeBucket(time3);
        record3.setFlowRateHourly(200);

        // [FIXED] Use argument matchers to make the stubbing flexible.
        when(trafficFlowMapper.selectByJunctionAndTimeRange(any(Date.class), any(Date.class), any()))
                .thenReturn(Arrays.asList(record1, record2, record3));

        // 2. Act
        Map<String, Object> result = trafficFlowService.buildDashboardData(null, "24hours", null);

        // 3. Assert
        assertNotNull(result);
        List<Integer> data = (List<Integer>) result.get("data");
        List<String> xAxisLabels = (List<String>) result.get("xAxisLabels");

        int hour8Index = xAxisLabels.indexOf("8");
        assertTrue(hour8Index != -1);
        assertEquals(150, data.get(hour8Index)); // 100 + 50 = 150

        int hour14Index = xAxisLabels.indexOf("14");
        assertTrue(hour14Index != -1);
        assertEquals(200, data.get(hour14Index));
    }
}