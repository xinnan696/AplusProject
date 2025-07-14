package com.ucd.urbanflow.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * VO for successful login response payload.
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private UserVO user;

}