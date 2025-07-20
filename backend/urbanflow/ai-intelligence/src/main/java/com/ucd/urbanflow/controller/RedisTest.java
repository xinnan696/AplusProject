package com.ucd.urbanflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/test")
    public String testRedis() {
        System.out.println("==== RedisTest Controller Loaded! ====");
        redisTemplate.opsForValue().set("test:hello", "world");
        return "Redis：" + redisTemplate.opsForValue().get("test:hello");
    }

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private String redisPort;

    @GetMapping("/api/redis/showConfig")
    public String showRedisConfig() {
        return "host=" + redisHost + " port=" + redisPort;
    }

}
