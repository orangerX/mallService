package com.mall.admin.auth.dto;

public class AdminTokenResponse {
    private final String tokenType = "Bearer";
    private final String accessToken;
    private final String refreshToken;
    private final long accessExpiresIn;
    private final long refreshExpiresIn;

    public AdminTokenResponse(String accessToken, String refreshToken, long accessExpiresIn, long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresIn = accessExpiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
    }
    public String getTokenType() { return tokenType; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public long getAccessExpiresIn() { return accessExpiresIn; }
    public long getRefreshExpiresIn() { return refreshExpiresIn; }
}
