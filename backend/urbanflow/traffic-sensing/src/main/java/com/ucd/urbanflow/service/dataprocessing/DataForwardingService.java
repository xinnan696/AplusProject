package com.ucd.urbanflow.service.dataprocessing;

import com.ucd.urbanflow.domain.dto.EnrichedTrafficEvent;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import com.ucd.urbanflow.repository.TrafficDataPointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

/**
 * Layer 2: Data Forwarding/Staging.
 * This version is modified for debugging to force synchronous writes and expose hidden errors.
 */
@Slf4j
@Service
public class DataForwardingService {
    private final RedisPollingService pollingService;
    private final TrafficDataPointRepository tsdbRepository;

    public DataForwardingService(RedisPollingService pollingService, TrafficDataPointRepository tsdbRepository) {
        this.pollingService = pollingService;
        this.tsdbRepository = tsdbRepository;
    }

    /**
     * The pipeline is now simplified to a direct, synchronous call
     * for each event to ensure any exception is immediately caught and logged.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startPipelineAfterStartup() {
        log.info("Application is ready. Starting data forwarding pipeline in DEBUG mode.");
        pollingService.getEventStream()
                .subscribe(this::processAndSaveEventSynchronously);
    }

    /**
     * This method processes and saves a single event within a try-catch block.
     * @param event The event to process.
     */
    private void processAndSaveEventSynchronously(EnrichedTrafficEvent event) {
        log.info(">>>> [LAYER 2 RECEIVED] Event for edge: {}", event.getEdgeId());
        try {
            TrafficDataPoint dataPoint = transform(event);
            log.info(">>>> [REPOSITORY] Attempting to synchronously write 1 data point to InfluxDB for step {}", dataPoint.getSimulationStep());

            // This is now a blocking call. If it fails, the exception will be caught below.
            tsdbRepository.saveAll(Collections.singletonList(dataPoint)).block();

            log.info(" Successfully forwarded data to InfluxDB for edge {}", dataPoint.getEdgeId());
        } catch (Exception e) {
            // If the write fails, this log is guaranteed to be printed.
            log.error("!!! [LAYER 2 FAILED] CRITICAL ERROR during synchronous InfluxDB write.", e);
        }
    }

    private TrafficDataPoint transform(EnrichedTrafficEvent event) {
        return TrafficDataPoint.builder()
                .junctionId(event.getJunctionId())
                .junctionName(event.getJunctionName())
                .edgeId(event.getEdgeId())
                .vehicleCount(event.getVehicleCount())
                .waitingTime(event.getWaitingTime())
                .waitingVehicleCount(event.getWaitingVehicleCount())
                .congested(event.isCongested())
                .occupancy(event.getOccupancy())
                .simulationStep(event.getSimulationStep())
                .time(Instant.now())
                .build();
    }
}