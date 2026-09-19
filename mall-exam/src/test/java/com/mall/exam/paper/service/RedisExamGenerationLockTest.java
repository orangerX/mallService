package com.mall.exam.paper.service;
import com.mall.exam.paper.lock.RedisExamGenerationLock;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RedisExamGenerationLockTest {
    @Test void leasesUseUniqueTokensAndAtomicCompareDelete() {
        StringRedisTemplate redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),anyString(),eq(Duration.ofSeconds(10)))).thenReturn(true,false,null,true);
        RedisExamGenerationLock lock=new RedisExamGenerationLock(redis);
        String first=lock.acquire("key",Duration.ofSeconds(10));assertNotNull(first);
        assertNull(lock.acquire("key",Duration.ofSeconds(10)));assertNull(lock.acquire("key",Duration.ofSeconds(10)));
        assertNotEquals(first,lock.acquire("key",Duration.ofSeconds(10)));
        lock.release("key",first);
        org.mockito.ArgumentCaptor<RedisScript<Long>> script=org.mockito.ArgumentCaptor.forClass(RedisScript.class);
        verify(redis).execute(script.capture(),eq(List.of("key")),eq(first));
        assertTrue(script.getValue().getScriptAsString().contains("redis.call('GET', KEYS[1]) == ARGV[1]"));
        assertTrue(script.getValue().getScriptAsString().contains("redis.call('DEL', KEYS[1])"));
        verify(redis,never()).delete(anyString());
    }
}
