package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.config.AuthenticatedUser;
import com.ucd.urbanflow.dto.ManualControlRequest;
import com.ucd.urbanflow.dto.ManualControlResponse;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.TraCIClient;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SignalControlService {
//    private static final Logger logger = LoggerFactory.getLogger(SignalControlService.class);
    private static final long MIN_INTERVAL_SECONDS=5;
    private static final String CONTROL_TIME_PREFIX = "last_control_time:";

    @Autowired
    private TraCIClient traCIClient;

    @Autowired
    private LogServiceClient logServiceClient;

    @Autowired
    private JunctionNameCache junctionNameCache;

//    @Autowired
//    private StringRedisTemplate redisTemplate;

    public ResponseEntity<ManualControlResponse<?>> handleManualControl(ManualControlRequest request){
        String junctionId = request.getJunctionId();
        String state = request.getState();
        Integer duration = request.getDuration();
        Integer lightIndex = request.getLightIndex();

        //user authentication info
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();

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
            recordLog(currentUser, request, "FAILURE", "SUMO is not connected.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ManualControlResponse.fail(503, "SUMO is not connected.")
            );
        }

        // check junction exists
        if (!traCIClient.checkJunctionExists(junctionId)){
            recordLog(currentUser, request, "FAILURE", "Invalid junction ID provided.");
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
            recordLog(currentUser, request, "FAILURE", "Invalid control request: At least duration, or a full state, must be provided.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ManualControlResponse.fail(400, "Invalid control request: duration or state missing."));
        }

        if (!controlSuccess){
            recordLog(currentUser, request, "FAILURE", "TraCI client failed to execute the signal control command.");
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ManualControlResponse.fail(500, "Failed to control signal."));
        }

//        // update redis control time
//        redisTemplate.opsForValue().set(redisKey, String.valueOf(now), 10, TimeUnit.MINUTES);
//
        // return success
        recordLog(currentUser, request, "SUCCESS", null);
        return ResponseEntity.ok(ManualControlResponse.success("Signal control executed successfully.", null));
    }


    private void recordLog(AuthenticatedUser user, ManualControlRequest request, String result, String failureReason) {
        SignalControlLogDTO logDTO = new SignalControlLogDTO();

        logDTO.setAccountNumber(user.getAccountNumber());
//        logDTO.setUserName(user.getUserName());
        logDTO.setJunctionId(request.getJunctionId());
        logDTO.setLightIndex(request.getLightIndex());
        logDTO.setLightState(request.getState());
        logDTO.setDuration(request.getDuration());
        logDTO.setOperationSource(request.getSource());
        logDTO.setOperationResult(result);

        String junctionName = junctionNameCache.getName(request.getJunctionId());

        String message;
        if ("SUCCESS".equals(result)) {
            if (request.getState() != null && !request.getState().isEmpty()) {
                message = String.format("Successfully set Junction(%s) Phase(%d) to State(%s) for a Duration(%d seconds).",
                        junctionName, request.getLightIndex(), request.getState(), request.getDuration());
            } else {
                message = String.format("Successfully set signal Duration(%d seconds) for Junction(%s).",
                        request.getDuration(), junctionName);
            }
        } else {
            message = String.format("Failed to control Junction(%s). Reason: %s", junctionName, failureReason);
        }
        logDTO.setResultMessage(message);

        logServiceClient.logSignalControl(logDTO);
    }
}