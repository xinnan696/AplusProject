package com.ucd.urbanflow.service;

import com.google.gson.Gson;
import com.ucd.urbanflow.domain.dto.RedisEdgeData;
import com.ucd.urbanflow.domain.pojo.JunctionIncomingEdge;
import com.ucd.urbanflow.mapper.JunctionMapper;
import com.ucd.urbanflow.service.dataprocessing.RedisPollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RedisPollingService.
 * Focuses on new data detection and event enrichment logic.
 */
@ExtendWith(MockitoExtension.class)
class RedisPollingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private JunctionMapper junctionMapper;

    @Spy // Use @Spy for Gson to use the real implementation
    private Gson gson = new Gson();

    @InjectMocks
    private RedisPollingService redisPollingService;

    @Test
    void pollRedisForChanges_emitsOnlyNewEvents() {
        // 1. Arrange
        // Mock the junction mapping from the database
        JunctionIncomingEdge mapping = new JunctionIncomingEdge();
        mapping.setJunctionId("J1");
        mapping.setJunctionName("Main St");
        mapping.setIncomingEdgeId("edge1");
        when(junctionMapper.findAllJunctionEdges()).thenReturn(Collections.singletonList(mapping));

        // Run the mapping logic to populate the service's internal cache
        redisPollingService.updateJunctionToEdgesMap();

        // Mock the data that will be returned from Redis
        RedisEdgeData oldData = new RedisEdgeData();
        oldData.setEdgeId("edge1");
        oldData.setTimestamp(100.0);

        RedisEdgeData newData = new RedisEdgeData();
        newData.setEdgeId("edge1");
        newData.setTimestamp(101.0);

        Map<Object, Object> redisDataMap = Map.of("edge1", gson.toJson(oldData));
        Map<Object, Object> updatedRedisDataMap = Map.of("edge1", gson.toJson(newData));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        // First poll returns old data, second poll returns new data
        when(hashOperations.entries(anyString())).thenReturn(redisDataMap).thenReturn(updatedRedisDataMap);

        // 2. Act & Assert
        // The first poll should emit one event
        redisPollingService.pollRedisForChanges();

        // The second poll should also emit one event because the timestamp has changed
        redisPollingService.pollRedisForChanges();

        // The third poll has the same data as the second, so it should emit nothing new
        redisPollingService.pollRedisForChanges();

        // Use StepVerifier to test the reactive stream (the Flux)
        StepVerifier.create(redisPollingService.getEventStream())
                .expectNextCount(2) // We expect a total of 2 events to have been published
                .thenCancel()
                .verify();
    }
}