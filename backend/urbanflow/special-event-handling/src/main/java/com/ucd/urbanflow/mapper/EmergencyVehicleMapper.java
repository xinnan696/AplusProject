package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.entity.EmergencyVehicleEvent;
import com.ucd.urbanflow.dto.EmergencyVehicleStaticDataDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface EmergencyVehicleMapper {
    List<EmergencyVehicleEvent> findPendingEventsByTriggerTime(@Param("currentTime") long currentTime);
    int updateEventStatus(@Param("eventId") String eventId, @Param("status") String status);
    // Find the static information of a single emergency vehicle event by its event ID
    Optional<EmergencyVehicleStaticDataDto> findByEventId(@Param("eventId") String eventId);
}
