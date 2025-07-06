package com.ucd.urbanflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ManualControlResponse<T> {
    private int code;
    private String msg;
    private T body;

    public static <T> ManualControlResponse <T> success(String msg, T body) {
        return new ManualControlResponse<>(200, msg, body);
    }

    public static <T> ManualControlResponse <T> fail(int code, String msg) {
        return new ManualControlResponse<>(code, msg, null);
    }
}
