package com.mall.user.mapper;

import com.mall.user.model.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findById(@Param("id") Long id);

    int insert(UserEntity user);
}
