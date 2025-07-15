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

    /**
     * [MODIFIED] The single, constant key for the Redis Hash containing all edge data.
     * Please change this value if your actual Redis hash key is different.
     */
    private static final String REDIS_EDGE_HASH_KEY = "sumo:edge";

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
            Map<String, List<String>> newMap = mappings.stream()
                    .collect(Collectors.groupingBy(
                            JunctionIncomingEdge::getJunctionId,
                            Collectors.mapping(JunctionIncomingEdge::getIncomingEdgeId, Collectors.toList())
                    ));
            Map<String, String> newNameMap = mappings.stream()
                    .collect(Collectors.toMap(JunctionIncomingEdge::getJunctionId, JunctionIncomingEdge::getJunctionName, (name1, name2) -> name1));

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
     * [MODIFIED] Polls Redis efficiently by fetching all edge data at once.
     */
    @Scheduled(fixedDelay = 1000) // Poll every 1 second
    public void pollRedisForChanges() {
        // Step 1: Fetch ALL edge data from the hash in a single, efficient command.
        Map<String, RedisEdgeData> allCurrentEdgeData = fetchAllEdgeDataFromHash();
        if (allCurrentEdgeData.isEmpty()) {
            return;
        }

        // Step 2: Calculate the set of congested junctions using the in-memory data.
        Set<String> congestedJunctions = getCongestedJunctions(allCurrentEdgeData);

        // Step 3: Process each data point from the fetched in-memory map.
        for (RedisEdgeData edgeData : allCurrentEdgeData.values()) {
            if (isNewData(edgeData)) {
                lastSeenTimestamps.put(edgeData.getEdgeId(), edgeData.getTimestamp());

                findJunctionIdForEdge(edgeData.getEdgeId()).ifPresent(junctionId -> {
                    EnrichedTrafficEvent event = EnrichedTrafficEvent.builder()
                            .edgeId(edgeData.getEdgeId())
                            .junctionId(junctionId)
                            .junctionName(junctionIdToNameMap.getOrDefault(junctionId, junctionId))
                            .simulationStep(edgeData.getTimestamp().longValue())
                            .vehicleCount(Optional.ofNullable(edgeData.getVehicleCount()).orElse(0))
                            .waitTime(Optional.ofNullable(edgeData.getWaitTime()).orElse(0.0))
                            .waitingVehicleCount(Optional.ofNullable(edgeData.getWaitingVehicleCount()).orElse(0))
                            .congested(congestedJunctions.contains(junctionId))
                            .build();

                    sink.tryEmitNext(event);
                    log.info(">>>> [LAYER 1 SUCCESS] Published event for edge: {}", event.getEdgeId());

                });
            }
        }
    }

    /**
     * [NEW] Helper method to fetch all fields from the edge data hash at once.
     * This corresponds to the HGETALL command in Redis.
     * @return A Map where the key is the edgeId and the value is the parsed RedisEdgeData object.
     */
    private Map<String, RedisEdgeData> fetchAllEdgeDataFromHash() {
        try {
            Map<Object, Object> rawHash = redisTemplate.opsForHash().entries(REDIS_EDGE_HASH_KEY);
            return rawHash.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> gson.fromJson((String) entry.getValue(), RedisEdgeData.class)
                    ));
        } catch (Exception e) {
            log.error("Failed to fetch or parse edge data hash from Redis key: {}", REDIS_EDGE_HASH_KEY, e);
            return Collections.emptyMap();
        }
    }

    /**
     * [MODIFIED] Calculates congested junctions using the pre-fetched data map for efficiency.
     * @param allEdgeData A map containing all current edge data from Redis for this cycle.
     * @return A set of congested junction IDs for the current time step.
     */
    private Set<String> getCongestedJunctions(Map<String, RedisEdgeData> allEdgeData) {
        if (junctionToEdgesMap.isEmpty()) {
            return Collections.emptySet();
        }

        return junctionToEdgesMap.entrySet().stream()
                .filter(entry -> isJunctionCongested(entry.getValue(), allEdgeData))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * [MODIFIED] Checks if a junction is congested by looking up its edges in the pre-fetched data map.
     * @param incomingEdgeIds A list of incoming edge IDs for the junction.
     * @param allEdgeData The map of all available edge data for this cycle.
     * @return true if the junction is congested, false otherwise.
     */
    private boolean isJunctionCongested(List<String> incomingEdgeIds, Map<String, RedisEdgeData> allEdgeData) {
        double maxOccupancy = 0.0;
        for (String edgeId : incomingEdgeIds) {
            // No Redis call here. We use the map that was already fetched.
            RedisEdgeData edgeData = allEdgeData.get(edgeId);
            if (edgeData == null) continue;

            // --- Simulation Block ---
            if (edgeData.getOccupancy() == null) {
                if (edgeData.getLaneNumber() != null && edgeData.getLaneNumber() > 0 && edgeData.getWaitingVehicleCount() != null) {
                    float simulatedOccupancy = (float) (((double) edgeData.getWaitingVehicleCount() / edgeData.getLaneNumber()) * 15.0);
                    edgeData.setOccupancy(Math.min(simulatedOccupancy, 100.0f));
                } else {
                    edgeData.setOccupancy(0.0f);
                }
            }
            // --- End Simulation Block ---

            float currentOccupancy = Optional.ofNullable(edgeData.getOccupancy()).orElse(0.0f);
            if (currentOccupancy > maxOccupancy) {
                maxOccupancy = currentOccupancy;
            }
        }
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
     * 对外暴露 Sink，用于测试时手动推送数据。
     */
    public Sinks.Many<EnrichedTrafficEvent> getSink() {
        return this.sink;
    }
}