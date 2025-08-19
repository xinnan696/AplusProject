package com.ucd.urbanflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * This component runs automatically once after application startup to test the Redis connection
 */
@Component
@RequiredArgsConstructor
public class RedisConnectionTester implements CommandLineRunner {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. Attempt to send a PING command to Redis
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();

            // 2. Check the response
            if ("PONG".equalsIgnoreCase(pong)) {
                System.out.println("==========================================================");
                System.out.println("Redis Connection test successful！ PING -> PONG");
                System.out.println("==========================================================");
            } else {
                System.err.println("==========================================================");
                System.err.println("Connection test failed: received a response other than PONG: " + pong);
                System.err.println("==========================================================");
            }
        } catch (Exception e) {
            System.err.println("==========================================================");
            System.err.println("Redis connection test failed: unable to connect to the Redis server。");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("==========================================================");
        }
    }
}
