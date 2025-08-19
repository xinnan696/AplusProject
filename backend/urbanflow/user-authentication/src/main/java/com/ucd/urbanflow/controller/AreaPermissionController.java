package com.ucd.urbanflow.controller;

import com.ucd.urbanflow.domain.pojo.User;
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

@RestController
@RequestMapping("/api/area-permission")
@RequiredArgsConstructor
@Slf4j
public class AreaPermissionController {

    private final AreaManagementService areaManagementService;
    private final JwtService jwtService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserAreaPermissions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            log.info("Getting area permissions for user: {}", email);
            log.debug("Authentication type: {}", authentication.getClass().getSimpleName());
            log.debug("Principal type: {}", authentication.getPrincipal().getClass().getSimpleName());
            log.debug("Credentials type: {}", authentication.getCredentials() != null ? authentication.getCredentials().getClass().getSimpleName() : "null");

            Long userId = null;

            try {
                userId = jwtService.getUserIdFromAuthentication(authentication);
                log.debug("Extracted userId from JWT: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to extract userId from JWT: {}", e.getMessage());
            }

            if (userId == null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                userId = user.getId();
                log.debug("Extracted userId from Principal: {}", userId);
            }

            if (userId == null) {
                log.error("Could not extract userId for user: {}", email);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Could not identify user"));
            }


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

            log.info("Toggling view mode for user: {}, viewAll: {}", email, viewAll);

            Long userId = null;

            try {
                userId = jwtService.getUserIdFromAuthentication(authentication);
                log.debug("Extracted userId from JWT: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to extract userId from JWT: {}", e.getMessage());
            }

            // 方法2：如果失败，尝试从Principal获取
            if (userId == null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                userId = user.getId();
                log.debug("Extracted userId from Principal: {}", userId);
            }

            if (userId == null) {
                log.error("Could not extract userId for user: {}", email);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Could not identify user"));
            }

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
            String email = authentication.getName();

            log.debug("Validating area access for user: {}, area: {}", email, area);

            Long userId = null;

            try {
                userId = jwtService.getUserIdFromAuthentication(authentication);
                log.debug("Extracted userId from JWT: {}", userId);
            } catch (Exception e) {
                log.warn("Failed to extract userId from JWT: {}", e.getMessage());
            }

            if (userId == null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                userId = user.getId();
                log.debug("Extracted userId from Principal: {}", userId);
            }


            if (userId == null) {
                log.error("Could not extract userId for user: {}", email);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Could not identify user"));
            }

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