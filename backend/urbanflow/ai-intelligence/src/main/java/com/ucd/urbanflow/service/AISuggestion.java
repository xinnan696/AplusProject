package com.ucd.urbanflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.BatchSuggestionsResponse;
import com.ucd.urbanflow.dto.SuggestionItem;
import com.ucd.urbanflow.model.JunctionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
public class AISuggestion {
    @Value("${ai.fastapi.url.batch}")
    private String fastapiUrlBatch;


    public String callFastApiBatch(List<JunctionStatus> statusList) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<JunctionStatus>> request = new HttpEntity<>(statusList, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                fastapiUrlBatch, request, String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        BatchSuggestionsResponse batchSuggestionResponse;

        try{
            batchSuggestionResponse = objectMapper.readValue(responseBody, BatchSuggestionsResponse.class);
            for (List<SuggestionItem> list : batchSuggestionResponse.getBatchSuggestions()) {
                list.removeIf(item -> item.getSuggestionLabel() == 0 || item.getSuggestionLabel() == 4);
            }

            return objectMapper.writeValueAsString(batchSuggestionResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"fail to parse AI response\"}";
        }

    }
    }
