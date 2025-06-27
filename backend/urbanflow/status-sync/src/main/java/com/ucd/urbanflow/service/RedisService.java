package com.ucd.urbanflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.model.Edge;
import com.ucd.urbanflow.model.Junction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ✅ 读取 Redis 中所有 edge（Hash 存储）
    public Map<String, Edge> getAllEdgesFromHash() {
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries("sumo:edge");
        Map<String, Edge> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            try {
                String key = entry.getKey().toString();
                String json = entry.getValue().toString();
                Edge edge = objectMapper.readValue(json, Edge.class);
                result.put(key, edge);
            } catch (Exception e) {
                System.err.println("❌ [RedisService] Failed to parse edge JSON for key: " + entry.getKey());
                e.printStackTrace();
            }
        }

        return result;
    }

    // ✅ 读取 Redis 中所有 junction（Hash 存储）
    public Map<String, Junction> getAllJunctionsFromHash() {
        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries("sumo:tls");
        Map<String, Junction> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            try {
                String key = entry.getKey().toString();
                String json = entry.getValue().toString();
                Junction junction = objectMapper.readValue(json, Junction.class);
                result.put(key, junction);
            } catch (Exception e) {
                System.err.println("❌ [RedisService] Failed to parse junction JSON for key: " + entry.getKey());
                System.err.println("🔍 Value: " + entry.getValue());  // 加上这行打印原始 Redis 字符串
                e.printStackTrace();
            }
        }

        return result;
    }
}
