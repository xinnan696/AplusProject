package com.ucd.urbanflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucd.urbanflow.dto.ManualControlRequest;
import com.ucd.urbanflow.service.SignalControlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignalControlController.class)
public class SignalControlControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SignalControlService signalControlService;

    @Test
    void testManualControlEndpoint() throws Exception{
        ManualControlRequest req = new ManualControlRequest("J1", null, 10, null,"manual");

        when(signalControlService.handleManualControl(any())).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/api/signalcontrol/manual").contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(req))).andExpect(status().isOk());

        verify(signalControlService, times(1)).handleManualControl(any());
    }
}
