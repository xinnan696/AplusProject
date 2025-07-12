package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.vo.ApiResponse;
import com.ucd.urbanflow.service.AreaManagementService;
import com.ucd.urbanflow.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区域权限控制器
 * 处理基于用户权限的地图区域访问控制
 */
@RestController
@RequestMapping("/api/area-permission")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class AreaPermissionController {
    
    private final AreaManagementService areaManagementService;
    private final JwtService jwtService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserAreaPermissions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            Long userId = jwtService.getUserIdFromAuthentication(authentication);

            List<String> managedAreas = areaManagementService.getUserManagedAreas(userId);
            
            Map<String, Object> permissions = new HashMap<>();
            permissions.put("managedAreas", managedAreas);
            permissions.put("canViewLeft", managedAreas.contains("Left"));
            permissions.put("canViewRight", managedAreas.contains("Right"));
            permissions.put("canViewAll", false); // 默认不能查看全部
            permissions.put("isFullAccess", managedAreas.size() == 2); // 是否有完全访问权限
            
            log.info("Retrieved area permissions for user {}: {}", email, permissions);
            
            return ResponseEntity.ok(ApiResponse.success(permissions));
            
        } catch (Exception e) {
            log.error("Error retrieving area permissions: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "Failed to retrieve area permissions"));
        }
    }

    @PostMapping("/toggle-view")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleViewMode(@RequestParam boolean viewAll) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Long userId = jwtService.getUserIdFromAuthentication(authentication);
            List<String> managedAreas = areaManagementService.getUserManagedAreas(userId);
            
            Map<String, Object> permissions = new HashMap<>();
            permissions.put("managedAreas", managedAreas);
            permissions.put("canViewLeft", managedAreas.contains("Left") || viewAll);
            permissions.put("canViewRight", managedAreas.contains("Right") || viewAll);
            permissions.put("canViewAll", viewAll);
            permissions.put("isFullAccess", managedAreas.size() == 2);
            
            log.info("Toggled view mode for user {}: viewAll={}, permissions={}", 
                    email, viewAll, permissions);
            
            return ResponseEntity.ok(ApiResponse.success(permissions));
            
        } catch (Exception e) {
            log.error("Error toggling view mode: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "Failed to toggle view mode"));
        }
    }

    @GetMapping("/validate/{area}")
    public ResponseEntity<ApiResponse<Boolean>> validateAreaAccess(@PathVariable String area) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = jwtService.getUserIdFromAuthentication(authentication);
            
            List<String> managedAreas = areaManagementService.getUserManagedAreas(userId);
            boolean hasAccess = managedAreas.contains(area);
            
            log.debug("Area access validation - User: {}, Area: {}, HasAccess: {}", 
                     userId, area, hasAccess);
            
            return ResponseEntity.ok(ApiResponse.success(hasAccess));
            
        } catch (Exception e) {
            log.error("Error validating area access: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "Failed to validate area access"));
        }
    }
}
