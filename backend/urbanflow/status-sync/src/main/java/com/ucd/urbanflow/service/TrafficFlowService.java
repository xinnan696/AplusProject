package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.TrafficFlowMapper;
import com.ucd.urbanflow.model.TrafficFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrafficFlowService {
    @Autowired
    private TrafficFlowMapper mapper;

    public Map<String, Object> buildDashboardData(String junctionId, String timeRange) {
//        Date end = new Date();
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


        List<TrafficFlow> stats = mapper.selectByJunctionAndTimeRange(junctionId, start, end);


        List<String> xAxisLabels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

        if ("24hours".equalsIgnoreCase(timeRange) || timeRange == null) {
            Map<Integer, Integer> flowByHour = new LinkedHashMap<>();
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(start);
            for (int i = 0; i < 24; i += 2) {
                int hourBucket = tempCal.get(Calendar.HOUR_OF_DAY);
                flowByHour.put((hourBucket / 2) * 2, 0);
                xAxisLabels.add(String.valueOf((hourBucket / 2) * 2));
                tempCal.add(Calendar.HOUR_OF_DAY, 2);
            }

            for (TrafficFlow s : stats) {
                Calendar c = Calendar.getInstance();
                c.setTime(s.getTimeBucket());
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int hourBucket = (hour / 2) * 2;
                flowByHour.put(hourBucket, flowByHour.getOrDefault(hourBucket, 0)
                        + (s.getFlowRateHourly() == null ? 0 : s.getFlowRateHourly()));
            }

            for(String label : xAxisLabels) {
                int hourKey = Integer.parseInt(label);
                int v = flowByHour.getOrDefault(hourKey, 0);
                data.add(v);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        } else if ("oneweek".equalsIgnoreCase(timeRange)) {
            Map<String, Integer> flowByDay = new LinkedHashMap<>();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            while (!calendar.getTime().after(end)) {
                String label = df.format(calendar.getTime());
                flowByDay.put(label, 0);
                calendar.add(Calendar.DATE, 1);
            }
            for (TrafficFlow s : stats) {
                String label = df.format(s.getTimeBucket());
                flowByDay.put(label, flowByDay.getOrDefault(label, 0)
                        + (s.getFlowRateHourly() == null ? 0 : s.getFlowRateHourly()));
            }
            for (String label : flowByDay.keySet()) {
                xAxisLabels.add(label);
                int v = flowByDay.get(label);
                data.add(v);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        } else if ("onemonth".equalsIgnoreCase(timeRange)) {
            Map<String, Integer> flowByWeek = new LinkedHashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            int weekIndex = 1;
            while (calendar.getTime().before(end)) {
                Date weekStart = calendar.getTime();
                calendar.add(Calendar.DATE, 7);
                String label = "Week " + weekIndex + " (" + new SimpleDateFormat("MM-dd").format(weekStart) + ")";
                flowByWeek.put(label, 0);
                weekIndex++;
            }
            for (TrafficFlow s : stats) {
                long days = (s.getTimeBucket().getTime() - start.getTime()) / (1000 * 3600 * 24);
                int idx = (int) (days / 7) + 1;
                Calendar tmp = Calendar.getInstance();
                tmp.setTime(start);
                tmp.add(Calendar.DATE, (idx - 1) * 7);
                String label = "Week " + idx + " (" + new SimpleDateFormat("MM-dd").format(tmp.getTime()) + ")";
                flowByWeek.put(label, flowByWeek.getOrDefault(label, 0)
                        + (s.getFlowRateHourly() == null ? 0 : s.getFlowRateHourly()));
            }
            for (String label : flowByWeek.keySet()) {
                xAxisLabels.add(label);
                int v = flowByWeek.get(label);
                data.add(v);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        } else if ("sixmonths".equalsIgnoreCase(timeRange)) {
            Map<String, Integer> flowByMonth = new LinkedHashMap<>();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            while (!calendar.getTime().after(end)) {
                String label = df.format(calendar.getTime());
                flowByMonth.put(label, 0);
                calendar.add(Calendar.MONTH, 1);
            }
            for (TrafficFlow s : stats) {
                String label = df.format(s.getTimeBucket());
                flowByMonth.put(label, flowByMonth.getOrDefault(label, 0)
                        + (s.getFlowRateHourly() == null ? 0 : s.getFlowRateHourly()));
            }
            for (String label : flowByMonth.keySet()) {
                xAxisLabels.add(label);
                int v = flowByMonth.get(label);
                data.add(v);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        } else if ("oneyear".equalsIgnoreCase(timeRange)) {
            Map<String, Integer> flowByQuarter = new LinkedHashMap<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            int quarterIndex = 1;
            SimpleDateFormat df = new SimpleDateFormat("yyyy");
            while (calendar.getTime().before(end)) {
                String label = df.format(calendar.getTime()) + "-Q" + quarterIndex;
                flowByQuarter.put(label, 0);
                calendar.add(Calendar.MONTH, 3);
                quarterIndex++;
            }
            for (TrafficFlow s : stats) {
                Calendar sc = Calendar.getInstance();
                sc.setTime(s.getTimeBucket());
                int month = sc.get(Calendar.MONTH);
                int idx = (month / 3) + 1;
                String label = new SimpleDateFormat("yyyy").format(s.getTimeBucket()) + "-Q" + idx;
                flowByQuarter.put(label, flowByQuarter.getOrDefault(label, 0)
                        + (s.getFlowRateHourly() == null ? 0 : s.getFlowRateHourly()));
            }
            for (String label : flowByQuarter.keySet()) {
                xAxisLabels.add(label);
                int v = flowByQuarter.get(label);
                data.add(v);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        if (min == Integer.MAX_VALUE) min = 0;
        if (max == Integer.MIN_VALUE) max = 0;
        int interval = (max - min) / 6 > 0 ? (max - min) / 6 : 500;

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
