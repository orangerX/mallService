package com.mall.user.mapper;

import com.mall.user.model.UserEntity;
import com.mall.user.dto.AdminUserResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    UserEntity findByUsername(@Param("username") String username);

    UserEntity findById(@Param("id") Long id);

    int insert(UserEntity user);

    int updateAdminUser(@Param("id") Long id, @Param("username") String username,
                        @Param("status") Integer status);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int deleteById(@Param("id") Long id);

    long countOrders(@Param("id") Long id);

    long countAdminPage(@Param("keyword") String keyword, @Param("status") Integer status);

    List<AdminUserResponse> findAdminPage(@Param("keyword") String keyword, @Param("status") Integer status,
                                          @Param("offset") int offset, @Param("size") int size);
}
