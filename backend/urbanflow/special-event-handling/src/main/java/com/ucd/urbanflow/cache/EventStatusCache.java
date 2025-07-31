package com.ucd.urbanflow.cache;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个简单的、线程安全的内存缓存，用于存储事件的状态。
 * 避免了在实时推送任务中频繁查询数据库。
 */
@Component
public class EventStatusCache {

    private final Map<String, String> statusCache = new ConcurrentHashMap<>();

    /**
     * 获取一个事件的状态。
     * @param eventId 事件ID
     * @return 包含状态的Optional，如果缓存中不存在则为空
     */
    public Optional<String> getStatus(String eventId) {
        return Optional.ofNullable(statusCache.get(eventId));
    }

    /**
     * 设置或更新一个事件的状态。
     * @param eventId 事件ID
     * @param status 新的状态
     */
    public void setStatus(String eventId, String status) {
        statusCache.put(eventId, status);
    }

    /**
     * 当事件结束时，从缓存中移除。
     * @param eventId 事件ID
     */
    public void removeStatus(String eventId) {
        statusCache.remove(eventId);
    }
}
