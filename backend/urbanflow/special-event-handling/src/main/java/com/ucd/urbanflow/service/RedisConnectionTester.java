package com.ucd.urbanflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 这个组件会在应用启动后自动运行一次，用于测试与Redis的连接。
 */
@Component
@RequiredArgsConstructor
public class RedisConnectionTester implements CommandLineRunner {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. 尝试向Redis发送一个 PING 命令
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();

            // 2. 检查响应
            if ("PONG".equalsIgnoreCase(pong)) {
                System.out.println("==========================================================");
                System.out.println("✅ Redis 连接测试成功！ PING -> PONG");
                System.out.println("==========================================================");
            } else {
                System.err.println("==========================================================");
                System.err.println("❌ Redis 连接测试异常，收到的响应不是 PONG: " + pong);
                System.err.println("==========================================================");
            }
        } catch (Exception e) {
            System.err.println("==========================================================");
            System.err.println("❌ Redis 连接测试失败！无法连接到Redis服务器。");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("==========================================================");
        }
    }
}
