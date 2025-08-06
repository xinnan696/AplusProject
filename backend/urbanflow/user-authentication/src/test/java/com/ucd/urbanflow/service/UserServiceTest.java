package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.domain.dto.CreateUserRequest;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.exception.DuplicateResourceException;
import com.ucd.urbanflow.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Mock
    private LogServiceClient logServiceClient;

    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setAccountNumber("newuser");
        createUserRequest.setUserName("New User");
        createUserRequest.setEmail("new@example.com");
        createUserRequest.setPassword("password123");
        createUserRequest.setRole("ROLE_USER");
    }

    @Test
    @DisplayName("Should create user successfully when account number and email are unique")
    void createUser_shouldSucceed_whenDataIsUnique() {
        // --- Arrange ---
        when(userMapper.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(userMapper.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        // --- Act ---
        UserVO createdUser = userService.createUser(createUserRequest);

        // --- Assert ---
        assertNotNull(createdUser);
        assertEquals("newuser", createdUser.getAccountNumber());
        // Verify that the save method on the mapper was called exactly once.
        verify(userMapper, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when account number already exists")
    void createUser_shouldFail_whenAccountNumberExists() {
        // --- Arrange ---
        // Pretend a user with this account number already exists.
        when(userMapper.findByAccountNumber("newuser")).thenReturn(Optional.of(new User()));

        // --- Act & Assert ---
        // Check that our custom exception is thrown.
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals("Account Number already exists.", exception.getMessage());
        // Verify that the save method was never called.
        verify(userMapper, never()).save(any(User.class));
    }
}