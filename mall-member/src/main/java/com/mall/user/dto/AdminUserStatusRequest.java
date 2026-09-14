package com.mall.user.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminUserStatusRequest {
    @NotNull
    @Min(1)
    private Long userId;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
