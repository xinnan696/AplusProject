package com.ucd.urbanflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 配置HTTP请求授权规则
                .authorizeHttpRequests(authz -> authz
                        // 对所有以 "/api/traffic/" 开头的请求，允许所有用户访问（无需认证）
                        .requestMatchers("/api/traffic/**").permitAll()
                        // 对于任何其他未明确匹配的请求，都要求用户必须经过认证
                        .anyRequest().authenticated()
                )
                // 启用HTTP Basic认证，用于访问受保护的资源
                .httpBasic(withDefaults());

        return http.build();
    }
}