package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.entity.SpecialEventSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;


@Mapper
public interface SpecialEventMapper {


    List<SpecialEventSchedule> findPendingEventsByTriggerTime(@Param("currentTime") int currentTime);
    

    List<SpecialEventSchedule> findTriggeredEventsToFinish(@Param("currentTime") int currentTime);
    

    int updateEventStatus(@Param("eventId") String eventId, @Param("status") String status);
}