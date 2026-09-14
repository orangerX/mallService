package com.mall.user.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.dto.UserResponse;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getCurrentUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND);
        }
        if (UserEntity.STATUS_ENABLED != user.getStatus()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
        }
        return UserResponse.from(user);
    }
}
