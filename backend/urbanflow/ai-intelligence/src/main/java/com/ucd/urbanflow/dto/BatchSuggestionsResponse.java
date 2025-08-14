package com.ucd.urbanflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BatchSuggestionsResponse {
    @JsonProperty("batch_suggestions")
    private List<List<SuggestionItem>> batchSuggestions;
}
