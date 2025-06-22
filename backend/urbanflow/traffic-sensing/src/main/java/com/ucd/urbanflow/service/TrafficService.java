package com.ucd.urbanflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.domain.dto.JunctionCongestionDTO;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.domain.vo.EdgeData;
import com.ucd.urbanflow.mapper.JunctionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficService {

    private final JunctionMapper junctionMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_EDGE_KEY_PREFIX = "sumo:edge:";
    private static final int TOP_N_JUNCTIONS = 6;

    // 定义用于缓存计算结果的 Redis Key
    private static final String CACHE_KEY_CONGESTED_JUNCTIONS = "traffic:cache:top6_congested_junctions";

    /**
     * 【无需修改】此方法为原始的REST API提供服务。
     * 它现在调用重构后的核心计算方法。
     */
    public List<JunctionCongestionDTO> getCongestedJunctions() {
        return this.calculateTopCongestedJunctions();
    }

    /**
     * 【新增】计划任务方法。
     * 此方法会按照 application.properties 中配置的频率自动执行。
     */
    @Scheduled(fixedRateString = "${traffic.cache.update-rate-ms}")
    public void updateCongestionCache() {
        log.info("Running scheduled task: Calculating and caching congested junctions...");
        try {
            // 1. 执行核心计算
            List<JunctionCongestionDTO> topJunctions = this.calculateTopCongestedJunctions();

            // 2. 将结果列表序列化为JSON字符串
            String jsonCacheData = objectMapper.writeValueAsString(topJunctions);

            // 3. 将JSON字符串存入指定的Redis Key中
            redisTemplate.opsForValue().set(CACHE_KEY_CONGESTED_JUNCTIONS, jsonCacheData);

            log.info("Successfully updated congested junctions cache. Found {} junctions.", topJunctions.size());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize congestion data to JSON for caching.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during congestion cache update.", e);
        }
    }

    /**
     * 【重构】核心计算逻辑。
     * 此私有方法被API和计划任务共同调用。
     * @return A list of the top N congested junctions.
     */
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
                .limit(TOP_N_JUNCTIONS)
                .collect(Collectors.toList());
    }

    private int calculateMaxCongestionForJunction(List<JunctionIncomingEdge> incomingEdges) {

        if (incomingEdges == null || incomingEdges.isEmpty()) {
            return 0;
        }
        List<String> redisKeys = incomingEdges.stream()
                .map(edge -> REDIS_EDGE_KEY_PREFIX + edge.getIncomingEdgeId())
                .collect(Collectors.toList());
        List<String> edgeJsonValues = redisTemplate.opsForValue().multiGet(redisKeys);
        if (edgeJsonValues == null) {
            return 0;
        }
        return edgeJsonValues.stream()
                .mapToInt(jsonString -> {
                    if (jsonString == null || jsonString.isEmpty()) {
                        return 0;
                    }
                    try {
                        EdgeData edgeData = objectMapper.readValue(jsonString, EdgeData.class);
                        return edgeData.getVehicleCount();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse edge data JSON: {}", jsonString, e);
                        return 0;
                    }
                })
                .max()
                .orElse(0);
    }
}