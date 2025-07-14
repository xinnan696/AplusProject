package com.ucd.urbanflow.respository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ucd.urbanflow.domain.tsdb.TrafficDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Repository
public class TrafficDataPointRepository {
    private final InfluxDBClient influxDBClient;


    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    public TrafficDataPointRepository(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
        this.org = org;
    }

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

    public Mono<Long> findLatestStep() {
        return Mono.fromCallable(() -> {
            String fluxQuery = String.format(
                    "from(bucket: \"%s\") |> range(start: -30d) |> filter(fn: (r) => r._measurement == \"traffic_events\") |> keep(columns: [\"simulationStep\"]) |> last()",
                    bucket
            );
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(fluxQuery, org);

            // Check if any tables were returned.
            if (tables.isEmpty()) {
                return 0L;
            }

            // Get the records from the first table.
            List<FluxRecord> records = tables.get(0).getRecords();

            if (records.isEmpty() || records.get(0).getValue() == null) {
                return 0L;
            }
            return Long.parseLong(records.get(0).getValue().toString());
        });
    }

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
