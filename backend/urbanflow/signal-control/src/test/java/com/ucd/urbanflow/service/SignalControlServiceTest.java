package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.config.AuthenticatedUser;
import com.ucd.urbanflow.dto.ManualControlRequest;
import com.ucd.urbanflow.dto.ManualControlResponse;
import com.ucd.urbanflow.dto.TraCIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.swing.text.html.parser.Entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SignalControlServiceTest {

    @Mock
    private TraCIClient traCIClient;

    @Mock
    private LogServiceClient logServiceClient;

    @Mock
    private JunctionNameCache junctionNameCache;

    @InjectMocks
    private SignalControlService signalControlService;

    private AuthenticatedUser mockUser;

    @BeforeEach
    void setUp() {
//        MockitoAnnotations.openMocks(this);

        // set SecurityContext user
        mockUser = new AuthenticatedUser();
        mockUser.setId(1L);
        mockUser.setAccountNumber("U001");
        mockUser.setUserName("TestUser");
        mockUser.setRole("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities())
        );

        when(junctionNameCache.getName(anyString())).thenReturn("TestJunction");
    }

    @Test
    void testHandleManualControl_SUMONotConnected(){
        ManualControlRequest req = new ManualControlRequest("J1", 0, 10, null, "manual");

        when(traCIClient.checkSUMOStatus()).thenReturn(false);

        ResponseEntity<ManualControlResponse<?>> res = signalControlService.handleManualControl(req);

        assertEquals(503, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        assertEquals(503, res.getBody().getCode());
    }

    @Test
    void testHandleManualControl_JunctionNotExists(){
        ManualControlRequest req = new ManualControlRequest("J1", 0, 10, null, "manual");

        when(traCIClient.checkSUMOStatus()).thenReturn(true);
        when(traCIClient.checkJunctionExists("J1")).thenReturn(false);

        ResponseEntity<ManualControlResponse<?>> res = signalControlService.handleManualControl(req);

        assertEquals(400, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        assertEquals(400, res.getBody().getCode());
    }

    @Test
    void testHandleManualControl_DurationOnly_Success(){
        ManualControlRequest req = new ManualControlRequest("J1", 0, 10, null, "manual");

        when(traCIClient.checkSUMOStatus()).thenReturn(true);
        when(traCIClient.checkJunctionExists("J1")).thenReturn(true);
        when(traCIClient.setSignalDuration("J1", 10)).thenReturn(true);

        ResponseEntity<ManualControlResponse<?>> res = signalControlService.handleManualControl(req);

        assertEquals(200, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        assertEquals(200, res.getBody().getCode());
        verify(traCIClient, times(1)).setSignalDuration("J1", 10);
    }

    @Test
    void testHandleManualControl_FullControl_Fail() {
        ManualControlRequest req = new ManualControlRequest("J1", 1, 30, "GGrr", "manual");

        when(traCIClient.checkSUMOStatus()).thenReturn(true);
        when(traCIClient.checkJunctionExists("J1")).thenReturn(true);
        when(traCIClient.setSignalStateAndDuration("J1", 1, "GGrr", 30)).thenReturn(false);

        ResponseEntity<ManualControlResponse<?>> res = signalControlService.handleManualControl(req);

        assertEquals(500, res.getStatusCodeValue());
        assertNotNull(res.getBody());
        assertEquals(500, res.getBody().getCode());
    }

}
