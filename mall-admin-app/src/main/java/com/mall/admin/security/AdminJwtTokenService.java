package com.mall.admin.security;

import com.mall.admin.config.AdminJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class AdminJwtTokenService {
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private final AdminJwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public AdminJwtTokenService(AdminJwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        try {
            byte[] bytes = Decoders.BASE64.decode(properties.getSecret());
            if (bytes.length < 32) throw new IllegalArgumentException();
            this.key = Keys.hmacShaKeyFor(bytes);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("ADMIN_JWT_SECRET 必须是有效 Base64，且解码后至少为 32 字节", exception);
        }
    }

    public String issueAccess(Long id, String username, String sid, String jti) {
        return issue(id, username, sid, jti, ACCESS, properties.getAccessTokenTtl());
    }
    public String issueRefresh(Long id, String username, String sid, String jti) {
        return issue(id, username, sid, jti, REFRESH, properties.getRefreshTokenTtl());
    }
    public AdminTokenClaims parseAccess(String token) { return parse(token, ACCESS); }
    public AdminTokenClaims parseRefresh(String token) { return parse(token, REFRESH); }
    public Duration refreshTtl() { return properties.getRefreshTokenTtl(); }
    public long accessSeconds() { return properties.getAccessTokenTtl().getSeconds(); }
    public long refreshSeconds() { return properties.getRefreshTokenTtl().getSeconds(); }

    private String issue(Long id, String username, String sid, String jti, String type, Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder().subject(String.valueOf(id))
                .claim("username", username).claim("sid", sid).claim("type", type)
                .claim("aud", properties.getAudience()).id(jti)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl))).signWith(key).compact();
    }

    private AdminTokenClaims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant()))
                    .build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get("type", String.class))
                    || claims.getAudience() == null
                    || !claims.getAudience().contains(properties.getAudience())) {
                throw new AdminTokenValidationException("管理员令牌类型或受众错误");
            }
            return new AdminTokenClaims(Long.valueOf(required(claims.getSubject())),
                    required(claims.get("username", String.class)), required(claims.get("sid", String.class)),
                    required(claims.getId()));
        } catch (AdminTokenValidationException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AdminTokenValidationException("管理员令牌无效或已过期", exception);
        }
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new AdminTokenValidationException("管理员令牌声明缺失");
        return value;
    }
}
