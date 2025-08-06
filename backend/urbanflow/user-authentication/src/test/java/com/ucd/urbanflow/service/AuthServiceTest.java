package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.domain.dto.LoginRequest;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.LoginResponse;
import com.ucd.urbanflow.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock creates a "fake" version of a dependency. We control its behavior.
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private AreaManagementService areaManagementService;
    @Mock
    private LogServiceClient logServiceClient;

    // @InjectMocks creates an instance of AuthService and injects all the @Mock objects into it.
    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Prepare common test data before each test runs.
        testUser = new User();
        testUser.setId(1L);
        testUser.setAccountNumber("admin01");
        testUser.setUserName("Admin User");
        testUser.setEmail("admin@example.com");
        testUser.setPassword("hashed_password");
        testUser.setRole("ROLE_ADMIN");

        loginRequest = new LoginRequest();
        loginRequest.setAccountNumber("admin01");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Login should return LoginResponse when credentials are valid")
    void login_shouldSucceed_withValidCredentials() {
        // --- Arrange (Setup mock behavior) ---
        // 1. When userMapper.findByAccountNumber is called, pretend we found the user.
        when(userMapper.findByAccountNumber("admin01")).thenReturn(Optional.of(testUser));
        // 2. Pretend the account is not locked.
        when(loginAttemptService.isBlocked("admin@example.com")).thenReturn(false);
        // 3. Pretend the authentication manager successfully authenticates.
        //    (For a successful test, it does nothing and doesn't throw an exception).
        // 4. When jwtService.generateToken is called, return a fake token.
        when(jwtService.generateToken(testUser)).thenReturn("fake.jwt.token");
        when(areaManagementService.getUserManagedAreas(1L)).thenReturn(Collections.emptyList());


        // --- Act (Execute the method we are testing) ---
        LoginResponse result = authService.login(loginRequest, httpServletRequest);

        // --- Assert (Verify the results) ---
        assertNotNull(result);
        assertEquals("fake.jwt.token", result.getToken());
        assertEquals("Admin User", result.getUser().getUserName());

        // Verify that the loginSucceeded method was called on our mock.
        verify(loginAttemptService).loginSucceeded("admin@example.com");
    }

    @Test
    @DisplayName("Login should throw UsernameNotFoundException when user does not exist")
    void login_shouldFail_whenUserNotFound() {
        // --- Arrange ---
        // 1. When userMapper is called, pretend we found nothing (return an empty Optional).
        when(userMapper.findByAccountNumber("admin01")).thenReturn(Optional.empty());

        // --- Act & Assert ---
        // 2. Assert that calling the login method throws the specific exception we expect.
        assertThrows(UsernameNotFoundException.class, () -> {
            authService.login(loginRequest, httpServletRequest);
        });
    }

    @Test
    @DisplayName("Login should throw BadCredentialsException when password is incorrect")
    void login_shouldFail_withIncorrectPassword() {
        // --- Arrange ---
        when(userMapper.findByAccountNumber("admin01")).thenReturn(Optional.of(testUser));
        when(loginAttemptService.isBlocked("admin@example.com")).thenReturn(false);
        // 1. Configure the mock AuthenticationManager to throw an exception when called.
        doThrow(new BadCredentialsException("Bad credentials")).when(authenticationManager).authenticate(any());

        // --- Act & Assert ---
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest, httpServletRequest);
        });

        // 2. Verify that the loginFailed method was called.
        verify(loginAttemptService).loginFailed("admin@example.com");
    }
}