package com.mall.user.service;

import com.mall.common.api.PageResponse;
import com.mall.user.dto.AdminUserResponse;
import com.mall.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserQueryService {
    private final UserMapper userMapper;

    public AdminUserQueryService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PageResponse<AdminUserResponse> page(String keyword, Integer status, int page, int size) {
        String normalized = normalize(keyword);
        return new PageResponse<>(
                userMapper.findAdminPage(normalized, status, (page - 1) * size, size),
                page, size, userMapper.countAdminPage(normalized, status));
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
