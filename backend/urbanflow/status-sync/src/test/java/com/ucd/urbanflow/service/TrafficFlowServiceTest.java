package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.TrafficFlowMapper;
import com.ucd.urbanflow.model.TrafficFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrafficFlowServiceTest {
    @InjectMocks
    private TrafficFlowService service;

    @Mock
    private TrafficFlowMapper mapper;

    @Test
    void testBuildDashboardData_flowGroupedByHour() {
        List<TrafficFlow> mockList = List.of(
                createItem(hourDate(2), 100),
                createItem(hourDate(3), 50)
        );

        when(mapper.selectByJunctionAndTimeRange(eq("J1"), any(), any())).thenReturn(mockList);

        Map<String, Object> result = service.buildDashboardData("J1", "24hours");
        List<Integer> data = (List<Integer>) result.get("data");

        assertEquals(12, data.size()); // 0–22，every 2 hours as a set
        assertTrue(data.stream().anyMatch(v -> v == 100 || v == 50));
    }

    private TrafficFlow createItem(Date time, int flowRate) {
        TrafficFlow item = new TrafficFlow();
        item.setTimeBucket(time);
        item.setFlowRateHourly(flowRate);
        item.setJunctionId("J1");
        return item;
    }

    private Date hourDate(int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
