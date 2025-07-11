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
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired
    private AreaManagementService areaManagementService;

    @Transactional
    public UserVO createUser(CreateUserRequest request) {
        log.info("Creating user with account number: {}", request.getAccountNumber());

        if (userMapper.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw new DuplicateResourceException("Account Number already exists.");
        }
        if (userMapper.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists.");
        }

        if ("Traffic Manager".equals(request.getRole())) {
            if (request.getManagedAreas() == null || request.getManagedAreas().isEmpty()) {
                throw new IllegalStateException("Traffic Manager must have at least one managed area.");
            }

            String validationError = areaManagementService.validateAreaAssignmentRequest(
                    null, request.getManagedAreas()
            );
            if (validationError != null) {
                throw new IllegalStateException(validationError);
            }
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

        log.info("About to save user to database...");
        userMapper.save(user);
        log.info("User saved successfully with ID: {}", user.getId());

        if ("Traffic Manager".equals(request.getRole())) {
            log.info("User is Traffic Manager, checking managed areas...");
            log.info("Managed areas from request: {}", request.getManagedAreas());

            if (request.getManagedAreas() != null && !request.getManagedAreas().isEmpty()) {
                log.info("Assigning areas {} to user {}", request.getManagedAreas(), user.getId());
                try {
                    List<String> assignedAreas = areaManagementService.assignAreasToUser(
                            user.getId(),
                            request.getManagedAreas(),
                            user.getId()
                    );
                    log.info("Successfully assigned areas {} to user {}", assignedAreas, user.getId());
                } catch (Exception e) {
                    log.error("Failed to assign areas to user {}: {}", user.getId(), e.getMessage());

                    userMapper.softDeleteById(user.getId());
                    throw new IllegalStateException("Failed to assign areas: " + e.getMessage());
                }
            }
        }

        return mapToUserVO(user);
    }

    @Transactional
    public UserVO updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);


        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));


        Optional<User> userWithSameEmail = userMapper.findByEmail(request.getEmail());
        if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
            throw new DuplicateResourceException("Email is already in use by another account.");
        }

        boolean wasTrafficManager = "Traffic Manager".equals(user.getRole());
        boolean isTrafficManager = "Traffic Manager".equals(request.getRole());

        user.setUserName(request.getUserName());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());

        userMapper.update(user);
        if (wasTrafficManager && !isTrafficManager) {
            areaManagementService.removeUserAreaAssignments(id);
            log.info("Removed all area assignments for user {} (role changed from Traffic Manager)", id);
        }

        if (isTrafficManager && request.getManagedAreas() != null) {

            try {

                areaManagementService.removeUserAreaAssignments(id);

                if (!request.getManagedAreas().isEmpty()) {
                    areaManagementService.assignAreasToUser(id, request.getManagedAreas(), id);
                }
            } catch (Exception e) {
                log.error("Failed to update area assignments for user {}: {}", id, e.getMessage());
                throw new IllegalStateException("Failed to update area assignments: " + e.getMessage());
            }
        }

        return mapToUserVO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if ("Traffic Manager".equals(user.getRole())) {
            areaManagementService.removeUserAreaAssignments(id);
            log.info("Removed area assignments for deleted user {}", id);
        }

        userMapper.softDeleteById(id);
    }

    public UserVO getUserById(Long id) {
        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToUserVO(user);
    }

    public List<UserVO> getAllUsers() {
        List<User> users = userMapper.findAllActive();
        return users.stream().map(this::mapToUserVO).collect(Collectors.toList());
    }

    private UserVO mapToUserVO(User user) {
        UserVO.UserVOBuilder builder = UserVO.builder()
                .id(user.getId())
                .accountNumber(user.getAccountNumber())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled());

        if ("Traffic Manager".equals(user.getRole())) {
            try {
                List<String> managedAreas = areaManagementService.getUserManagedAreas(user.getId());
                builder.managedAreas(managedAreas);
            } catch (Exception e) {
                log.error("Failed to get managed areas for user {}: {}", user.getId(), e.getMessage());
                builder.managedAreas(List.of());
            }
        }

        return builder.build();
    }
}