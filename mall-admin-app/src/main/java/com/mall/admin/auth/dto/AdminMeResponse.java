package com.mall.admin.auth.dto;

public class AdminMeResponse {
    private final Long id;
    private final String username;
    private final Integer status;
    public AdminMeResponse(Long id, String username, Integer status) {
        this.id = id; this.username = username; this.status = status;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Integer getStatus() { return status; }
}
