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
    // 根据事件ID查找单个紧急车辆事件的静态信息
    Optional<EmergencyVehicleStaticDataDto> findByEventId(@Param("eventId") String eventId);
}
