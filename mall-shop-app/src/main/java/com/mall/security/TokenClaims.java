package com.mall.security;

public class TokenClaims {

    private final Long userId;
    private final String username;
    private final String sessionId;
    private final String tokenId;
    private final String tokenType;

    public TokenClaims(Long userId, String username, String sessionId, String tokenId, String tokenType) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.tokenId = tokenId;
        this.tokenType = tokenType;
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

    public String getTokenId() {
        return tokenId;
    }

    public String getTokenType() {
        return tokenType;
    }
}
