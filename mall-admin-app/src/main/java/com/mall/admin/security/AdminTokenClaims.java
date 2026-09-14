package com.mall.admin.security;

public class AdminTokenClaims {
    private final Long adminId;
    private final String username;
    private final String sessionId;
    private final String tokenId;
    public AdminTokenClaims(Long adminId, String username, String sessionId, String tokenId) {
        this.adminId = adminId; this.username = username; this.sessionId = sessionId; this.tokenId = tokenId;
    }
    public Long getAdminId() { return adminId; }
    public String getUsername() { return username; }
    public String getSessionId() { return sessionId; }
    public String getTokenId() { return tokenId; }
}
