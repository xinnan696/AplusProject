package com.ucd.urbanflow.mapper;

import com.ucd.urbanflow.domain.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findByAccountNumber(@Param("accountNumber") String accountNumber);
    Optional<User> findByEmail(@Param("email") String email);
    Optional<User> findById(@Param("id") Long id);
    List<User> findAllActive();
    int save(User user);
    int update(User user);
    void updatePassword(@Param("email") String email, @Param("newPassword") String newPassword);
    void updateUserLockedStatus(@Param("email") String email, @Param("locked") boolean locked);
    int softDeleteById(@Param("id") Long id);
}