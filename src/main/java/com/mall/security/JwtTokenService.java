package com.mall.security;

import com.mall.config.JwtProperties;
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
public class JwtTokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = buildSigningKey(properties.getSecret());
        requirePositive(properties.getAccessTokenTtl(), "access-token-ttl");
        requirePositive(properties.getRefreshTokenTtl(), "refresh-token-ttl");
    }

    public String issueAccessToken(Long userId, String username, String sessionId, String tokenId) {
        return issueToken(userId, username, sessionId, tokenId, ACCESS_TOKEN_TYPE, properties.getAccessTokenTtl());
    }

    public String issueRefreshToken(Long userId, String username, String sessionId, String tokenId) {
        return issueToken(userId, username, sessionId, tokenId, REFRESH_TOKEN_TYPE, properties.getRefreshTokenTtl());
    }

    public TokenClaims parseAccessToken(String token) {
        return parseToken(token, ACCESS_TOKEN_TYPE);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parseToken(token, REFRESH_TOKEN_TYPE);
    }

    public long getAccessExpiresInSeconds() {
        return properties.getAccessTokenTtl().getSeconds();
    }

    public long getRefreshExpiresInSeconds() {
        return properties.getRefreshTokenTtl().getSeconds();
    }

    public Duration getRefreshTokenTtl() {
        return properties.getRefreshTokenTtl();
    }

    private String issueToken(Long userId, String username, String sessionId, String tokenId,
                              String tokenType, Duration ttl) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .id(tokenId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    private TokenClaims parseToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(tokenType)) {
                throw new TokenValidationException("令牌类型错误");
            }
            String subject = requiredClaim(claims.getSubject(), "sub");
            String username = requiredClaim(claims.get(CLAIM_USERNAME, String.class), CLAIM_USERNAME);
            String sessionId = requiredClaim(claims.get(CLAIM_SESSION_ID, String.class), CLAIM_SESSION_ID);
            String tokenId = requiredClaim(claims.getId(), "jti");
            return new TokenClaims(Long.valueOf(subject), username, sessionId, tokenId, tokenType);
        } catch (TokenValidationException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new TokenValidationException("令牌无效或已过期", exception);
        }
    }

    private static String requiredClaim(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new TokenValidationException("令牌缺少声明: " + name);
        }
        return value;
    }

    private static SecretKey buildSigningKey(String encodedSecret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("JWT_SECRET 解码后必须至少为 32 字节");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("JWT_SECRET 必须是有效 Base64，且解码后至少为 32 字节", exception);
        }
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(propertyName + " 必须大于 0");
        }
    }
}
