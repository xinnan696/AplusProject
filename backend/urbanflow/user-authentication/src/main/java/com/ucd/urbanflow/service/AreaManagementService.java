package com.ucd.urbanflow.service;

import com.ucd.urbanflow.domain.pojo.UserAreaMapping;
import com.ucd.urbanflow.mapper.UserAreaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaManagementService {
    
    private final UserAreaMapper userAreaMapper;
    

    @Transactional
    public List<String> assignAreasToUser(Long userId, List<String> areaNames, Long createdBy) {
        log.info("Assigning areas {} to user {}", areaNames, userId);
        

        for (String areaName : areaNames) {
            if (!"Left".equals(areaName) && !"Right".equals(areaName)) {
                throw new IllegalArgumentException("Invalid area name: " + areaName + ". Only 'Left' and 'Right' are supported.");
            }
        }

        for (String areaName : areaNames) {
            if (isAreaOccupied(areaName)) {
                Optional<UserAreaMapping> existingMapping = userAreaMapper.findByAreaName(areaName);
                if (existingMapping.isPresent()) {
                    throw new IllegalStateException(
                        String.format("Area '%s' is already managed by user ID: %d", 
                                    areaName, existingMapping.get().getUserId())
                    );
                }
            }
        }

        userAreaMapper.deleteByUserId(userId);

        List<UserAreaMapping> mappings = areaNames.stream()
            .map(areaName -> {
                UserAreaMapping mapping = new UserAreaMapping();
                mapping.setUserId(userId);
                mapping.setAreaName(areaName);
                mapping.setEnabled(true);
                mapping.setCreatedBy(createdBy);
                mapping.setCreatedAt(LocalDateTime.now());
                mapping.setUpdatedAt(LocalDateTime.now());
                return mapping;
            })
            .toList();
        
        try {
            userAreaMapper.saveBatch(mappings);
            log.info("Successfully assigned {} areas to user {}", areaNames.size(), userId);
            return areaNames;
        } catch (DuplicateKeyException e) {
            log.error("Duplicate key error when assigning areas to user {}: {}", userId, e.getMessage());
            throw new IllegalStateException("One or more areas are already assigned to another user");
        }
    }
    

    public boolean isAreaOccupied(String areaName) {
        return userAreaMapper.isAreaOccupied(areaName);
    }

    public List<String> getUserManagedAreas(Long userId) {
        return userAreaMapper.findByUserId(userId)
                .stream()
                .map(UserAreaMapping::getAreaName)
                .toList();
    }
    

    public List<Map<String, Object>> getOccupiedAreasWithManagerInfo() {
        return userAreaMapper.findOccupiedAreasWithUserInfo();
    }
    

    public List<String> getAvailableAreas() {
        return userAreaMapper.getAvailableAreas();
    }
    

    @Transactional
    public void removeUserAreaAssignments(Long userId) {
        log.info("Removing all area assignments for user {}", userId);
        userAreaMapper.deleteByUserId(userId);
    }
    

    public String validateAreaAssignmentRequest(Long userId, List<String> areaNames) {
        if (areaNames == null || areaNames.isEmpty()) {
            return "No areas specified for assignment";
        }

        for (String areaName : areaNames) {
            if (!"Left".equals(areaName) && !"Right".equals(areaName)) {
                return "Invalid area name: " + areaName + ". Only 'Left' and 'Right' are supported.";
            }
        }

        for (String areaName : areaNames) {
            if (isAreaOccupied(areaName)) {
                Optional<UserAreaMapping> existingMapping = userAreaMapper.findByAreaName(areaName);
                if (existingMapping.isPresent() && !existingMapping.get().getUserId().equals(userId)) {
                    return String.format("Area '%s' is already managed by another user", areaName);
                }
            }
        }
        
        return null;
    }
}