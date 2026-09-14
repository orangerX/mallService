package com.mall.admin.security;

public class AdminPrincipal {
    private final Long adminId;
    private final String username;
    private final String sessionId;
    public AdminPrincipal(Long adminId, String username, String sessionId) {
        this.adminId = adminId; this.username = username; this.sessionId = sessionId;
    }
    public Long getAdminId() { return adminId; }
    public String getUsername() { return username; }
    public String getSessionId() { return sessionId; }
}
