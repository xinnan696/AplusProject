package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.dto.ManualControlRequest;
import com.ucd.urbanflow.dto.ManualControlResponse;
import com.ucd.urbanflow.service.SignalControlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/signalcontrol")
@RequiredArgsConstructor

public class SignalControlController{
    private static final Logger logger = LoggerFactory.getLogger(SignalControlController.class);
    private final SignalControlService signalControlService;

    @PostMapping("/manual")
    public ResponseEntity<ManualControlResponse<?>> manualControl(@RequestBody ManualControlRequest request) {
        logger.info("API called: POST /api/signalcontrol/manual with body: {}", request);
        return signalControlService.handleManualControl(request);
    }

}
