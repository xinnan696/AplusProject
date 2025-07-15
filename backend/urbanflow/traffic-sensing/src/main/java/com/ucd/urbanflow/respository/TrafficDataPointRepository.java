package com.ucd.urbanflow.respository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxTable;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;



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

    /**
     * Saves a list of data points to InfluxDB.
     */
    public Mono<Void> saveAll(List<TrafficDataPoint> dataPoints) {
        return Mono.fromRunnable(() -> {
            if (dataPoints == null || dataPoints.isEmpty()) {
                return;
            }
            try (WriteApi writeApi = influxDBClient.getWriteApi()) {
                writeApi.writeMeasurements(bucket, org, WritePrecision.NS, dataPoints);
            } catch (Exception e) {
                log.error("Failed to write to InfluxDB", e);
                throw new RuntimeException("Failed to save to InfluxDB", e);
            }
        });
    }

    /**
     * Finds the latest simulation step from the database.
     */
//    public Mono<Long> findLatestStep() {
//        return Mono.fromCallable(() -> {
//            String fluxQuery = String.format(
//                    "from(bucket: \"%s\") |> range(start: -30d) |> filter(fn: (r) => r._measurement == \"traffic_events\") |> keep(columns: [\"simulationStep\"]) |> last()",
//                    bucket
//            );
//            QueryApi queryApi = influxDBClient.getQueryApi();
//            List<FluxTable> tables = queryApi.query(fluxQuery, org);
//
//            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty() || tables.get(0).getRecords().get(0).getValue() == null) {
//                return 0L;
//            }
//            return Long.parseLong(tables.get(0).getRecords().get(0).getValue().toString());
//        });
//    }
    public Mono<Long> findLatestStep() {
        return Mono.fromCallable(() -> {
            String fluxQuery = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -30d) " +
                            "|> filter(fn: (r) => r._measurement == \"traffic_events\" and r._field == \"simulationStep\") " +
                            "|> sort(columns:[\"_time\"], desc: true) " +
                            "|> limit(n:1)",
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
     * Finds data points within a given range of simulation steps.
     */
    public Mono<List<TrafficDataPoint>> findByStepRange(long startStep, long endStep) {
        return Mono.fromCallable(() -> {
            String fluxQuery = String.format(
                    "from(bucket: \"%s\") |> range(start: -30d) |> filter(fn: (r) => r._measurement == \"traffic_events\" and r.simulationStep >= %d and r.simulationStep <= %d)",
                    bucket, startStep, endStep
            );
            return influxDBClient.getQueryApi().query(fluxQuery, org, TrafficDataPoint.class);
        });
    }
}