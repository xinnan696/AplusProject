package com.ucd.urbanflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.Socket;

@RestController
@RequestMapping("/api/redis")
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/test")
    public String testRedis() {
        System.out.println("==== RedisTest Controller Loaded! ====");
        try{
            redisTemplate.opsForValue().set("test:hello", "world");
            return "Redis：" + redisTemplate.opsForValue().get("test:hello");
        } catch (Exception e) {
            e.printStackTrace();
            return "Error:" + e.getMessage();
        }

    }

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private String redisPort;

    @GetMapping("/showConfig")
    public String showRedisConfig() {
        return "host=" + redisHost + " port=" + redisPort;
    }

    @GetMapping("/socketTest")
    public String socketTest() {
        try (Socket s = new Socket("192.168.83.189", 6379)) {
            return "Socket connect success!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Socket ERROR: " + e.getMessage();
        }
    }

}
