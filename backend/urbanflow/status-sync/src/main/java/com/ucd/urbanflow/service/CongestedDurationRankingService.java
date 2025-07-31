package com.ucd.urbanflow.service;


import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;

import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import com.ucd.urbanflow.model.CongestedDurationRanking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.lettuce.core.GeoArgs.Unit.m;

@Service
public class CongestedDurationRankingService {
    @Autowired
    private CongestedDurationRankingMapper congestedDurationRankingMapper;
    @Autowired
    private JunctionRegionsMapper junctionRegionsMapper;

    private static final int TOP_N = 6;

    public Map<String, Object> buildDashboardData(String timeRange, String managedAreas) {
        Date end = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(end);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        end = c.getTime();

        Date start;

        // aggregate by time range
        switch (timeRange == null ? "24hours" : timeRange.toLowerCase()) {
            case "24hours":
                c.add(Calendar.HOUR_OF_DAY, -24);
                break;
            case "oneweek":
                c.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "onemonth":
                c.add(Calendar.MONTH, -1);
                break;
            case "sixmonths":
                c.add(Calendar.MONTH, -6);
                break;
            case "oneyear":
                c.add(Calendar.YEAR, -1);
                break;
            default:
                c.add(Calendar.HOUR_OF_DAY, -24);
        }
        start = c.getTime();

        List<String> junctionIdFilter = null;
        if (managedAreas != null && !managedAreas.isEmpty()) {
            junctionIdFilter = junctionRegionsMapper.findJunctionIdsByArea(managedAreas);
            // If the filter is specified but returns no junctions, we can return an empty result early.
            if (junctionIdFilter.isEmpty()) {
                return createEmptyDashboardResponse(); // Return an empty but valid response structure
            }
        }

        List<CongestedDurationRanking> stats = congestedDurationRankingMapper.selectByTimeRange(start, end, junctionIdFilter);


        Map<String, Double> durationByJunction = new HashMap<>();
        if(!stats.isEmpty()){
            for (CongestedDurationRanking s : stats) {
                String name = s.getJunctionName();
                double val = s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds();
                durationByJunction.put(name, durationByJunction.getOrDefault(name, 0.0) + val);
            }
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(durationByJunction.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        if (sorted.size() > TOP_N) {
            sorted = sorted.subList(0, TOP_N);
        }

        List<String> yAxisLabels = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

        for (Map.Entry<String, Double> entry : sorted) {
            yAxisLabels.add(entry.getKey());
            Map<String, Object> m = new HashMap<>();
            m.put("junction_name", entry.getKey());
            m.put("total_congestion_duration_seconds", entry.getValue());
            data.add(m);
            min = 0;
            max = Math.max(max, entry.getValue());
        }

        if (min == Double.MAX_VALUE) min = 0;
        if (max == Double.MIN_VALUE) max = 0;
        double interval = (max - min) / 6 > 0 ? (max - min) / 6 : 100;

        Map<String, Object> xAxisConfig = new HashMap<>();
        xAxisConfig.put("min", 0);
        xAxisConfig.put("max", max);
        xAxisConfig.put("interval", interval);

        Map<String, Object> resp = new HashMap<>();
        resp.put("yAxisLabels", yAxisLabels);
        resp.put("xAxisConfig", xAxisConfig);
        resp.put("data", data);
        return resp;
    }

    private Map<String, Object> createEmptyDashboardResponse() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("yAxisLabels", Collections.emptyList());
        Map<String, Object> xAxisConfig = new HashMap<>();
        xAxisConfig.put("min", 0);
        xAxisConfig.put("max", 0);
        xAxisConfig.put("interval", 100);
        resp.put("xAxisConfig", xAxisConfig);
        resp.put("data", Collections.emptyList());
        return resp;
    }
}
