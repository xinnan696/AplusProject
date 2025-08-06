package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.CongestedRoadCountMapper;
import com.ucd.urbanflow.model.CongestedRoadCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CongestedRoadCountService.
 * Focuses on verifying the daily aggregation of congested junction counts.
 */
@ExtendWith(MockitoExtension.class)
class CongestedRoadCountServiceTest {

    @Mock
    private CongestedRoadCountMapper congestedRoadCountMapper;

    @InjectMocks
    private CongestedRoadCountService congestedRoadCountService;

    @Test
    void buildDashboardData_shouldAggregateCountsByDayCorrectly() throws Exception {
        // 1. Arrange: Prepare mock data
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date day1 = sdf.parse("2025-07-20");
        Date day2 = sdf.parse("2025-07-21");

        // Two records for the same day to test aggregation
        CongestedRoadCount record1_day1 = new CongestedRoadCount();
        record1_day1.setTimeBucket(day1);
        record1_day1.setCongestedJunctionCount(5);

        CongestedRoadCount record2_day1 = new CongestedRoadCount();
        record2_day1.setTimeBucket(day1);
        record2_day1.setCongestedJunctionCount(10);

        // One record for a different day
        CongestedRoadCount record1_day2 = new CongestedRoadCount();
        record1_day2.setTimeBucket(day2);
        record1_day2.setCongestedJunctionCount(8);

        // which only has two parameters: start and end dates.
        when(congestedRoadCountMapper.selectByTimeRange(any(Date.class), any(Date.class)))
                .thenReturn(Arrays.asList(record1_day1, record2_day1, record1_day2));

        // 2. Act: Call the method being tested
        Map<String, Object> result = congestedRoadCountService.buildDashboardData("oneweek");

        // 3. Assert: Verify the results
        assertNotNull(result);

        // Extract the data and labels from the response
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");
        @SuppressWarnings("unchecked")
        List<String> xAxisLabels = (List<String>) result.get("xAxisLabels");
        assertNotNull(dataList);

        // Find the index for day1 in the labels list
        int day1Index = xAxisLabels.indexOf("2025-07-20");
        assertTrue(day1Index != -1, "Label for 2025-07-20 should exist");

        assertEquals(15, dataList.get(day1Index).get("congested_junction_count")); // 5 + 10 = 15

        // Find the index for day2 and assert its value
        int day2Index = xAxisLabels.indexOf("2025-07-21");
        assertTrue(day2Index != -1, "Label for 2025-07-21 should exist");
        assertEquals(8, dataList.get(day2Index).get("congested_junction_count"));
    }
}