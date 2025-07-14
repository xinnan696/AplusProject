package com.ucd.urbanflow.service;


import com.ucd.urbanflow.mapper.CongestedDurationRankingMapper;

import com.ucd.urbanflow.model.CongestedDurationRanking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CongestedDurationRankingService {
    @Autowired
    private CongestedDurationRankingMapper congestedDurationRankingMapper;


    public Map<String, Object> buildDashboardData(String timeRange) {
        Date end = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(end);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Date start;

        // aggregate by time range
        switch (timeRange == null ? "24hours" : timeRange.toLowerCase()) {
            case "24 hours":
                c.add(Calendar.HOUR_OF_DAY, -24);
                break;
            case "one week":
                c.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "one month":
                c.add(Calendar.MONTH, -1);
                break;
            case "six months":
                c.add(Calendar.MONTH, -6);
                break;
            case "one year":
                c.add(Calendar.YEAR, -1);
                break;
            default:
                c.add(Calendar.HOUR_OF_DAY, -24);
        }
        start = c.getTime();

        List<CongestedDurationRanking> stats = congestedDurationRankingMapper.selectByTimeRange(start, end);


        List<String> xAxisLabels = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

        if ("24 hours".equalsIgnoreCase(timeRange) || timeRange == null) {
            Map<Integer, Double> bucket = new LinkedHashMap<>();
            for (int i=0; i<24; i+=2) bucket.put(i, 0.0);
            for (CongestedDurationRanking s : stats) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(s.getTimeBucket());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int hourBucket = (hour / 2) * 2;
                bucket.put(hourBucket, bucket.getOrDefault(hourBucket, 0.0) + (s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds()));
            }
            xAxisLabels = new ArrayList<>();
            data = new ArrayList<>();
            for (int i=0; i<24; i+=2) {
                xAxisLabels.add(String.valueOf(i));
                double val = bucket.get(i);
                Map<String, Object> m = new HashMap<>();
                m.put("duration_sum", val);
                data.add(m);
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }
        else if ("one week".equalsIgnoreCase(timeRange)) {
            Map<String, Double> bucket = new LinkedHashMap<>();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            while (!calendar.getTime().after(end)) {
                String label = df.format(calendar.getTime());
                bucket.put(label, 0.0);
                calendar.add(Calendar.DATE, 1);
            }
            for (CongestedDurationRanking s : stats) {
                String label = df.format(s.getTimeBucket());
                bucket.put(label, bucket.getOrDefault(label, 0.0)
                        + (s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds()));
            }
            xAxisLabels = new ArrayList<>(bucket.keySet());
            data = new ArrayList<>();
            for (String label : xAxisLabels) {
                double val = bucket.get(label);
                Map<String, Object> m = new HashMap<>();
                m.put("duration_sum", val);
                data.add(m);
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }
        else if ("one month".equalsIgnoreCase(timeRange)) {
            Map<String, Double> bucket = new LinkedHashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            int weekIndex = 1;
            while (calendar.getTime().before(end)) {
                Date weekStart = calendar.getTime();
                calendar.add(Calendar.DATE, 7);
                Date weekEnd = calendar.getTime().before(end) ? calendar.getTime() : end;
                String label = "Week " + weekIndex + " (" + new SimpleDateFormat("MM-dd").format(weekStart) + ")";
                bucket.put(label, 0.0);
                weekIndex++;
            }
            for (CongestedDurationRanking s : stats) {
                long days = (s.getTimeBucket().getTime() - start.getTime()) / (1000 * 3600 * 24);
                int idx = (int)(days / 7) + 1;
                Calendar tmp = Calendar.getInstance();
                tmp.setTime(start);
                tmp.add(Calendar.DATE, (idx - 1) * 7);
                String label = "Week " + idx + " (" + new SimpleDateFormat("MM-dd").format(tmp.getTime()) + ")";
                bucket.put(label, bucket.getOrDefault(label, 0.0)
                        + (s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds()));
            }
            xAxisLabels = new ArrayList<>(bucket.keySet());
            data = new ArrayList<>();
            for (String label : xAxisLabels) {
                double val = bucket.get(label);
                Map<String, Object> m = new HashMap<>();
                m.put("duration_sum", val);
                data.add(m);
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        } else if ("six months".equalsIgnoreCase(timeRange)) {
            Map<String, Double> bucket = new LinkedHashMap<>();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            while (!calendar.getTime().after(end)) {
                String label = df.format(calendar.getTime());
                bucket.put(label, 0.0);
                calendar.add(Calendar.MONTH, 1);
            }
            for (CongestedDurationRanking s : stats) {
                String label = df.format(s.getTimeBucket());
                bucket.put(label, bucket.getOrDefault(label, 0.0)
                        + (s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds()));
            }
            xAxisLabels = new ArrayList<>(bucket.keySet());
            data = new ArrayList<>();
            for (String label : xAxisLabels) {
                double val = bucket.get(label);
                Map<String, Object> m = new HashMap<>();
                m.put("duration_sum", val);
                data.add(m);
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        } else if ("one year".equalsIgnoreCase(timeRange)) {
            Map<String, Double> bucket = new LinkedHashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            int quarterIndex = 1;
            SimpleDateFormat df = new SimpleDateFormat("yyyy");
            while (calendar.getTime().before(end)) {
                String label = df.format(calendar.getTime()) + "-Q" + quarterIndex;
                bucket.put(label, 0.0);
                calendar.add(Calendar.MONTH, 3);
                quarterIndex++;
            }
            for (CongestedDurationRanking s : stats) {
                Calendar sc = Calendar.getInstance();
                sc.setTime(s.getTimeBucket());
                int month = sc.get(Calendar.MONTH);
                int idx = (month / 3) + 1;
                String label = new SimpleDateFormat("yyyy").format(s.getTimeBucket()) + "-Q" + idx;
                bucket.put(label, bucket.getOrDefault(label, 0.0)
                        + (s.getTotalCongestionDurationSeconds() == null ? 0.0 : s.getTotalCongestionDurationSeconds()));
            }
            xAxisLabels = new ArrayList<>(bucket.keySet());
            data = new ArrayList<>();
            for (String label : xAxisLabels) {
                double val = bucket.get(label);
                Map<String, Object> m = new HashMap<>();
                m.put("duration_sum", val);
                data.add(m);
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }


        if (min == Double.MAX_VALUE) min = 0;
        if (max == Double.MIN_VALUE) max = 0;
        double interval = (max - min) / 6 > 0 ? (max - min) / 6 : 50;

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
