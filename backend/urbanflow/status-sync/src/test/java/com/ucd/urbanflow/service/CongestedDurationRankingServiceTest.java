package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import com.ucd.urbanflow.model.CongestedDurationRanking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CongestedDurationRankingService.
 * It focuses on verifying the aggregation and sorting of congestion duration data.
 */
@ExtendWith(MockitoExtension.class)
class CongestedDurationRankingServiceTest {

    @Mock
    private CongestedDurationRankingMapper congestedDurationRankingMapper;
    @Mock
    private JunctionRegionsMapper junctionRegionsMapper; // Mocking this dependency

    @InjectMocks
    private CongestedDurationRankingService congestedDurationRankingService;

    @Test
    void buildDashboardData_shouldAggregateAndSortDurationsCorrectly() {
        // 1. Arrange: Prepare mock data and define behavior
        // Mock the data that would be returned from the database
        CongestedDurationRanking record1 = new CongestedDurationRanking();
        record1.setJunctionName("Main St");
        record1.setTotalCongestionDurationSeconds(120.5f);

        CongestedDurationRanking record2 = new CongestedDurationRanking();
        record2.setJunctionName("Park Lane");
        record2.setTotalCongestionDurationSeconds(180.0f);

        CongestedDurationRanking record3 = new CongestedDurationRanking();
        record3.setJunctionName("Main St");
        record3.setTotalCongestionDurationSeconds(60.0f);


        when(congestedDurationRankingMapper.selectByTimeRange(any(Date.class), any(Date.class), any()))
                .thenReturn(Arrays.asList(record1, record2, record3));

        // 2. Act: Call the method being tested
        Map<String, Object> result = congestedDurationRankingService.buildDashboardData("24hours", null);

        // 3. Assert: Verify the results
        assertNotNull(result);

        // Verify that the data is correctly aggregated and sorted
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertNotNull(data);
        assertEquals(2, data.size()); // Should be 2 unique junctions

        // "Main St" should be first because its total duration (180.5) is highest
        Map<String, Object> firstJunction = data.get(0);
        assertEquals("Main St", firstJunction.get("junction_name"));
        assertEquals(180.5, (Double) firstJunction.get("total_congestion_duration_seconds"), 0.01);

        // "Park Lane" should be second with its duration (180.0)
        Map<String, Object> secondJunction = data.get(1);
        assertEquals("Park Lane", secondJunction.get("junction_name"));
        assertEquals(180.0, (Double) secondJunction.get("total_congestion_duration_seconds"), 0.01);

        // Verify yAxisLabels are also sorted correctly
        List<String> yAxisLabels = (List<String>) result.get("yAxisLabels");
        assertEquals("Main St", yAxisLabels.get(0));
        assertEquals("Park Lane", yAxisLabels.get(1));
    }
}