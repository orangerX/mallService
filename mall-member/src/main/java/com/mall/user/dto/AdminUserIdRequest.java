package com.mall.user.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminUserIdRequest {
    @NotNull
    @Min(1)
    private Long userId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
