package com.ucd.urbanflow.domain.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiResponse<T> {

    private int statusCode;
    private String message;
    private long timestamp;
    private T data;

    private ApiResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return new ApiResponse<>(statusCode, message, null);
    }
}
