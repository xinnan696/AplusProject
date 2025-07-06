package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.service.EventSchedulerEvent;
import com.ucd.urbanflow.websocket.TraCIEventDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    @Autowired
    private TraCIEventDispatcher dispatcher;

//    send event
    public void triggerEvent(EventSchedulerEvent event) {
        dispatcher.sendEvent(event);
    }

//    receive results
    public void handleEventResult(
            String status,
            String eventType,
            String msg,
            List<String> vehicleIds,
            List<String> laneIds,
            String fullJson) {
        System.out.println("EventResult: status=" + status + ", eventType=" + eventType + ", msg=" + msg);
    }
}
