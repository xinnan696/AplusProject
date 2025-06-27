package com.ucd.urbanflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.model.MapRealtimeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public MapRealtimeData getRealtimeData() {
        try {
            String json = redisTemplate.opsForValue().get("map:realtime");
            if (json == null) return new MapRealtimeData();
            return objectMapper.readValue(json, MapRealtimeData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new MapRealtimeData();
        }
    }
}

