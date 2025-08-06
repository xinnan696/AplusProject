package com.ucd.urbanflow.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventStatusCache 的单元测试
 * 目的: 验证这个简单的并发缓存的 set, get, remove 功能是否正常工作。
 */
class EventStatusCacheTest {

    private EventStatusCache eventStatusCache;

    @BeforeEach
    void setUp() {
        // 在每个测试方法执行前，都创建一个新的实例，避免测试间的状态污染
        eventStatusCache = new EventStatusCache();
    }

    @Test
    void testSetAndGetStatus() {
        // Arrange: 准备测试数据
        String eventId = "evt-123";
        String status = "triggering";

        // Act: 执行被测试的方法
        eventStatusCache.setStatus(eventId, status);
        Optional<String> retrievedStatus = eventStatusCache.getStatus(eventId);

        // Assert: 验证结果
        assertTrue(retrievedStatus.isPresent(), "状态应该存在于缓存中");
        assertEquals(status, retrievedStatus.get(), "获取到的状态应与设置的相符");
    }

    @Test
    void testGetStatus_WhenNotFound() {
        // Arrange: 一个不存在的 eventId
        String eventId = "evt-non-existent";

        // Act: 尝试获取一个不存在的状态
        Optional<String> retrievedStatus = eventStatusCache.getStatus(eventId);

        // Assert: 验证结果
        assertFalse(retrievedStatus.isPresent(), "对于不存在的事件，应返回一个空的Optional");
    }

    @Test
    void testRemoveStatus() {
        // Arrange: 先在缓存中设置一个值
        String eventId = "evt-to-remove";
        String status = "completed";
        eventStatusCache.setStatus(eventId, status);

        // Act: 执行移除操作
        eventStatusCache.removeStatus(eventId);
        Optional<String> retrievedStatus = eventStatusCache.getStatus(eventId);

        // Assert: 验证它是否已被成功移除
        assertFalse(retrievedStatus.isPresent(), "状态被移除后，不应该再能从缓存中获取到");
    }
}