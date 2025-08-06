package com.ucd.urbanflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.domain.dto.JunctionCongestionDTO;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.domain.vo.EdgeData;
import com.ucd.urbanflow.mapper.JunctionMapper;
import com.ucd.urbanflow.mapper.JunctionRegionsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficServiceTest {

    @Mock
    private JunctionMapper junctionMapper;
    @Mock
    private JunctionRegionsMapper junctionRegionsMapper;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TrafficService trafficService;

    @Test
    void getCongestedJunctions_calculatesAndSortsCorrectly() throws Exception {
        // 1. Arrange
        // Mock the database call to get the list of all junctions.
        JunctionIncomingEdge j1e1 = new JunctionIncomingEdge();
        j1e1.setJunctionId("J1");
        j1e1.setJunctionName("Main St");
        j1e1.setIncomingEdgeId("j1e1"); // Edge for J1

        JunctionIncomingEdge j2e1 = new JunctionIncomingEdge();
        j2e1.setJunctionId("J2");
        j2e1.setJunctionName("Park Lane");
        j2e1.setIncomingEdgeId("j2e1"); // Edge for J2

        when(junctionMapper.findAllJunctionEdges()).thenReturn(Arrays.asList(j1e1, j2e1));

        // Mock the Redis data for the edges
        EdgeData congestedEdgeData = new EdgeData(); // Junction 1 IS congested (occupancy > 0.6)
        congestedEdgeData.setOccupancy(0.8f);
        congestedEdgeData.setVehicleCount(50);
        String congestedJson = objectMapper.writeValueAsString(congestedEdgeData);

        EdgeData nonCongestedEdgeData = new EdgeData(); // Junction 2 is NOT congested (occupancy <= 0.6)
        nonCongestedEdgeData.setOccupancy(0.4f);
        nonCongestedEdgeData.setVehicleCount(20);
        String nonCongestedJson = objectMapper.writeValueAsString(nonCongestedEdgeData);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        when(hashOperations.multiGet(any(), anyList())).thenAnswer(invocation -> {
            List<String> requestedEdgeIds = invocation.getArgument(1);
            if (requestedEdgeIds.contains("j1e1")) {
                return Collections.singletonList(congestedJson);
            }
            if (requestedEdgeIds.contains("j2e1")) {
                return Collections.singletonList(nonCongestedJson);
            }
            return Collections.emptyList();
        });

        // 2. Act
        // Call the public method.
        List<JunctionCongestionDTO> result = trafficService.getCongestedJunctions();

        // 3. Assert
        assertEquals(1, result.size());
        assertEquals("J1", result.get(0).getJunctionId());
        assertEquals(50, result.get(0).getCongestionCount());
    }
}