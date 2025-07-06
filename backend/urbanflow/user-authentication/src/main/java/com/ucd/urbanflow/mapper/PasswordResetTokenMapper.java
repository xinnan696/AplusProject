package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface PasswordResetTokenMapper {

    void save(PasswordResetToken token);
    Optional<PasswordResetToken> findByToken(@Param("token") String token);
    void markAsUsed(@Param("token") String token);
}