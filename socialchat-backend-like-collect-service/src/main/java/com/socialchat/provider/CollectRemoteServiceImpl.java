package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.CollectRemoteService;
import com.socialchat.constant.CollectConstant;
import com.socialchat.dao.CollectCountMapper;
import com.socialchat.dao.CollectMapper;
import com.socialchat.model.entity.Collect;
import com.socialchat.model.entity.CollectCount;
import com.socialchat.utils.DistributedLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@DubboService
@Slf4j
public class CollectRemoteServiceImpl implements CollectRemoteService {

    @Resource
    private CollectCountMapper collectCountMapper;

    @Resource
    private CollectMapper collectMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private DistributedLockUtil distributedLockUtil;

    @Override
    public Integer countCollectByTargetIdAndTargetType(Long targetId, Integer targetType) {
        // 先从 Redis 中读取
        String collectCountRedisKey = String.format(CollectConstant.COLLECT_COUNT_KEY, targetType, targetId);
        Integer collectCount = (Integer) redisTemplate.opsForValue().get(collectCountRedisKey);
        if (collectCount != null) {
            return collectCount;
        }

        // 如果 Redis 中没有，则从数据库中读取，并且更新数据到 Redis 中
        collectCount = updateCollectDataToRedis(targetId, targetType);
        return collectCount;
    }

    private Integer updateCollectDataToRedis(Long targetId, Integer targetType) {
        // 先加分布式锁
        String collectLockKey = String.format(CollectConstant.COLLECT_LOCK_KEY, targetType, targetId);
        String collectLockValue = UUID.randomUUID().toString();
        Integer collectNum = 0;
        if (distributedLockUtil.acquireLock(
                collectLockKey,
                collectLockValue,
                CollectConstant.MAX_COLLECT_RETRIES,
                CollectConstant.COLLECT_TIME_INTERVAL,
                CollectConstant.TIME_UNIT,
                CollectConstant.COLLECT_LOCK_EXPIRE_TIME
        )) {
            try {
                // 加锁成功，更新 Redis 中的数据
                String collectRecordRedisKey = String.format(CollectConstant.COLLECT_RECORD_KEY, targetType, targetId);
                String collectCountRedisKey = String.format(CollectConstant.COLLECT_COUNT_KEY, targetType, targetId);
                if (redisTemplate.hasKey(collectRecordRedisKey) && redisTemplate.hasKey(collectCountRedisKey)) {
                    return (Integer) redisTemplate.opsForValue().get(collectCountRedisKey);
                }

                // 更新缓存计数缓存
                LambdaQueryWrapper<CollectCount> collectCountLambdaQueryWrapper = new LambdaQueryWrapper<>();
                collectCountLambdaQueryWrapper.eq(CollectCount::getTargetId, targetId);
                collectCountLambdaQueryWrapper.eq(CollectCount::getTargetType, targetType);
                CollectCount collectCount = collectCountMapper.selectOne(collectCountLambdaQueryWrapper);

                if (collectCount != null) {
                    collectNum = collectCount.getCollectNum();
                }
                redisTemplate.opsForValue().set(collectCountRedisKey, collectNum, Duration.ofDays(7));

                // 更新收藏列表缓存
                redisTemplate.opsForZSet().add(collectRecordRedisKey, "test", 0);
                redisTemplate.opsForZSet().remove(collectRecordRedisKey, "test");
                redisTemplate.expire(collectRecordRedisKey, Duration.ofDays(7));

                LambdaQueryWrapper<Collect> collectLambdaQueryWrapper = new LambdaQueryWrapper<>();
                collectLambdaQueryWrapper.eq(Collect::getTargetId, targetId);
                collectLambdaQueryWrapper.eq(Collect::getTargetType, targetType);
                collectLambdaQueryWrapper.eq(Collect::getCollectAction, CollectConstant.COLLECT);
                Long count = collectMapper.selectCount(collectLambdaQueryWrapper);

                if (count > 0) {
                    List<Collect> collectRecordList;
                    if (count > 1000) {
                        Page<Collect> collectRecordPage = collectMapper.selectPage(new Page<>(1, 1000), collectLambdaQueryWrapper);
                        collectRecordList = collectRecordPage.getRecords();
                    } else {
                        collectRecordList = collectMapper.selectList(collectLambdaQueryWrapper);
                    }

                    for (Collect collectRecord : collectRecordList) {
                        redisTemplate.opsForZSet().add(collectRecordRedisKey, collectRecord.getCollectUserId(), collectRecord.getCreateTime().getTime());
                    }
                }
            } catch (Exception e) {
                log.error("获取收藏记录失败", e);
            } finally {
                // 释放锁
                distributedLockUtil.releaseLock(collectLockKey, collectLockValue);
            }
        } else {
            log.info("获取分布式锁失败，达到重试次数上限");
        }
        return collectNum;
    }
}
