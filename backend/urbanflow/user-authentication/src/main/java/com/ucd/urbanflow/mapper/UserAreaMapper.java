package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.UserAreaMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;


@Mapper
public interface UserAreaMapper {
    

    void saveBatch(@Param("mappings") List<UserAreaMapping> mappings);

    List<UserAreaMapping> findByUserId(@Param("userId") Long userId);

    Optional<UserAreaMapping> findByAreaName(@Param("areaName") String areaName);

    List<java.util.Map<String, Object>> findOccupiedAreasWithUserInfo();

    void deleteByUserId(@Param("userId") Long userId);

    boolean isAreaOccupied(@Param("areaName") String areaName);

    List<String> getAvailableAreas();
}