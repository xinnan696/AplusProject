package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.Junction;
import com.ucd.urbanflow.mapper.JunctionMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JunctionNameCache {

    @Autowired
    private JunctionMapper junctionMapper;

    private Map<String, String> junctionNameMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeCache() {
        log.info("Initializing JunctionNameCache...");
        List<Junction> allJunctions = junctionMapper.findAllJunctions();
        if (allJunctions != null && !allJunctions.isEmpty()) {
            this.junctionNameMap = allJunctions.stream()
                    .collect(Collectors.toMap(Junction::getJunctionId, Junction::getJunctionName, (existing, replacement) -> existing));
            log.info("Successfully loaded {} junction name mappings into cache.", this.junctionNameMap.size());
        } else {
            log.warn("No junction name mappings found in the database to cache.");
        }
    }

    public String getName(String junctionId) {
        if (junctionId == null) return "N/A";
        return junctionNameMap.getOrDefault(junctionId, junctionId);
    }
}