package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.EmergencyVehicleEventMapper;
import com.ucd.urbanflow.model.EmergencyVehicleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmergencyVehicleEventService {

    @Autowired
    private EmergencyVehicleEventMapper emergencyVehicleEventMapper;

    public List<EmergencyVehicleEvent> getAllEvents() {
        return emergencyVehicleEventMapper.selectAllEvents();
    }
}
