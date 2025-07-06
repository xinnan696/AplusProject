package com.ucd.urbanflow;

import redis.clients.jedis.Jedis;


public class RedisConnectionTest {
    public static void main(String[] args) {
        // 替换为主机的 ZeroTier 虚拟 IP 和端口
        String redisHost = "10.241.72.201";
        int redisPort = 6379;
        // 如果设置了密码，添加 String password = "yourpassword";

        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            // 如果 Redis 设置了密码，需要认证：
            // jedis.auth(password);

            String pong = jedis.ping();
            if ("PONG".equals(pong)) {
                System.out.println("Redis 连接成功！");
            } else {
                System.out.println("Redis 连接失败，返回: " + pong);
            }
        } catch (Exception e) {
            System.out.println("无法连接到 Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


