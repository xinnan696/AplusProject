package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.CongestedRoadCountMapper;
import com.ucd.urbanflow.model.CongestedRoadCount;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CongestedRoadCountServiceTest {
    @InjectMocks
    private CongestedRoadCountService service;

    @Mock
    private CongestedRoadCountMapper mapper;

    @Test
    void testBuildDashboardData_bucket24Hours() {
        List<CongestedRoadCount> mockList = List.of(
                createItem(hourDate(2), 3),
                createItem(hourDate(4), 5)
        );

        when(mapper.selectByTimeRange(any(), any())).thenReturn(mockList);

        Map<String, Object> result = service.buildDashboardData("24hours");
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

        assertEquals(12, data.size()); // 0–22 every 2 hours as a bucket
    }

    private CongestedRoadCount createItem(Date time, int count) {
        CongestedRoadCount item = new CongestedRoadCount();
        item.setTimeBucket(time);
        item.setCongestedJunctionCount(count);
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
