package com.ucd.urbanflow.mapper;


import com.ucd.urbanflow.domain.vo.UserLogVO;
import com.ucd.urbanflow.dto.AuthLogDTO;
import com.ucd.urbanflow.dto.SignalControlLogDTO;
import com.ucd.urbanflow.dto.SpecialEventLogDTO;
import com.ucd.urbanflow.dto.UserPermissionLogDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {

    void insertAuthLog(AuthLogDTO log);

    void insertSignalControlLog(SignalControlLogDTO log);

    void insertUserPermissionLog(UserPermissionLogDTO log);

    void insertSpecialEventLog(@Param("log") SpecialEventLogDTO log, @Param("laneIdsJson") String laneIdsJson);

    List<UserLogVO> queryUserLogs(@Param("startDate") String startDate, @Param("endDate") String endDate);
}