package com.mall.admin.mapper;

import com.mall.admin.model.AdminEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {
    long count();
    AdminEntity findByUsername(@Param("username") String username);
    AdminEntity findById(@Param("id") Long id);
    int insert(AdminEntity admin);
}
