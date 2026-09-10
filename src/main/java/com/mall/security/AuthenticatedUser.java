package com.mall.security;

public class AuthenticatedUser {

    private final Long userId;
    private final String username;
    private final String sessionId;
    private final String accessTokenId;

    public AuthenticatedUser(Long userId, String username, String sessionId, String accessTokenId) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.accessTokenId = accessTokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAccessTokenId() {
        return accessTokenId;
    }
}
