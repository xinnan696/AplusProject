package com.ucd.urbanflow.service;

import com.ucd.urbanflow.dto.service.ManualControlRequest;
import com.ucd.urbanflow.dto.service.ManualControlResponse;
import com.ucd.urbanflow.dto.service.TraCIClient;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class SignalControlService {
//    private static final Logger logger = LoggerFactory.getLogger(SignalControlService.class);
    private static final long MIN_INTERVAL_SECONDS=5;
    private static final String CONTROL_TIME_PREFIX = "last_control_time:";

    @Autowired
    private TraCIClient traCIClient;

//    @Autowired
//    private StringRedisTemplate redisTemplate;

    public ResponseEntity<ManualControlResponse<?>> handleManualControl(ManualControlRequest request){
        String junctionId = request.getJunctionId();
        String state = request.getState();
        Integer duration = request.getDuration();
        Integer lightIndex = request.getLightIndex();

//        logger.info("Received Request: {}", request);

//        // check frequency from Redis
//        String redisKey = CONTROL_TIME_PREFIX + junction_id;
//        String lastTimeStr = redisTemplate.opsForValue().get(redisKey);
//        long currentTime = Instant.now().getEpochSecond();
//
//        if (lastTimeStr != null){
//            long lastTime = Long.parseLong(lastTimeStr);
//            if ((currentTime - lastTime) < MIN_INTERVAL_SECONDS){
//                return ResponseEntity.status(429).body(
//                        ManualControlResponse.fail(429, "Control too frequent. Please wait a few seconds.")
//                );
//            }
//        }

        // check SUMO connection status
        if (!traCIClient.checkSUMOStatus()){
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ManualControlResponse.fail(503, "SUMO is not connected.")
            );
        }

        // check junction exists
        if (!traCIClient.checkJunctionExists(junctionId)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ManualControlResponse.fail(400, "Invalid junction ID"));
        }

        // call traci
        boolean controlSuccess = false;
        if ((state == null || state.trim().isEmpty()) && duration != null){
            // only duration is provided
            controlSuccess = traCIClient.setSignalDuration(junctionId, duration);
        }
        else if (state != null && !state.trim().isEmpty() && duration != null && lightIndex != null){
            controlSuccess = traCIClient.setSignalStateAndDuration(junctionId, lightIndex, state, duration);
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ManualControlResponse.fail(400, "Invalid control request: duration or state missing."));
        }

        if (!controlSuccess){
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ManualControlResponse.fail(500, "Failed to control signal."));
        }

//        // update redis control time
//        redisTemplate.opsForValue().set(redisKey, String.valueOf(now), 10, TimeUnit.MINUTES);
//
        // return success
        return ResponseEntity.ok(ManualControlResponse.success("Signal control executed successfully.", null));
    }
}