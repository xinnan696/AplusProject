package com.ucd.urbanflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.domain.dto.JunctionCongestionDTO;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.domain.pojo.JunctionInfo;
import com.ucd.urbanflow.domain.vo.EdgeData;
import com.ucd.urbanflow.mapper.JunctionMapper;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficService {

    private final JunctionMapper junctionMapper;
    private final JunctionRegionsMapper junctionRegionsMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_EDGE_KEY_PREFIX = "sumo:edge:";
    private static final int TOP_N_JUNCTIONS = 6;

    private static final String CACHE_KEY_CONGESTED_JUNCTIONS = "traffic:cache:top6_congested_junctions";

    private static final double OCCUPANCY_THRESHOLD = 0.6;


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
                // Convert the list of POJOs to the required List<Map<String, String>> format
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
//        log.info("Running scheduled task: Calculating and caching congested junctions...");
        try {
            // 1.Execute core computation
            List<JunctionCongestionDTO> topJunctions = this.calculateTopCongestedJunctions();

            // 2.Serialize the result list into a JSON string
            String jsonCacheData = objectMapper.writeValueAsString(topJunctions);

            // 3.Save the JSON string to the specified Redis key
            redisTemplate.opsForValue().set(CACHE_KEY_CONGESTED_JUNCTIONS, jsonCacheData);

//            log.info("Successfully updated congested junctions cache. Found {} junctions.", topJunctions.size());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize congestion data to JSON for caching.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during congestion cache update.", e);
        }
    }


    private List<JunctionCongestionDTO> calculateTopCongestedJunctions() {
        // 从数据库获取所有 "路口-入口" 的关系
        List<JunctionIncomingEdge> allJunctionEdges = junctionMapper.findAllJunctionEdges();
        if (allJunctionEdges == null || allJunctionEdges.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 junction_id 分组
        Map<String, List<JunctionIncomingEdge>> edgesByJunction = allJunctionEdges.stream()
                .collect(Collectors.groupingBy(JunctionIncomingEdge::getJunctionId));

        // 为每个路口计算拥堵指数
        List<JunctionCongestionDTO> junctionCongestions = edgesByJunction.entrySet().stream()
                .map(entry -> {
                    String junctionId = entry.getKey();
                    List<JunctionIncomingEdge> incomingEdges = entry.getValue();
                    // 因为同一个junctionId的路口名称都是一样的，所以从第一个元素获取即可
                    String junctionName = incomingEdges.isEmpty() ? "Unknown" : incomingEdges.get(0).getJunctionName();
                    int maxCongestion = calculateMaxCongestionForJunction(incomingEdges);
                    return new JunctionCongestionDTO(junctionId,junctionName, maxCongestion);
                })
                .collect(Collectors.toList());

        // 按拥堵指数降序排序并返回前 N 个
        return junctionCongestions.stream()
                .sorted(Comparator.comparingInt(JunctionCongestionDTO::getCongestionCount).reversed())
//                .limit(TOP_N_JUNCTIONS)
                .collect(Collectors.toList());
    }

//    private int calculateMaxCongestionForJunction(List<JunctionIncomingEdge> incomingEdges) {
//
//        if (incomingEdges == null || incomingEdges.isEmpty()) {
//            return 0;
//        }
//        List<String> redisKeys = incomingEdges.stream()
//                .map(edge -> REDIS_EDGE_KEY_PREFIX + edge.getIncomingEdgeId())
//                .collect(Collectors.toList());
//        List<String> edgeJsonValues = redisTemplate.opsForValue().multiGet(redisKeys);
//        if (edgeJsonValues == null) {
//            return 0;
//        }
//        return edgeJsonValues.stream()
//                .mapToInt(jsonString -> {
//                    if (jsonString == null || jsonString.isEmpty()) {
//                        return 0;
//                    }
//                    try {
//                        EdgeData edgeData = objectMapper.readValue(jsonString, EdgeData.class);
//                        return edgeData.getVehicleCount();
//                    } catch (JsonProcessingException e) {
//                        log.error("Failed to parse edge data JSON: {}", jsonString, e);
//                        return 0;
//                    }
//                })
//                .max()
//                .orElse(0);
//    }


//    private int calculateMaxCongestionForJunction(List<JunctionIncomingEdge> incomingEdges) {
//        if (incomingEdges == null || incomingEdges.isEmpty()) {
//            return 0;
//        }
//
//        List<String> redisKeys = incomingEdges.stream()
//                .map(edge -> REDIS_EDGE_KEY_PREFIX + edge.getIncomingEdgeId())
//                .collect(Collectors.toList());
//
//        List<String> edgeJsonValues = redisTemplate.opsForValue().multiGet(redisKeys);
//        if (edgeJsonValues == null) {
//            return 0;
//        }
//
//        double maxOccupancy = 0.0;
//        int vehicleCountAtMaxOccupancy = 0;
//
//        for (String jsonString : edgeJsonValues) {
//            if (jsonString == null || jsonString.isEmpty()) {
//                continue;
//            }
//
//            try {
//                EdgeData edgeData = objectMapper.readValue(jsonString, EdgeData.class);
//                float occupancy = edgeData.getOccupancy();
//
//                log.info("Evaluating edge for congestion - occupancy: {}, vehicleCount: {}, edgeData: {}",
//                        occupancy, edgeData.getVehicleCount(), edgeData);
//
//                if (occupancy > maxOccupancy) {
//                    maxOccupancy = occupancy;
//                    vehicleCountAtMaxOccupancy = edgeData.getVehicleCount();
//                }
//
//            } catch (JsonProcessingException e) {
//                log.error("Failed to parse edge data JSON: {}", jsonString, e);
//            }
//        }
//
//        return maxOccupancy > OCCUPANCY_THRESHOLD ? vehicleCountAtMaxOccupancy : 0;
//    }

    private int calculateMaxCongestionForJunction(List<JunctionIncomingEdge> incomingEdges) {
        if (incomingEdges == null || incomingEdges.isEmpty()) {
            return 0;
        }

        // 获取所有的 edgeId（hash 的 field）
        List<String> edgeIds = incomingEdges.stream()
                .map(JunctionIncomingEdge::getIncomingEdgeId)
                .collect(Collectors.toList());

        // 使用 opsForHash().multiGet 从 Redis 的 hash 结构中读取数据
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

//                log.info("Evaluating edge for congestion - occupancy: {}, vehicleCount: {}, edgeData: {}",
//                        occupancy, edgeData.getVehicleCount(), edgeData);

                if (occupancy > maxOccupancy) {
                    maxOccupancy = occupancy;
                    vehicleCountAtMaxOccupancy = edgeData.getVehicleCount();
                }

            } catch (JsonProcessingException e) {
                log.error("Failed to parse edge data JSON: {}", jsonString, e);
            }
        }

        return maxOccupancy > OCCUPANCY_THRESHOLD ? vehicleCountAtMaxOccupancy : 0;
    }
}