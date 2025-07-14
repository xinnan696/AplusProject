package com.ucd.urbanflow.service.dataprocessing;

import com.google.gson.Gson;
import com.ucd.urbanflow.domain.dto.EnrichedTrafficEvent;
import com.ucd.urbanflow.domain.dto.RedisEdgeData;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.mapper.JunctionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RedisPollingService {

    private final StringRedisTemplate redisTemplate;
    private final JunctionMapper junctionMapper;
    private final Gson gson = new Gson();

    @Value("${traffic.congestion.threshold}")
    private double congestionThreshold;

    // State management to detect new data based on timestamp/step.
    private final Map<String, Double> lastSeenTimestamps = new ConcurrentHashMap<>();

    // An in-memory cache for mapping an edge to its parent junction information.
    private final Map<String, List<String>> junctionToEdgesMap = new ConcurrentHashMap<>();
    private final Map<String, String> junctionIdToNameMap = new ConcurrentHashMap<>();

    // A thread-safe, reactive sink to publish events to downstream services.
    private final Sinks.Many<EnrichedTrafficEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public RedisPollingService(StringRedisTemplate redisTemplate, JunctionMapper junctionMapper) {
        this.redisTemplate = redisTemplate;
        this.junctionMapper = junctionMapper;
    }

    /**
     * Periodically rebuilds the mapping from junctions to their incoming edges.
     */
    @Scheduled(fixedRate = 300000) // Rebuild map every 5 minutes
    public void updateJunctionToEdgesMap() {
        log.info("Updating junction-to-edges map from database...");
        try {
            List<JunctionIncomingEdge> mappings = junctionMapper.findAllJunctionEdges();

            // Group all edge IDs by their parent junction ID.
            Map<String, List<String>> newMap = mappings.stream()
                    .collect(Collectors.groupingBy(
                            JunctionIncomingEdge::getJunctionId,
                            Collectors.mapping(JunctionIncomingEdge::getIncomingEdgeId, Collectors.toList())
                    ));

            // Create a simple map from junction ID to junction name.
            Map<String, String> newNameMap = mappings.stream()
                    .collect(Collectors.toMap(JunctionIncomingEdge::getJunctionId, JunctionIncomingEdge::getJunctionName, (name1, name2) -> name1));

            // Atomically update the caches.
            junctionToEdgesMap.clear();
            junctionToEdgesMap.putAll(newMap);
            junctionIdToNameMap.clear();
            junctionIdToNameMap.putAll(newNameMap);

            log.info("Junction-to-edges map updated. Total junctions: {}", newMap.size());
        } catch (Exception e) {
            log.error("Failed to update junction-to-edges map from database.", e);
        }
    }

    /**
     * Polls Redis for new edge data at high frequency, simulates missing fields,
     * enriches the data, and publishes it to the reactive stream.
     */
    @Scheduled(fixedDelay = 1000) // Poll every 1 second
    public void pollRedisForChanges() {
        // First, calculate the set of currently congested junctions for this time step.
        Set<String> congestedJunctions = getCongestedJunctions();

        // NOTE: In a production environment, use SCAN to avoid blocking Redis.
        Set<String> edgeKeys = redisTemplate.keys("sumo:edge:*");
        if (edgeKeys == null || edgeKeys.isEmpty()) {
            return;
        }

        for (String key : edgeKeys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) continue;

            RedisEdgeData edgeData = gson.fromJson(json, RedisEdgeData.class);

            // If the real "occupancy" field is not present in the message from Redis,simulate it here.
//            if (edgeData.getOccupancy() == null) {
//                if (edgeData.getLaneNumber() != null && edgeData.getLaneNumber() > 0 && edgeData.getWaitingVehicleCount() != null) {
//                    // Assuming each waiting car occupies ~15% of a lane segment for calculation purposes.
//                    float simulatedOccupancy = (float) (((double) edgeData.getWaitingVehicleCount() / edgeData.getLaneNumber()) * 15.0);
//                    edgeData.setOccupancy(Math.min(simulatedOccupancy, 100.0f)); // Cap at 100%
//                } else {
//                    edgeData.setOccupancy(0.0f); // Default to 0 if data is missing for simulation
//                }
//            }

            if (isNewData(edgeData)) {
                lastSeenTimestamps.put(edgeData.getEdgeId(), edgeData.getTimestamp());

                // Find the parent junction for the current edge from our cached map.
                findJunctionIdForEdge(edgeData.getEdgeId()).ifPresent(junctionId -> {
                    EnrichedTrafficEvent event = EnrichedTrafficEvent.builder()
                            .edgeId(edgeData.getEdgeId())
                            .junctionId(junctionId)
                            .junctionName(junctionIdToNameMap.getOrDefault(junctionId, junctionId))
                            .simulationStep(edgeData.getTimestamp().longValue())
                            .vehicleCount(edgeData.getVehicleCount() != null ? edgeData.getVehicleCount() : 0)
                            .waitTime(edgeData.getWaitTime() != null ? edgeData.getWaitTime() : 0.0)
                            .waitingVehicleCount(edgeData.getWaitingVehicleCount() != null ? edgeData.getWaitingVehicleCount() : 0)
                            .congested(congestedJunctions.contains(junctionId))
                            .build();

                    // Publish the fully enriched event to the stream for other services to consume.
                    sink.tryEmitNext(event);
                });
            }
        }
    }

    /**
     * Calculates the set of currently congested junctions by calculating occupancy for each.
     * @return A set of congested junction IDs for the current time step.
     */
    private Set<String> getCongestedJunctions() {
        if (junctionToEdgesMap.isEmpty()) {
            return Collections.emptySet();
        }

        return junctionToEdgesMap.entrySet().stream()
                .filter(entry -> isJunctionCongested(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a single junction is congested by finding the max lane occupancy of its incoming edges.
     * @param incomingEdgeIds A list of incoming edge IDs for the junction.
     * @return true if the junction is congested, false otherwise.
     */
    private boolean isJunctionCongested(List<String> incomingEdgeIds) {
        double maxOccupancy = 0.0;
        for (String edgeId : incomingEdgeIds) {
            String json = redisTemplate.opsForValue().get("sumo:edge:" + edgeId);
            if (json == null) continue;

            RedisEdgeData edgeData = gson.fromJson(json, RedisEdgeData.class);
            if (edgeData == null) continue;

            float currentOccupancy = edgeData.getOccupancy() != null ? edgeData.getOccupancy() : 0.0f;
            if (currentOccupancy > maxOccupancy) {
                maxOccupancy = currentOccupancy;
            }
        }

        // Compare the resulting max occupancy with the configurable threshold.
        return maxOccupancy > congestionThreshold;
    }

    /**
     * Checks if the data from Redis is new by comparing its timestamp with the last seen timestamp.
     * @param data The data fetched from Redis.
     * @return true if the data is new, false otherwise.
     */
    private boolean isNewData(RedisEdgeData data) {
        if (data == null || data.getTimestamp() == null) return false;
        double previous = lastSeenTimestamps.getOrDefault(data.getEdgeId(), -1.0);
        return data.getTimestamp() > previous;
    }

    /**
     * A reverse lookup to find a junction ID for a given edge ID from the cached map.
     * @param edgeId The edge ID to find.
     * @return An Optional containing the junction ID if found.
     */
    private Optional<String> findJunctionIdForEdge(String edgeId) {
        return junctionToEdgesMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(edgeId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Exposes the reactive stream of enriched events to downstream services.
     * @return A hot stream (Flux) of EnrichedTrafficEvent objects.
     */
    public Flux<EnrichedTrafficEvent> getEventStream() {
        return this.sink.asFlux();
    }

    /**
     * A simple inner class to hold junction information for the mapping cache.
     * Using a record for a compact, immutable data carrier.
     */
    private static class JunctionInfo {
        private final String id;
        private final String name;

        JunctionInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String getId() { return id; }
        public String getName() { return name; }
    }
}