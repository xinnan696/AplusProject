package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.dto.CreateUserRequest;
import com.ucd.urbanflow.domain.dto.UpdateUserRequest;
import com.ucd.urbanflow.domain.vo.ApiResponse;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ApiResponse<UserVO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @GetMapping("/list")
    public ApiResponse<List<UserVO>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<UserVO> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping("/update/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("User deleted successfully!");
    }
}