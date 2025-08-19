package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.UserAreaMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for user-area mapping
 */
@Mapper
public interface UserAreaMapper {
    
    /**
     * Batch save user-area mappings
     */
    void saveBatch(@Param("mappings") List<UserAreaMapping> mappings);
    
    /**
     * Find all mappings by user ID
     */
    List<UserAreaMapping> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find mappings by area name (used to check uniqueness)
     */
    Optional<UserAreaMapping> findByAreaName(@Param("areaName") String areaName);
    
    /**
     * Find all assigned areas and their manager information
     */
    List<java.util.Map<String, Object>> findOccupiedAreasWithUserInfo();
    
    /**
     * Delete all area mappings of the user
     */
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Check if the area is already occupied
     */
    boolean isAreaOccupied(@Param("areaName") String areaName);
    
    /**
     * Get the list of available areas (Left and Right that are not occupied)
     */
    List<String> getAvailableAreas();
}