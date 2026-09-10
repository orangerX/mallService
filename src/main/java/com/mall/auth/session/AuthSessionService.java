package com.mall.auth.session;

import java.time.Duration;

public interface AuthSessionService {

    void create(String sessionId, Long userId, String refreshTokenId, Duration ttl);

    boolean isActive(String sessionId, Long userId);

    boolean rotateRefreshToken(String sessionId, Long userId, String currentRefreshTokenId,
                               String newRefreshTokenId, Duration ttl);

    void delete(String sessionId);
}
