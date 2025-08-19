package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.model.JunctionStatus;
import com.ucd.urbanflow.service.AISuggestion;
import com.ucd.urbanflow.service.Redis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrafficSuggestionController.class)
public class TrafficSuggestionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Redis redis;

    @MockBean
    private AISuggestion aiSuggestion;

    @Test
    void testSuggestion_success() throws Exception{
        List<JunctionStatus> mockStatusList = List.of(new JunctionStatus());

        when(redis.getAllJunctionStatus()).thenReturn(mockStatusList);
        when(aiSuggestion.callFastApiBatch(mockStatusList)).thenReturn("{\"mock\":\"result\"}");

        mockMvc.perform(get("/api/traffic/suggestion")).andExpect(status().isOk()).andExpect(content().string("{\"mock\":\"result\"}"));
    }

    @Test
    void testSuggestion_noData() throws Exception{
        when(redis.getAllJunctionStatus()).thenReturn(List.of());

        mockMvc.perform(get("/api/traffic/suggestion"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("NO DATA IN REDIS"));
    }

    @Test
    void testSuggestion_exception() throws Exception{
        when(redis.getAllJunctionStatus()).thenThrow(new RuntimeException("Redis Error"));

        mockMvc.perform(get("/api/traffic/suggestion")).andExpect(status().isInternalServerError()).andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to get suggestions")));
    }
}
