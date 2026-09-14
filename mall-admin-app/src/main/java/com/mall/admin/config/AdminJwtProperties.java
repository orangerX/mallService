package com.mall.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.admin.jwt")
public class AdminJwtProperties {
    @NotBlank private String secret;
    @NotBlank private String audience = "mall-admin";
    @NotNull private Duration accessTokenTtl = Duration.ofMinutes(30);
    @NotNull private Duration refreshTokenTtl = Duration.ofDays(7);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration value) { this.accessTokenTtl = value; }
    public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(Duration value) { this.refreshTokenTtl = value; }
}
