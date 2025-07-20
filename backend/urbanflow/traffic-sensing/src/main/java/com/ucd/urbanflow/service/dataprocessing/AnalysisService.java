package com.ucd.urbanflow.service.dataprocessing;

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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final int WINDOW_SIZE = 3; //280;
    private final TrafficDataPointRepository tsdbRepository;
    private final TrafficFlowMapper trafficFlowMapper;
    private final CongestionRoadCountMapper congestionRoadCountMapper;
    private final TopCongestedSegmentsMapper topCongestedSegmentsMapper;
    private final CongestedDurationRankingMapper congestedDurationRankingMapper;

    // A stateful, atomic counter for the last processed simulation step.
    private final AtomicLong lastProcessedStep = new AtomicLong(0);
//    private final AtomicLong lastProcessedStep = new AtomicLong(259);


    // The configurable initial time for the aggregation buckets.
    @Value("${traffic.analysis.base-time}")
    private String initialBaseTimeStr;

    // The stateful, current base time for the next aggregation window.
    private Date baseTime;

    /**
     * Initializes the baseTime from the application properties after the bean is constructed.
     */
    @PostConstruct
    public void initializeBaseTime() throws ParseException {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            this.baseTime = sdf.parse(initialBaseTimeStr);
            log.info("Analysis baseTime initialized to: {}", this.baseTime);
        } catch (ParseException e) {
            log.error("Failed to parse initial base-time property. Using current time as fallback.", e);
            this.baseTime = new Date();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void analysisTrigger() {
        tsdbRepository.findLatestStep().subscribe(latestStep -> {
            long currentLastProcessed = lastProcessedStep.get();
            log.info("Checking for window. Latest step in DB: {}, Last processed step: {}, Window size: {}", latestStep, currentLastProcessed, WINDOW_SIZE);

            if (latestStep >= currentLastProcessed + WINDOW_SIZE) {
                long endStep = currentLastProcessed + WINDOW_SIZE;

                Date currentTimeBucket = this.baseTime;
                log.info(">>>> [LAYER 3 TRIGGERED] Condition met. Processing window: steps {} -> {} with timeBucket {}", currentLastProcessed + 1, endStep, currentTimeBucket);

                log.info("Analysis window triggered: steps {} -> {} with timeBucket {}", currentLastProcessed + 1, endStep, currentTimeBucket);

                // Fetch data and process it asynchronously using the determined time bucket.
                tsdbRepository.findByStepRange(currentLastProcessed + 1, endStep)
                        .subscribe(dataWindow -> processWindowData(dataWindow, currentTimeBucket));

                // Immediately update the state for the next processing cycle.
                this.lastProcessedStep.set(endStep);
                this.baseTime = Date.from(this.baseTime.toInstant().plus(2, ChronoUnit.HOURS));
                log.info("State updated. Next window will use timeBucket: {}", this.baseTime);
            }
        });
    }

    private void processWindowData(List<TrafficDataPoint> dataWindow, Date timeBucket) {
        log.info(">>>> [LAYER 3 PROCESSING] Received {} data points from InfluxDB for timeBucket: {}", dataWindow.size(), timeBucket);

        if (dataWindow == null || dataWindow.isEmpty()) {
            log.warn("Window data is empty, skipping analysis for timeBucket: {}", timeBucket);
            return;
        }

        Map<String, List<TrafficDataPoint>> groupByJunction = dataWindow.stream()
                .collect(Collectors.groupingBy(TrafficDataPoint::getJunctionId));

        processTrafficFlow(groupByJunction, timeBucket);
        processCongestionJunctionCounts(groupByJunction, timeBucket);
        processCongestedTimesRanking(groupByJunction, timeBucket);
        processCongestionDurationRanking(groupByJunction, timeBucket);

        log.info("Dashboard data analysis and saving complete for timeBucket: {}.", timeBucket);
    }

    private void processTrafficFlow(Map<String, List<TrafficDataPoint>> data, Date timeBucket) {
        data.forEach((junctionId, points) -> {
            int totalFlow = points.stream()
                    .mapToInt(p -> p.getVehicleCount() != null ? p.getVehicleCount() : 0)
                    .sum();

            TrafficFlow tf = new TrafficFlow();
            tf.setTimeBucket(timeBucket);
            tf.setJunctionId(junctionId);
            tf.setFlowRateHourly(totalFlow);
            log.info(">>>> [LAYER 3 SAVING] Attempting to save to MySQL traffic_flow: {}", tf);
            try {
                trafficFlowMapper.insert(tf);
            } catch (Exception e) {
                log.error("!!! [LAYER 3 FAILED] Error while saving to traffic_flow table.", e);
            }        });
    }

    private void processCongestionJunctionCounts(Map<String, List<TrafficDataPoint>> data, Date timeBucket) {
        long congestedCount = data.values().stream()
                .filter(points -> points.stream().anyMatch(p -> p.getCongested() != null && p.getCongested()))
                .count();

        CongestedRoadCount cjc = new CongestedRoadCount();
        cjc.setTimeBucket(timeBucket);
        cjc.setCongestedJunctionCount((int) congestedCount);
        congestionRoadCountMapper.insert(cjc);
    }

    private void processCongestedTimesRanking(Map<String, List<TrafficDataPoint>> data, Date timeBucket) {
        data.forEach((junctionId, points) -> {
            long congestionTimes = points.stream()
                    .filter(p -> p.getCongested() != null && p.getCongested())
                    .count();

            if (congestionTimes > 0) {
                TopCongestedSegments ctr = new TopCongestedSegments();
                ctr.setTimeBucket(timeBucket);
                ctr.setJunctionName(points.get(0).getJunctionName());
                ctr.setCongestionTimes((int) congestionTimes);
                topCongestedSegmentsMapper.insert(ctr);
            }
        });
    }

    private void processCongestionDurationRanking(Map<String, List<TrafficDataPoint>> data, Date timeBucket) {

        data.forEach((junctionId, points) -> {

            Map<Long, Map<String, TrafficDataPoint>> stepToEdgeDataMap = points.stream()
                    .collect(Collectors.groupingBy(
                            TrafficDataPoint::getSimulationStep,
                            Collectors.toMap(TrafficDataPoint::getEdgeId, p -> p, (p1, p2) -> p1)
                    ));

            long minStep = points.stream().mapToLong(TrafficDataPoint::getSimulationStep).min().orElse(0);
            long maxStep = points.stream().mapToLong(TrafficDataPoint::getSimulationStep).max().orElse(0);

            double totalAggregatedDurationInSeconds = 0.0;

            for (long step = minStep; step <= maxStep; step++) {
                Map<String, TrafficDataPoint> currentStepData = stepToEdgeDataMap.getOrDefault(step, Collections.emptyMap());
                Map<String, TrafficDataPoint> prevStepData = stepToEdgeDataMap.getOrDefault(step - 1, Collections.emptyMap());

                double maxAvgWaitTimeForStep = 0.0;

                for (TrafficDataPoint currentPoint : currentStepData.values()) {
                    TrafficDataPoint prevPoint = prevStepData.get(currentPoint.getEdgeId());

                    double prevWaitTime = (prevPoint != null && prevPoint.getWaitTime() != null) ? prevPoint.getWaitTime() : 0.0;
                    double currentWaitTime = currentPoint.getWaitTime() != null ? currentPoint.getWaitTime() : 0.0;
                    double deltaWaitTime = currentWaitTime - prevWaitTime;

                    int waitingVehicleCount = currentPoint.getWaitingVehicleCount() != null ? currentPoint.getWaitingVehicleCount() : 0;

                    if (waitingVehicleCount > 0 && deltaWaitTime > 0) {
                        double avgWaitTime = deltaWaitTime / waitingVehicleCount;
                        if (avgWaitTime > maxAvgWaitTimeForStep) {
                            maxAvgWaitTimeForStep = avgWaitTime;
                        }
                    }
                }
                totalAggregatedDurationInSeconds += maxAvgWaitTimeForStep;
            }

            double totalDurationInMinutes = totalAggregatedDurationInSeconds / 60.0;
            double finalValue = Math.min(totalDurationInMinutes, 120.0);

            CongestedDurationRanking cdr = new CongestedDurationRanking();
            cdr.setTimeBucket(timeBucket);
            cdr.setJunctionName(points.get(0).getJunctionName());

            cdr.setTotalCongestionDurationSeconds((int)finalValue);

            congestedDurationRankingMapper.insert(cdr);
        });
    }
}