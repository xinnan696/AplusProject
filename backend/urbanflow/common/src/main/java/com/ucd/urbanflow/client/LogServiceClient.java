package com.ucd.urbanflow.client;

import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.SpecialEventLogDTO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for interacting with the Logging and Audit service.
 * All other modules should use this client to send log data.
 */
@FeignClient(name = "logging-and-audit", url = "${urbanflow.services.logging-and-audit.url}")
public interface LogServiceClient {

    @PostMapping("/api/logs/auth")
    void logAuth(@RequestBody AuthLogDTO dto);

    @PostMapping("/api/logs/signal-control")
    void logSignalControl(@RequestBody SignalControlLogDTO dto);

    @PostMapping("/api/logs/user-permission")
    void logUserPermission(@RequestBody UserPermissionLogDTO dto);

    @PostMapping("/api/logs/special-event")
    void logSpecialEvent(@RequestBody SpecialEventLogDTO dto);
}