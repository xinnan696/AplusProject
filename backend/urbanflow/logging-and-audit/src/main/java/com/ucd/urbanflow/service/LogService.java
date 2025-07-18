package com.ucd.urbanflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.domain.vo.UserLogVO;
import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.SpecialEventLogDTO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import com.ucd.urbanflow.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final LogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Async("logTaskExecutor")
    public void recordAuthLog(AuthLogDTO dto) {
        log.info("Async writing auth log for account: {}", dto.getAccountNumber());
        logMapper.insertAuthLog(dto);
    }

    @Async("logTaskExecutor")
    public void recordSignalControlLog(SignalControlLogDTO dto) {
        log.info("Async writing signal control log for junction: {}", dto.getJunctionId());
        logMapper.insertSignalControlLog(dto);
    }

    @Async("logTaskExecutor")
    public void recordUserPermissionLog(UserPermissionLogDTO dto) {
        log.info("Async writing user permission log for target account: {}", dto.getTargetAccount());
        logMapper.insertUserPermissionLog(dto);
    }

    @Async("logTaskExecutor")
    public void recordSpecialEventLog(SpecialEventLogDTO dto) {
        log.info("Async writing special event log for event ID: {}", dto.getEventId());
        try {
            String laneIdsJson = objectMapper.writeValueAsString(dto.getLaneIds());
            logMapper.insertSpecialEventLog(dto, laneIdsJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize laneIds for special event log: {}. Error: {}", dto.getEventId(), e.getMessage());
        }
    }

    public List<UserLogVO> getAggregatedUserLogs(String startDate, String endDate) {
        log.info("Querying user logs between {} and {}", startDate, endDate);
        List<UserLogVO> logs = logMapper.queryUserLogs(startDate, endDate);
        return logs != null ? logs : Collections.emptyList();
    }
}