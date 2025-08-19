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
                System.err.println("[RedisService] Failed to parse edge JSON for key: " + entry.getKey());
                e.printStackTrace();
            }
        }

        return result;
    }


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
                System.err.println("[RedisService] Failed to parse junction JSON for key: " + entry.getKey());
                System.err.println(" Value: " + entry.getValue());
                e.printStackTrace();
            }
        }

        return result;
    }
}
