package com.ucd.urbanflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.model.JunctionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class Redis {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static int encodeLightState(String state) {
        if (state == null) return 0;
        switch (state.toLowerCase()) {
            case "g":
                return 0;
            case "r":
                return 1;
            case "y":
                return 2;
            default:
                return 0;
        }
    }


    public List<JunctionStatus> getAllJunctionStatus() {
        Map<Object, Object> all = redisTemplate.opsForHash().entries("sumo:junction");
        List<JunctionStatus> result = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : all.entrySet()) {
            try {
                String junctionid = entry.getKey().toString();
                String jsonStr = entry.getValue().toString();

                Map<String, Object> map = objectMapper.readValue(jsonStr, Map.class);
                JunctionStatus status = objectMapper.readValue(jsonStr, JunctionStatus.class);
                status.setJunctionid(junctionid);

                if (map.get("edge1_light_state") != null) {
                    status.setEdge1LightState(encodeLightState(map.get("edge1_light_state").toString()));
                }

                if (map.get("edge2_light_state") != null) {
                    status.setEdge2LightState(encodeLightState(map.get("edge2_light_state").toString()));
                }

                result.add(status);
            } catch (Exception e) {
                System.err.println("wrong data: field" + entry.getKey() + ", value=" + entry.getValue());
                e.printStackTrace();
            }
        }
        return result;
    }
}
