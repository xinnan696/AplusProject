package com.ucd.urbanflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.domain.dto.JunctionCongestionDTO;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.domain.pojo.JunctionInfo;
import com.ucd.urbanflow.domain.vo.EdgeData;
import com.ucd.urbanflow.mapper.JunctionMapper;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrafficService {

    private final JunctionMapper junctionMapper;
    private final JunctionRegionsMapper junctionRegionsMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final double occupancyThreshold;

    private static final String REDIS_EDGE_KEY_PREFIX = "sumo:edge";
    private static final String CACHE_KEY_CONGESTED_JUNCTIONS = "traffic:cache:top6_congested_junctions";

    @Autowired
    public TrafficService(JunctionMapper junctionMapper,
                          JunctionRegionsMapper junctionRegionsMapper,
                          RedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${traffic.congestion.threshold}") double occupancyThreshold) {
        this.junctionMapper = junctionMapper;
        this.junctionRegionsMapper = junctionRegionsMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.occupancyThreshold = occupancyThreshold;
    }


    public List<Map<String, String>> getJunctionname(String managedAreas) {
        List<Map<String, String>> junctionList = new ArrayList<>();

        if (managedAreas == null || managedAreas.isEmpty()) {
            List<JunctionIncomingEdge> allJunctionEdges = junctionMapper.findAllJunctionEdges();
            Set<String> seenJunctionIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(allJunctionEdges)){
                for (JunctionIncomingEdge edge : allJunctionEdges) {
                    if (seenJunctionIds.add(edge.getJunctionId())) {
                        Map<String, String> map = new HashMap<>();
                        map.put("junctionId", edge.getJunctionId());
                        map.put("junctionName", edge.getJunctionName());
                        junctionList.add(map);
                    }
                }
            }
        } else {
            List<JunctionInfo> filteredJunctions = junctionRegionsMapper.findJunctionsByArea(managedAreas);
            if (!CollectionUtils.isEmpty(filteredJunctions)) {
                junctionList = filteredJunctions.stream()
                        .map(info -> {
                            Map<String, String> map = new HashMap<>();
                            map.put("junctionId", info.getJunctionId());
                            map.put("junctionName", info.getJunctionName());
                            return map;
                        })
                        .collect(Collectors.toList());
            }
        }

        return junctionList;
    }


    public List<JunctionCongestionDTO> getCongestedJunctions() {
        return this.calculateTopCongestedJunctions();
    }


    @Scheduled(fixedRateString = "${traffic.cache.update-rate-ms}")
    public void updateCongestionCache() {
        try {
            List<JunctionCongestionDTO> topJunctions = this.calculateTopCongestedJunctions();
            String jsonCacheData = objectMapper.writeValueAsString(topJunctions);
            redisTemplate.opsForValue().set(CACHE_KEY_CONGESTED_JUNCTIONS, jsonCacheData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize congestion data to JSON for caching.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during congestion cache update.", e);
        }
    }


    private List<JunctionCongestionDTO> calculateTopCongestedJunctions() {
        List<JunctionIncomingEdge> allJunctionEdges = junctionMapper.findAllJunctionEdges();
        if (allJunctionEdges == null || allJunctionEdges.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<JunctionIncomingEdge>> edgesByJunction = allJunctionEdges.stream()
                .collect(Collectors.groupingBy(JunctionIncomingEdge::getJunctionId));

        List<JunctionCongestionDTO> junctionCongestions = edgesByJunction.entrySet().stream()
                .map(entry -> {
                    String junctionId = entry.getKey();
                    List<JunctionIncomingEdge> incomingEdges = entry.getValue();
                    String junctionName = incomingEdges.isEmpty() ? "Unknown" : incomingEdges.get(0).getJunctionName();
                    int maxCongestion = calculateMaxCongestionForJunction(incomingEdges);
                    return new JunctionCongestionDTO(junctionId,junctionName, maxCongestion);
                })
                .collect(Collectors.toList());

        return junctionCongestions.stream()
                .sorted(Comparator.comparingInt(JunctionCongestionDTO::getCongestionCount).reversed())
                .filter(dto -> dto.getCongestionCount() > 0)
                .collect(Collectors.toList());
    }


    protected int calculateMaxCongestionForJunction(List<JunctionIncomingEdge> incomingEdges) {
        if (incomingEdges == null || incomingEdges.isEmpty()) {
            return 0;
        }

        List<String> edgeIds = incomingEdges.stream()
                .map(JunctionIncomingEdge::getIncomingEdgeId)
                .collect(Collectors.toList());

        List<Object> edgeIdObjects = new ArrayList<>(edgeIds);
        List<Object> edgeJsonValues = redisTemplate.opsForHash().multiGet(REDIS_EDGE_KEY_PREFIX, edgeIdObjects);

        if (edgeJsonValues == null) {
            return 0;
        }

        double maxOccupancy = 0.0;
        int vehicleCountAtMaxOccupancy = 0;

        for (Object jsonObject : edgeJsonValues) {
            if (jsonObject == null) continue;

            String jsonString = jsonObject.toString();

            try {
                EdgeData edgeData = objectMapper.readValue(jsonString, EdgeData.class);
                float occupancy = edgeData.getOccupancy();


                log.info("Evaluating edge for congestion - occupancy: {}, vehicleCount: {}, edgeData: {}",
                        occupancy, edgeData.getVehicleCount(), edgeData);

                if (occupancy > maxOccupancy) {
                    maxOccupancy = occupancy;
                    vehicleCountAtMaxOccupancy = edgeData.getVehicleCount();
                }

            } catch (JsonProcessingException e) {
                log.error("Failed to parse edge data JSON: {}", jsonString, e);
            }
        }

        return maxOccupancy >= this.occupancyThreshold ? vehicleCountAtMaxOccupancy : 0;
    }
}