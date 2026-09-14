package com.mall.user.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.dto.AdminUserCreateRequest;
import com.mall.user.dto.AdminUserResponse;
import com.mall.user.dto.AdminUserUpdateRequest;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.UserEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminUserResponse detail(Long userId) {
        return AdminUserResponse.from(required(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse create(AdminUserCreateRequest request) {
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username, null);
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(request.getStatus());
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USERNAME_EXISTS);
        }
        return detail(user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse update(AdminUserUpdateRequest request) {
        required(request.getUserId());
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username, request.getUserId());
        try {
            userMapper.updateAdminUser(request.getUserId(), username, request.getStatus());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USERNAME_EXISTS);
        }
        return detail(request.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse changeStatus(Long userId, Integer status) {
        required(userId);
        userMapper.updateStatus(userId, status);
        return detail(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId) {
        required(userId);
        userMapper.updatePassword(userId, passwordEncoder.encode(DEFAULT_PASSWORD));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        required(userId);
        if (userMapper.countOrders(userId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USER_HAS_ORDERS);
        }
        userMapper.deleteById(userId);
    }

    private void ensureUsernameAvailable(String username, Long currentId) {
        UserEntity existing = userMapper.findByUsername(username);
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.USERNAME_EXISTS);
        }
    }

    private UserEntity required(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
