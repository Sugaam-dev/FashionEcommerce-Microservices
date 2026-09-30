package com.pmrgsolution.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    public boolean acquireLock(String lockKey, String lockValue, Duration expireDuration) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireDuration);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("Redis lock error for key {}: {}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    public void releaseLock(String lockKey, String lockValue) {
        try {
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue != null && lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.error("Redis release lock error for key {}: {}", lockKey, e.getMessage(), e);
        }
    }

    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get error for key {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    public void set(String key, String value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
        } catch (Exception e) {
            log.error("Redis set error for key {}: {}", key, e.getMessage(), e);
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis hasKey error for key {}: {}", key, e.getMessage(), e);
            return false;
        }
    }
}
