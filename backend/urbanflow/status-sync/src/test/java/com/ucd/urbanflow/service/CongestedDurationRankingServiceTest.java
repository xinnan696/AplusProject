package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;
import com.ucd.urbanflow.model.CongestedDurationRanking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CongestedDurationRankingServiceTest {
    @InjectMocks
    private CongestedDurationRankingService service;

    @Mock
    private CongestedDurationRankingMapper mapper;

    @Test
    void testBuildDashboardData_aggregationCorrect() {
        List<CongestedDurationRanking> mockList = List.of(
                createItem("J1", 100),
                createItem("J2", 200),
                createItem("J1", 50)
        );

        when(mapper.selectByTimeRange(any(), any())).thenReturn(mockList);

        Map<String, Object> result = service.buildDashboardData("24hours");
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

        assertEquals(2, data.size());
        assertTrue(data.stream().anyMatch(m -> m.get("junction_name").equals("J1")
                && ((Double) m.get("total_congestion_duration_seconds")).intValue() == 150));
    }

    private CongestedDurationRanking createItem(String name, Integer duration) {
        CongestedDurationRanking item = new CongestedDurationRanking();
        item.setJunctionName(name);
        item.setTotalCongestionDurationSeconds(duration);
        item.setTimeBucket(new Date());
        return item;
    }
}
