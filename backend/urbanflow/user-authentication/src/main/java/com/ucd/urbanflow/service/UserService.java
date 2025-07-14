package com.ucd.urbanflow.service;

import com.ucd.urbanflow.domain.dto.CreateUserRequest;
import com.ucd.urbanflow.domain.dto.UpdateUserRequest;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserVO createUser(CreateUserRequest request) {
        userMapper.findByAccountNumber(request.getAccountNumber()).ifPresent(u -> {
            throw new IllegalStateException("Account Number already exists.");
        });
        userMapper.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalStateException("Email already exists.");
        });
        User user = new User();
        user.setAccountNumber(request.getAccountNumber());
        user.setUserName(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(request.getDepartment());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());
        user.setLocked(false);
        user.setDeleted(false);
        userMapper.save(user);
        return mapToUserVO(user);
    }

    public UserVO updateUser(Long id, UpdateUserRequest request) {
        User user = userMapper.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setUserName(request.getUserName());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());
        userMapper.update(user);
        return mapToUserVO(user);
    }

    public void deleteUser(Long id) {
        userMapper.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        userMapper.softDeleteById(id);
    }

    public UserVO getUserById(Long id) {
        User user = userMapper.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToUserVO(user);
    }

    public List<UserVO> getAllUsers() {
        return userMapper.findAllActive().stream().map(this::mapToUserVO).collect(Collectors.toList());
    }

    private UserVO mapToUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}