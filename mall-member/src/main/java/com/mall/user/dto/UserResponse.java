package com.mall.user.dto;

import com.mall.user.model.UserEntity;

import java.time.LocalDateTime;

public class UserResponse {

    private final Long id;
    private final String username;
    private final Integer status;
    private final LocalDateTime createdAt;

    public UserResponse(Long id, String username, Integer status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getStatus(), user.getCreatedAt());
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
