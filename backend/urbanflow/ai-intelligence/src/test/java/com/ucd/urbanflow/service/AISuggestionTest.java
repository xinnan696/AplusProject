package com.ucd.urbanflow.service;

import com.ucd.urbanflow.model.JunctionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AISuggestionTest {

    @InjectMocks
    private AISuggestion aiSuggestion;

    @Mock
    private RestTemplate restTemplate;

    @Value("${ai.fastapi.url.batch}")
    private String fastapiUrl = "http://localhost:8001/batch";

    @Test
    void testCallFastApiBatch_success_with_filtering() throws Exception{
        String fakeJson = """
            {
              "batch_suggestions": [
                [
                  {"suggestion_label": 0, "junction":"A"},
                  {"suggestion_label": 1, "junction":"B"},
                  {"suggestion_label": 4, "junction":"C"}
                ]
              ]
            }
        """;

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(fakeJson));

        String result = aiSuggestion.callFastApiBatch(List.of(new JunctionStatus()));

        assertTrue(result.contains("B"));
        assertFalse(result.contains("A"));
        assertFalse(result.contains("C"));
    }

    @Test
    void testCallFastApiBatch_fail_to_parse() {

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not a json"));

        String result = aiSuggestion.callFastApiBatch(List.of(new JunctionStatus()));
        assertTrue(result.contains("fail to parse"));
    }
}
