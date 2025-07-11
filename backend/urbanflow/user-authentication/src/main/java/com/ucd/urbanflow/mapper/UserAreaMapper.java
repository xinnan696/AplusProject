package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.UserAreaMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

/**
 * 用户区域映射数据访问层
 */
@Mapper
public interface UserAreaMapper {
    
    /**
     * 批量保存用户区域映射
     */
    void saveBatch(@Param("mappings") List<UserAreaMapping> mappings);
    
    /**
     * 根据用户ID查找所有映射
     */
    List<UserAreaMapping> findByUserId(@Param("userId") Long userId);
    
    /**
     * 根据区域名称查找映射（用于检查唯一性）
     */
    Optional<UserAreaMapping> findByAreaName(@Param("areaName") String areaName);
    
    /**
     * 查找所有已占用的区域及其管理者信息
     */
    List<java.util.Map<String, Object>> findOccupiedAreasWithUserInfo();
    
    /**
     * 删除用户的所有区域映射
     */
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 检查区域是否已被占用
     */
    boolean isAreaOccupied(@Param("areaName") String areaName);
    
    /**
     * 获取可用的区域列表（Left, Right中未被占用的）
     */
    List<String> getAvailableAreas();
}