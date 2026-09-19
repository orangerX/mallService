package com.mall.exam.paper.lock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;
@Component
public class RedisExamGenerationLock implements ExamGenerationLock {
    private static final DefaultRedisScript<Long> RELEASE=new DefaultRedisScript<>(
        "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",Long.class);
    private final StringRedisTemplate redis;
    public RedisExamGenerationLock(StringRedisTemplate redis){this.redis=redis;}
    public String acquire(String key,Duration timeout) {
        String token=UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,token,timeout))?token:null;
    }
    public void release(String key,String ownerToken) {
        redis.execute(RELEASE,Collections.singletonList(key),ownerToken);
    }
}
