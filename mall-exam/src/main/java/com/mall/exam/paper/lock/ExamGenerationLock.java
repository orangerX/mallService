package com.mall.exam.paper.lock;
import java.time.Duration;
public interface ExamGenerationLock {
    /** Returns the acquired ownership token, or null on contention/timeout. The duration is the lease TTL. */
    String acquire(String key,Duration timeout);
    void release(String key,String ownerToken);
}
