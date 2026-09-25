package com.pmrgsolution.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

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

    public boolean releaseLock(String lockKey, String lockToken) {
        try {
            Long result = stringRedisTemplate.execute(unlockScript, Collections.singletonList(lockKey), lockToken);
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Failed to safely release Redis lock on key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }
}
