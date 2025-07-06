package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.TopCongestedSegmentsMapper;
import com.ucd.urbanflow.model.TopCongestedSegments;
import com.ucd.urbanflow.model.TrafficFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopCongestedSegmentsService {
    @Autowired
    private TopCongestedSegmentsMapper topCongestedSegmentsMapper;

    private static final int TOP_N = 6;

    public Map<String, Object> buildDashboardData(String timeRange) {
        // 自动算 start/end
        Date end = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date start;
        switch (timeRange == null ? "24 hours" : timeRange.toLowerCase()) {
            case "one week":
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "one month":
                cal.add(Calendar.MONTH, -1);
                break;
            case "six months":
                cal.add(Calendar.MONTH, -6);
                break;
            case "one year":
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                cal.add(Calendar.HOUR_OF_DAY, -24);
        }
        start = cal.getTime();

        List<TopCongestedSegments> stats = topCongestedSegmentsMapper.selectByTimeRange(start, end);


        Map<String, Integer> sumByJunction = new HashMap<>();
        for (TopCongestedSegments s : stats) {
            String name = s.getJunctionName();
            int count = s.getCongestionTimes() == null ? 0 : s.getCongestionTimes();
            sumByJunction.merge(name, count, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(sumByJunction.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (sorted.size() > TOP_N) {
            sorted = sorted.subList(0, TOP_N);
        }


        List<String> xAxisLabels = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : sorted) {
            xAxisLabels.add(entry.getKey());
            Map<String, Object> m = new HashMap<>();
            m.put("junction_name", entry.getKey());
            m.put("congestion_count", entry.getValue());
            data.add(m);
            min = Math.min(min, entry.getValue());
            max = Math.max(max, entry.getValue());
        }
        if (min == Integer.MAX_VALUE) min = 0;
        if (max == Integer.MIN_VALUE) max = 0;
        int interval = (max - min) / 6 > 0 ? (max - min) / 6 : 5;

        Map<String, Object> yAxisConfig = new HashMap<>();
        yAxisConfig.put("min", min);
        yAxisConfig.put("max", max);
        yAxisConfig.put("interval", interval);

        Map<String, Object> resp = new HashMap<>();
        resp.put("xAxisLabels", xAxisLabels);
        resp.put("yAxisConfig", yAxisConfig);
        resp.put("data", data);
        return resp;
    }
}
