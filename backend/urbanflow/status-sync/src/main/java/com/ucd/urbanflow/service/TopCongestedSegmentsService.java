package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
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
    @Autowired
    private JunctionRegionsMapper junctionRegionsMapper;

    private static final int TOP_N = 6;

    public Map<String, Object> buildDashboardData(String timeRange, String managedAreas) {

        Date end = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        end = cal.getTime();

        Date start;
        switch (timeRange == null ? "24hours" : timeRange.toLowerCase()) {
            case "oneweek":
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "onemonth":
                cal.add(Calendar.MONTH, -1);
                break;
            case "sixmonths":
                cal.add(Calendar.MONTH, -6);
                break;
            case "oneyear":
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                cal.add(Calendar.HOUR_OF_DAY, -24);
        }
        start = cal.getTime();

        List<String> junctionIdFilter = null;
        if (managedAreas != null && !managedAreas.isEmpty()) {
            junctionIdFilter = junctionRegionsMapper.findJunctionIdsByArea(managedAreas);
            // If the filter is specified but returns no junctions, return an empty result early.
            if (junctionIdFilter.isEmpty()) {
                return createEmptyDashboardResponse();
            }
        }

        List<TopCongestedSegments> stats = topCongestedSegmentsMapper.selectByTimeRange(start, end, junctionIdFilter);


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
            min = 0;
            max = Math.max(max, entry.getValue());
        }
        if (min == Integer.MAX_VALUE) min = 0;
        if (max == Integer.MIN_VALUE) max = 0;
        int interval = (max - min) / 6 > 0 ? (max - min) / 6 : 5;

        Map<String, Object> yAxisConfig = new HashMap<>();
        yAxisConfig.put("min", 0);
        yAxisConfig.put("max", max);
        yAxisConfig.put("interval", interval);

        Map<String, Object> resp = new HashMap<>();
        resp.put("xAxisLabels", xAxisLabels);
        resp.put("yAxisConfig", yAxisConfig);
        resp.put("data", data);
        return resp;
    }

    private Map<String, Object> createEmptyDashboardResponse() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("xAxisLabels", Collections.emptyList());
        Map<String, Object> yAxisConfig = new HashMap<>();
        yAxisConfig.put("min", 0);
        yAxisConfig.put("max", 0);
        yAxisConfig.put("interval", 5);
        resp.put("yAxisConfig", yAxisConfig);
        resp.put("data", Collections.emptyList());
        return resp;
    }
}
