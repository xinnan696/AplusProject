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

/**
 * 区域管理服务类
 * 处理用户区域映射的业务逻辑，确保区域管理的唯一性
 * 目前支持两个区域：Left 和 Right
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AreaManagementService {
    
    private final UserAreaMapper userAreaMapper;
    
    /**
     * 为用户分配区域管理权限
     * 确保一个区域只能被一个manager管理
     * 
     * @param userId 用户ID
     * @param areaNames 区域名称列表（Left 和/或 Right）
     * @param createdBy 创建者ID
     * @return 成功分配的区域列表
     * @throws IllegalStateException 如果区域已被其他用户占用
     */
    @Transactional
    public List<String> assignAreasToUser(Long userId, List<String> areaNames, Long createdBy) {
        log.info("Assigning areas {} to user {}", areaNames, userId);
        
        // 1. 验证区域名称只能是 Left 或 Right
        for (String areaName : areaNames) {
            if (!"Left".equals(areaName) && !"Right".equals(areaName)) {
                throw new IllegalArgumentException("Invalid area name: " + areaName + ". Only 'Left' and 'Right' are supported.");
            }
        }
        
        // 2. 检查所有区域是否可用
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
        
        // 3. 清除用户现有的区域映射（如果需要重新分配）
        userAreaMapper.deleteByUserId(userId);
        
        // 4. 创建新的区域映射
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
    
    /**
     * 检查区域是否已被占用
     * 
     * @param areaName 区域名称（Left 或 Right）
     * @return true如果已被占用，false如果可用
     */
    public boolean isAreaOccupied(String areaName) {
        return userAreaMapper.isAreaOccupied(areaName);
    }
    
    /**
     * 获取用户管理的区域列表
     * 
     * @param userId 用户ID
     * @return 区域名称列表
     */
    public List<String> getUserManagedAreas(Long userId) {
        return userAreaMapper.findByUserId(userId)
                .stream()
                .map(UserAreaMapping::getAreaName)
                .toList();
    }
    
    /**
     * 获取所有已占用的区域及其管理者信息
     * 
     * @return 包含区域信息和管理者信息的Map列表
     */
    public List<Map<String, Object>> getOccupiedAreasWithManagerInfo() {
        return userAreaMapper.findOccupiedAreasWithUserInfo();
    }
    
    /**
     * 获取可用的区域列表
     * 
     * @return 可用区域名称列表（Left, Right中未被占用的）
     */
    public List<String> getAvailableAreas() {
        return userAreaMapper.getAvailableAreas();
    }
    
    /**
     * 移除用户的区域管理权限
     * 
     * @param userId 用户ID
     */
    @Transactional
    public void removeUserAreaAssignments(Long userId) {
        log.info("Removing all area assignments for user {}", userId);
        userAreaMapper.deleteByUserId(userId);
    }
    
    /**
     * 验证区域分配请求
     * 
     * @param userId 用户ID
     * @param areaNames 请求的区域列表
     * @return 验证结果消息（null表示验证通过）
     */
    public String validateAreaAssignmentRequest(Long userId, List<String> areaNames) {
        if (areaNames == null || areaNames.isEmpty()) {
            return "No areas specified for assignment";
        }
        
        // 验证区域名称
        for (String areaName : areaNames) {
            if (!"Left".equals(areaName) && !"Right".equals(areaName)) {
                return "Invalid area name: " + areaName + ". Only 'Left' and 'Right' are supported.";
            }
        }
        
        // 检查区域占用情况
        for (String areaName : areaNames) {
            if (isAreaOccupied(areaName)) {
                Optional<UserAreaMapping> existingMapping = userAreaMapper.findByAreaName(areaName);
                if (existingMapping.isPresent() && !existingMapping.get().getUserId().equals(userId)) {
                    return String.format("Area '%s' is already managed by another user", areaName);
                }
            }
        }
        
        return null; // 验证通过
    }
}