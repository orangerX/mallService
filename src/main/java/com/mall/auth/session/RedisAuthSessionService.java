package com.mall.auth.session;

import com.mall.config.AuthSessionProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class RedisAuthSessionService implements AuthSessionService {

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]); "
                    + "if not current or current ~= ARGV[1] then return 0 end; "
                    + "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]); return 1;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final AuthSessionProperties properties;

    public RedisAuthSessionService(StringRedisTemplate redisTemplate, AuthSessionProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void create(String sessionId, Long userId, String refreshTokenId, Duration ttl) {
        redisTemplate.opsForValue().set(key(sessionId), value(userId, refreshTokenId), ttl);
    }

    @Override
    public boolean isActive(String sessionId, Long userId) {
        String current = redisTemplate.opsForValue().get(key(sessionId));
        return current != null && current.startsWith(userId + "|");
    }

    @Override
    public boolean rotateRefreshToken(String sessionId, Long userId, String currentRefreshTokenId,
                                      String newRefreshTokenId, Duration ttl) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                Collections.singletonList(key(sessionId)),
                value(userId, currentRefreshTokenId),
                value(userId, newRefreshTokenId),
                String.valueOf(ttl.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return properties.getSessionPrefix() + sessionId;
    }

    private static String value(Long userId, String refreshTokenId) {
        return userId + "|" + refreshTokenId;
    }
}
