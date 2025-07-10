package com.ucd.urbanflow.service;

import com.ucd.urbanflow.domain.dto.CreateUserRequest;
import com.ucd.urbanflow.domain.dto.UpdateUserRequest;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.exception.DuplicateResourceException;
import com.ucd.urbanflow.exception.ResourceNotFoundException;
import com.ucd.urbanflow.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserVO createUser(CreateUserRequest request) {
        log.info("Attempting to create user. Checking if userMapper is null: {}", userMapper == null);
        if (userMapper.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw new DuplicateResourceException("Account Number already exists.");
        }
        if (userMapper.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists.");
        }
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
        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // Validation for email uniqueness
        Optional<User> userWithSameEmail = userMapper.findByEmail(request.getEmail());
        if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
            throw new DuplicateResourceException("Email is already in use by another account.");
        }
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
        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserVO(user);
    }

    public List<UserVO> getAllUsers() {
        return userMapper.findAllActive().stream().map(this::mapToUserVO).collect(Collectors.toList());
    }

    private UserVO mapToUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .accountNumber(user.getAccountNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}