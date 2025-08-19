package com.ucd.urbanflow.cache;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple, thread-safe in-memory cache for storing the status of events.
 * This avoids frequent database queries in real-time push tasks.
 */
@Component
public class EventStatusCache {

    private final Map<String, String> statusCache = new ConcurrentHashMap<>();

    /**
     * Retrieves the status of an event.
     * @param eventId The ID of the event.
     * @return an Optional containing the status, or an empty Optional if it's not in the cache.
     */
    public Optional<String> getStatus(String eventId) {
        return Optional.ofNullable(statusCache.get(eventId));
    }

    /**
     * Sets or updates the status of an event.
     * @param eventId The ID of the event.
     * @param status The new status.
     */
    public void setStatus(String eventId, String status) {
        statusCache.put(eventId, status);
    }

    /**
     * Removes an event from the cache when it has concluded.
     * @param eventId The ID of the event.
     */
    public void removeStatus(String eventId) {
        statusCache.remove(eventId);
    }
}
