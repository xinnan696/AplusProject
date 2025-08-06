package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.TopCongestedSegmentsMapper;
import com.ucd.urbanflow.model.TopCongestedSegments;
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
public class TopCongestedSegmentsServiceTest {
    @InjectMocks
    private TopCongestedSegmentsService service;

    @Mock
    private TopCongestedSegmentsMapper mapper;

    @Test
    void testBuildDashboardData_topNSegments() {
        List<TopCongestedSegments> mockList = List.of(
                createItem("J1", 5),
                createItem("J2", 8),
                createItem("J3", 4),
                createItem("J4", 7),
                createItem("J5", 6),
                createItem("J6", 3),
                createItem("J7", 9)
        );

        when(mapper.selectByTimeRange(any(), any())).thenReturn(mockList);

        Map<String, Object> result = service.buildDashboardData("24hours");
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

        assertEquals(6, data.size()); // 只取 TOP 6
        assertTrue(data.stream().anyMatch(m -> m.get("junction_name").equals("J7")));
    }

    private TopCongestedSegments createItem(String name, int count) {
        TopCongestedSegments item = new TopCongestedSegments();
        item.setJunctionName(name);
        item.setCongestionTimes(count);
        item.setTimeBucket(new Date());
        return item;
    }
}
