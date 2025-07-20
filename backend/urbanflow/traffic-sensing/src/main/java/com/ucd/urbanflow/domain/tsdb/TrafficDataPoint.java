package com.ucd.urbanflow.domain.tsdb;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * POJO representing a single data point to be written to the InfluxDB staging area.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "traffic_events")
public class TrafficDataPoint {

    @Column(tag = true)
    private String junctionId;

    @Column(tag = true)
    private String junctionName;

    @Column(tag = true)
    private String edgeId;

    @Column
    private Integer vehicleCount;

    @Column
    private Double waitTime;

    @Column
    private Boolean congested;

    @Column
    private Long simulationStep;

    @Column(timestamp = true)
    private Instant time;

    @Column
    private Float occupancy;

    @Column
    private Integer waitingVehicleCount;
}