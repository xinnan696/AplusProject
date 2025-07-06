package com.ucd.urbanflow.event.mapper;

import com.ucd.urbanflow.event.entity.SpecialEventSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Special Event Schedule Data Access Interface
 */
@Mapper
public interface SpecialEventMapper {
    
    /**
     * Find pending events by trigger time
     * @param currentTime Current simulation time
     * @return List of pending events
     */
    List<SpecialEventSchedule> findPendingEventsByTriggerTime(@Param("currentTime") int currentTime);
    
    /**
     * Find triggered events that should finish
     * @param currentTime Current simulation time  
     * @return List of events to finish
     */
    List<SpecialEventSchedule> findTriggeredEventsToFinish(@Param("currentTime") int currentTime);
    
    /**
     * Update event status
     * @param eventId Event ID
     * @param status New status
     * @return Number of updated rows
     */
    int updateEventStatus(@Param("eventId") String eventId, @Param("status") String status);
}