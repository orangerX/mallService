package com.mall.admin.auth.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class AdminSessionService {
    private static final String PREFIX = "mall:admin:session:";
    private static final DefaultRedisScript<Long> ROTATE = new DefaultRedisScript<>(
            "local v=redis.call('GET',KEYS[1]); if not v or v~=ARGV[1] then return 0 end; "
                    + "redis.call('SET',KEYS[1],ARGV[2],'PX',ARGV[3]); return 1;", Long.class);
    private final StringRedisTemplate redis;
    public AdminSessionService(StringRedisTemplate redis) { this.redis = redis; }
    public void create(String sid, Long id, String refreshId, Duration ttl) {
        redis.opsForValue().set(PREFIX + sid, value(id, refreshId), ttl);
    }
    public boolean active(String sid, Long id) {
        String value = redis.opsForValue().get(PREFIX + sid);
        return value != null && value.startsWith(id + "|");
    }
    public boolean rotate(String sid, Long id, String current, String next, Duration ttl) {
        Long result = redis.execute(ROTATE, Collections.singletonList(PREFIX + sid),
                value(id, current), value(id, next), String.valueOf(ttl.toMillis()));
        return Long.valueOf(1).equals(result);
    }
    public void delete(String sid) { redis.delete(PREFIX + sid); }
    private static String value(Long id, String refreshId) { return id + "|" + refreshId; }
}
