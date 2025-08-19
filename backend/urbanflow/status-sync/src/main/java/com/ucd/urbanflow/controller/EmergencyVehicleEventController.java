package com.ucd.urbanflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api-status")
public class EmergencyVehicleEventController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();


    @GetMapping("/emergency-routes")
    public ResponseEntity<List<Map<String, Object>>> getEmergencyRoutes() {
        try {
            String sql = "SELECT * FROM emergency_vehicle_events ORDER BY created_at DESC";
            
            List<Map<String, Object>> events = jdbcTemplate.queryForList(sql);
            

            for (Map<String, Object> event : events) {

                Object routeEdgesObj = event.get("route_edges");
                if (routeEdgesObj != null) {
                    try {
                        List<String> routeEdges = objectMapper.readValue(routeEdgesObj.toString(), List.class);
                        event.put("route_edges", routeEdges);
                    } catch (Exception e) {
                        System.err.println("Failed to parse route_edges: " + e.getMessage());
                        event.put("route_edges", new ArrayList<>());
                    }
                }

                Object junctionsObj = event.get("signalized_junctions");
                if (junctionsObj != null) {
                    try {
                        List<String> junctions = objectMapper.readValue(junctionsObj.toString(), List.class);
                        event.put("signalized_junctions", junctions);
                    } catch (Exception e) {
                        System.err.println("Failed to parse signalized_junctions: " + e.getMessage());
                        event.put("signalized_junctions", new ArrayList<>());
                    }
                }
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
