package com.ucd.urbanflow.service.dataprocessing;

import com.ucd.urbanflow.domain.dto.EnrichedTrafficEvent;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import com.ucd.urbanflow.respository.TrafficDataPointRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class DataForwardingService {
    private final RedisPollingService pollingService;
    private final TrafficDataPointRepository tsdbRepository;

    public DataForwardingService(RedisPollingService pollingService, TrafficDataPointRepository tsdbRepository) {
        this.pollingService = pollingService;
        this.tsdbRepository = tsdbRepository;
    }

    @PostConstruct
    public void setUpPipeline() {
        log.info("Starting data forwarding pipeline: Redis -> InfluxDB");
        pollingService.getEventStream()
                .publishOn(Schedulers.boundedElastic())
                .map(this::transform)
                .bufferTimeout(200, Duration.ofSeconds(5)) // Batch 200 items or every 5 seconds
                .flatMap(tsdbRepository::saveAll)
                .doOnError(e -> log.error("Data forwarding pipeline error", e))
                .subscribe(
                        success -> log.trace("Successfully forwarded a batch of data to InfluxDB"),
                        error -> log.error("Forwarding pipeline terminated with an error", error)
                );
    }

    private TrafficDataPoint transform(EnrichedTrafficEvent event) {
        return TrafficDataPoint.builder()
                .junctionId(event.getJunctionId())
                .junctionName(event.getJunctionName())
                .edgeId(event.getEdgeId())
                .vehicleCount(event.getVehicleCount())
                .waitTime(event.getWaitTime())
                .waitingVehicleCount(event.getWaitingVehicleCount())
                .congested(event.isCongested())
                .simulationStep(event.getSimulationStep())
                .time(Instant.now())
                .build();
    }
}
