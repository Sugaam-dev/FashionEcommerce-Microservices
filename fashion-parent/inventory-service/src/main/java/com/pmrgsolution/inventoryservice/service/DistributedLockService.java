package com.pmrgsolution.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);

    public boolean acquireLock(String lockKey, String lockToken, Duration ttl) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, ttl);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("Failed to acquire Redis lock on key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    public boolean acquireLockWithRetry(String lockKey, String lockToken, Duration ttl, int maxRetries, long retryIntervalMs) {
        for (int i = 0; i <= maxRetries; i++) {
            if (acquireLock(lockKey, lockToken, ttl)) {
                return true;
            }
            if (i < maxRetries) {
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    public boolean acquireLocks(List<String> lockKeys, String lockToken, Duration ttl, int maxRetries, long retryIntervalMs) {
        List<String> acquiredKeys = new ArrayList<>();
        for (String key : lockKeys) {
            boolean acquired = acquireLockWithRetry(key, lockToken, ttl, maxRetries, retryIntervalMs);
            if (acquired) {
                acquiredKeys.add(key);
            } else {
                // Rollback previously acquired locks in this batch
                releaseLocks(acquiredKeys, lockToken);
                return false;
            }
        }
        return true;
    }

    public boolean releaseLock(String lockKey, String lockToken) {
        try {
            Long result = stringRedisTemplate.execute(unlockScript, Collections.singletonList(lockKey), lockToken);
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Failed to safely release Redis lock on key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    public void releaseLocks(List<String> lockKeys, String lockToken) {
        for (String key : lockKeys) {
            releaseLock(key, lockToken);
        }
    }
}
