package com.mall.security;

import com.mall.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    private MutableClock clock;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        clock = new MutableClock(Instant.parse("2026-09-10T12:00:00Z"));
        tokenService = new JwtTokenService(properties, clock);
    }

    @Test
    void issuesAndParsesAccessToken() {
        String token = tokenService.issueAccessToken(7L, "alice", "session-1", "access-1");

        TokenClaims claims = tokenService.parseAccessToken(token);

        assertEquals(7L, claims.getUserId());
        assertEquals("alice", claims.getUsername());
        assertEquals("session-1", claims.getSessionId());
        assertEquals("access-1", claims.getTokenId());
        assertEquals("access", claims.getTokenType());
    }

    @Test
    void rejectsWrongTokenType() {
        String token = tokenService.issueRefreshToken(7L, "alice", "session-1", "refresh-1");

        assertThrows(TokenValidationException.class, () -> tokenService.parseAccessToken(token));
    }

    @Test
    void rejectsExpiredToken() {
        String token = tokenService.issueAccessToken(7L, "alice", "session-1", "access-1");
        clock.advance(Duration.ofMinutes(31));

        assertThrows(TokenValidationException.class, () -> tokenService.parseAccessToken(token));
    }

    @Test
    void rejectsTokenForAdminAudience() {
        JwtProperties adminProperties = new JwtProperties();
        adminProperties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        adminProperties.setAudience("mall-admin");
        JwtTokenService adminTokenService = new JwtTokenService(adminProperties, clock);
        String token = adminTokenService.issueAccessToken(7L, "admin", "session-1", "access-1");

        assertThrows(TokenValidationException.class, () -> tokenService.parseAccessToken(token));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
