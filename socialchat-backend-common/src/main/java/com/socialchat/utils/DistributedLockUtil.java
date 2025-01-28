package com.socialchat.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DistributedLockUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean acquireLock(String lockKey, String lockValue, int maxRetries, Long timeInterval, TimeUnit timeUnit, Duration expireTime) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime);
            if (Boolean.TRUE.equals(lockAcquired)) {
                return true;
            }

            log.info("未能获取到锁，重试中... ({}/{})", attempt, maxRetries);
            try {
                timeUnit.sleep(timeInterval);
            } catch (InterruptedException e) {
                log.error("获取锁时发生中断异常", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    public void releaseLock(String lockKey, String lockValue) {
        if (stringRedisTemplate.hasKey(lockKey)) {
            String redisValue = stringRedisTemplate.opsForValue().get(lockKey);
            if (redisValue.equals(lockValue)) {
                log.info("解锁成功，lockKey:{}，lockValue:{}", lockKey, lockValue);
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

}
