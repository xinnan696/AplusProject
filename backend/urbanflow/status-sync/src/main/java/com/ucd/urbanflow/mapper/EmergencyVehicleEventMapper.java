package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.model.EmergencyVehicleEvent;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface EmergencyVehicleEventMapper {
    
    List<EmergencyVehicleEvent> selectAllEvents();
}
