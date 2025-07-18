package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EmergencyVehicleMapper {
    List<EmergencyVehicleEvent> findPendingEventsByTriggerTime(@Param("currentTime") long currentTime);
    int updateEventStatus(@Param("eventId") String eventId, @Param("status") String status);
}
