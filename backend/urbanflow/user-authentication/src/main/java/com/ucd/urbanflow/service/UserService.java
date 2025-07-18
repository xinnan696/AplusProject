package com.ucd.urbanflow.service;

import com.ucd.urbanflow.client.LogServiceClient;
import com.ucd.urbanflow.config.AuthenticatedUser;
import com.ucd.urbanflow.domain.dto.CreateUserRequest;
import com.ucd.urbanflow.domain.dto.UpdateUserRequest;
import com.ucd.urbanflow.domain.pojo.User;
import com.ucd.urbanflow.domain.vo.UserVO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import com.ucd.urbanflow.exception.DuplicateResourceException;
import com.ucd.urbanflow.exception.ResourceNotFoundException;
import com.ucd.urbanflow.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
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
    @Autowired
    private LogServiceClient logServiceClient;


    @Transactional
    public UserVO createUser(CreateUserRequest request) {
        AuthenticatedUser operator = getAuthenticatedUser();
        log.info("Operator '{}' is creating user with account number: {}", operator.getAccountNumber(), request.getAccountNumber());

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
        StringJoiner createdFields = new StringJoiner(", ");
        createdFields.add("role=" + user.getRole());
        createdFields.add("enabled=" + user.isEnabled());
        recordUserPermissionLog(operator, request.getAccountNumber(), "CREATE", createdFields.toString(), "User created successfully.");
        return mapToUserVO(user);
    }

    @Transactional
    public UserVO updateUser(Long id, UpdateUserRequest request) {
        AuthenticatedUser operator = getAuthenticatedUser();
        log.info("Operator '{}' is updating user with account number: {}", operator.getAccountNumber(), id);

        log.info("Updating user with ID: {}", id);

        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String oldUserName = user.getUserName();
        String oldEmail = user.getEmail();
        String oldRole = user.getRole();
        boolean oldEnabled = user.isEnabled();

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

        StringJoiner changes = new StringJoiner("; ");
        if (!Objects.equals(oldUserName, request.getUserName())) {
            changes.add(String.format("userName: '%s' -> '%s'", oldUserName, request.getUserName()));
        }
        if (!Objects.equals(oldEmail, request.getEmail())) {
            changes.add(String.format("email: '%s' -> '%s'", oldEmail, request.getEmail()));
        }
        if (!Objects.equals(oldRole, request.getRole())) {
            changes.add(String.format("role: '%s' -> '%s'", oldRole, request.getRole()));
        }
        if (oldEnabled != request.isEnabled()) {
            changes.add(String.format("enabled: '%s' -> '%s'", oldEnabled, request.isEnabled()));
        }
        String operatedFields = changes.toString();
        if (operatedFields.isEmpty()) {
            operatedFields = "No basic user fields were changed. Area assignments may have been updated.";
        }
        recordUserPermissionLog(operator, user.getAccountNumber(), "UPDATE", operatedFields, "User updated successfully.");
        return mapToUserVO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        AuthenticatedUser operator = getAuthenticatedUser();
        log.info("Operator '{}' is deleting user with ID: {}", operator.getAccountNumber(), id);
        log.info("Deleting user with ID: {}", id);

        User user = userMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String targetAccountNumber = user.getAccountNumber();

        if ("Traffic Manager".equals(user.getRole())) {
            areaManagementService.removeUserAreaAssignments(id);
            log.info("Removed area assignments for deleted user {}", id);
        }

        userMapper.softDeleteById(id);
        recordUserPermissionLog(operator, targetAccountNumber, "DELETE", "User account marked as deleted.", "User deleted successfully.");

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

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser) {
            return (AuthenticatedUser) authentication.getPrincipal();
        }
        log.warn("AuthenticatedUser not found in SecurityContext. Using default system user for logging.");
        AuthenticatedUser systemUser = new AuthenticatedUser();
        systemUser.setAccountNumber("system");
        systemUser.setUserName("System");
        return systemUser;
    }

    private void recordUserPermissionLog(AuthenticatedUser operator, String targetAccountNumber, String operationType, String operatedFields, String message) {
        UserPermissionLogDTO logDTO = new UserPermissionLogDTO();
        logDTO.setAccountNumber(operator.getAccountNumber());
//        logDTO.setUserName(operator.getUserName());
        logDTO.setTargetAccount(targetAccountNumber);
        logDTO.setOperationType(operationType);
        logDTO.setOperatedFields(operatedFields);
        logDTO.setOperationResult("SUCCESS");
        logDTO.setResultMessage(message);

        try {
            logServiceClient.logUserPermission(logDTO);
        } catch (Exception e) {
            log.error("Failed to send user permission log. Log data: {}", logDTO, e);
        }
    }

}