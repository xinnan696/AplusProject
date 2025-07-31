package com.ucd.urbanflow.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TrafficDataPointRepository {

    private final InfluxDBClient influxDBClient;
    private final String bucket;
    private final String org;
    private static final Logger log = LoggerFactory.getLogger(TrafficDataPointRepository.class);

    public TrafficDataPointRepository(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.bucket}") String bucket,
            @Value("${influxdb.org}") String org
    ) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
        this.org = org;
    }

    public Mono<Void> saveAll(List<TrafficDataPoint> dataPoints) {
        // This method is correct and remains unchanged.
        log.info(">>>> [REPOSITORY] Attempting to write {} data points to InfluxDB...", dataPoints.size());
        return Mono.fromRunnable(() -> {
            if (dataPoints == null || dataPoints.isEmpty()) {
                return;
            }
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            try {
                writeApi.writeMeasurements(bucket, org, WritePrecision.NS, dataPoints);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save to InfluxDB", e);
            }
        });
    }

    /**
     * Finds the highest simulationStep value using a more robust max() function.
     */
    public Mono<Long> findLatestStep() {
        return Mono.fromCallable(() -> {
            // This query is more direct and reliable for finding the maximum value of a field.
            String fluxQuery = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -30d) " +
                            "|> filter(fn: (r) => r._measurement == \"traffic_events\" and r._field == \"simulationStep\") " +
                            "|> max()",
                    bucket
            );
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery, org);

            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
                return 0L;
            }
            Object value = tables.get(0).getRecords().get(0).getValue();
            return value != null ? Long.parseLong(value.toString()) : 0L;
        });
    }

    /**
     * This method is correct and remains unchanged.
     */
    public Mono<List<TrafficDataPoint>> findByStepRange(long startStep, long endStep) {
        return Mono.fromCallable(() -> {
            String fluxQuery = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -30d) " +
                            "|> filter(fn: (r) => r._measurement == \"traffic_events\") " +
                            "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                            "|> filter(fn: (r) => r.simulationStep >= %d and r.simulationStep <= %d)",
                    bucket, startStep, endStep
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery, org);
            List<TrafficDataPoint> results = new ArrayList<>();

            if (tables.isEmpty()) {
                return results;
            }

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    // Manually map each record to a TrafficDataPoint object
                    // This gives us full control and avoids hidden errors.
                    TrafficDataPoint point = TrafficDataPoint.builder()
                            .time(record.getTime())
                            .junctionId((String) record.getValueByKey("junctionId"))
                            .junctionName((String) record.getValueByKey("junctionName"))
                            .edgeId((String) record.getValueByKey("edgeId"))
                            .simulationStep((Long) record.getValueByKey("simulationStep"))
                            .vehicleCount(record.getValueByKey("vehicleCount") != null ? ((Number) record.getValueByKey("vehicleCount")).intValue() : 0)
                            .waitTime(record.getValueByKey("waitTime") != null ? ((Number) record.getValueByKey("waitTime")).doubleValue() : 0.0)
                            .waitingVehicleCount(record.getValueByKey("waitingVehicleCount") != null ? ((Number) record.getValueByKey("waitingVehicleCount")).intValue() : 0)
                            .occupancy(record.getValueByKey("occupancy") != null ? ((Number) record.getValueByKey("occupancy")).floatValue() : 0.0f)
                            .congested((Boolean) record.getValueByKey("congested"))
                            .build();
                    results.add(point);
                }
            }
            return results;
        });
    }
}