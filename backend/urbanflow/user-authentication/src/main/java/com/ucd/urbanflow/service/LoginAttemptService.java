package com.ucd.urbanflow.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service to track and block login attempts to prevent brute-force attacks.
 * Uses a time-based cache.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_ATTEMPTS = 3;
    private final LoadingCache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        // Initialize a cache that expires entries 5 minutes after they are written.
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void loginFailed(String key) {
        int attempts = 0;
        try {
            attempts = attemptsCache.get(key);
        } catch (ExecutionException e) {
            // Should not happen, but default to 0
        }
        attempts++;
        attemptsCache.put(key, attempts);
    }

    public void loginSucceeded(String key) {
        attemptsCache.invalidate(key);
    }

    public boolean isBlocked(String key) {
        try {
            return attemptsCache.get(key) >= MAX_ATTEMPTS;
        } catch (ExecutionException e) {
            return false;
        }
    }
}