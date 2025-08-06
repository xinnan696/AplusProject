package com.ucd.urbanflow.service;

import com.ucd.urbanflow.domain.pojo.CongestedDurationRanking;
import com.ucd.urbanflow.domain.pojo.CongestedRoadCount;
import com.ucd.urbanflow.domain.pojo.TopCongestedSegments;
import com.ucd.urbanflow.domain.pojo.TrafficFlow;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;
import com.ucd.urbanflow.mapper.CongestionRoadCountMapper;
import com.ucd.urbanflow.mapper.TopCongestedSegmentsMapper;
import com.ucd.urbanflow.mapper.TrafficFlowMapper;
import com.ucd.urbanflow.repository.TrafficDataPointRepository;
import com.ucd.urbanflow.service.dataprocessing.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalysisService.
 * This class tests the core aggregation logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private TrafficDataPointRepository tsdbRepository;
    @Mock
    private TrafficFlowMapper trafficFlowMapper;
    @Mock
    private CongestionRoadCountMapper congestionRoadCountMapper;
    @Mock
    private TopCongestedSegmentsMapper topCongestedSegmentsMapper;
    @Mock
    private CongestedDurationRankingMapper congestedDurationRankingMapper;

    @InjectMocks
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date mockBaseTime = sdf.parse("2025-01-01T00:00:00");
        ReflectionTestUtils.setField(analysisService, "baseTime", mockBaseTime);
        ReflectionTestUtils.setField(analysisService, "initialBaseTimeStr", "2025-01-01T00:00:00");
    }

    @Test
    void testProcessWindowData_aggregatesAndSavesAllMetrics() {
        // 1. Arrange: Prepare mock data and define mock behavior.
        Date testTimeBucket = new Date();

        // Create a list of test data points for a single window.
        List<TrafficDataPoint> testDataWindow = Arrays.asList(
                // Junction 1: 2 steps, 1 congested
                TrafficDataPoint.builder().junctionId("J1").junctionName("Main St").edgeId("E1").simulationStep(1L).vehicleCount(10).waitingTime(10.0).congested(false).build(),
                TrafficDataPoint.builder().junctionId("J1").junctionName("Main St").edgeId("E2").simulationStep(1L).vehicleCount(5).waitingTime(5.0).congested(false).build(),
                TrafficDataPoint.builder().junctionId("J1").junctionName("Main St").edgeId("E1").simulationStep(2L).vehicleCount(20).waitingTime(50.0).congested(true).build(),
                // Junction 2: 1 step, not congested
                TrafficDataPoint.builder().junctionId("J2").junctionName("Park Lane").edgeId("E3").simulationStep(1L).vehicleCount(8).waitingTime(8.0).congested(false).build()
        );

        // Use ArgumentCaptor to capture the objects passed to our mock mappers.
        ArgumentCaptor<TrafficFlow> trafficFlowCaptor = ArgumentCaptor.forClass(TrafficFlow.class);
        ArgumentCaptor<CongestedRoadCount> roadCountCaptor = ArgumentCaptor.forClass(CongestedRoadCount.class);
        ArgumentCaptor<TopCongestedSegments> topSegmentsCaptor = ArgumentCaptor.forClass(TopCongestedSegments.class);
        ArgumentCaptor<CongestedDurationRanking> durationRankingCaptor = ArgumentCaptor.forClass(CongestedDurationRanking.class);

        // 2. Act: Call the method we want to test.
        ReflectionTestUtils.invokeMethod(analysisService, "processWindowData", testDataWindow, testTimeBucket);

        // 3. Assert: Verify the results.

        // Verify TrafficFlow
        verify(trafficFlowMapper, times(2)).insert(trafficFlowCaptor.capture());
        TrafficFlow j1Flow = trafficFlowCaptor.getAllValues().stream().filter(f -> f.getJunctionId().equals("J1")).findFirst().orElseThrow();
        TrafficFlow j2Flow = trafficFlowCaptor.getAllValues().stream().filter(f -> f.getJunctionId().equals("J2")).findFirst().orElseThrow();
        assertEquals(35, j1Flow.getFlowRateHourly()); // 10 + 5 + 20 = 35
        assertEquals(8, j2Flow.getFlowRateHourly());

        // Verify CongestionJunctionCounts
        verify(congestionRoadCountMapper, times(1)).insert(roadCountCaptor.capture());
        assertEquals(1, roadCountCaptor.getValue().getCongestedJunctionCount());

        // Verify CongestedTimesRanking
        verify(topCongestedSegmentsMapper, times(1)).insert(topSegmentsCaptor.capture());
        assertEquals("Main St", topSegmentsCaptor.getValue().getJunctionName());
        assertEquals(1, topSegmentsCaptor.getValue().getCongestionTimes());

        // Verify CongestionDurationRanking
        verify(congestedDurationRankingMapper, times(2)).insert(durationRankingCaptor.capture());
        CongestedDurationRanking j1Duration = durationRankingCaptor.getAllValues().stream().filter(d -> d.getJunctionName().equals("Main St")).findFirst().orElseThrow();
        assertEquals(50.0f, j1Duration.getTotalCongestionDurationSeconds());
    }
}