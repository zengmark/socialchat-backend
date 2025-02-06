package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.LikeRemoteService;
import com.socialchat.constant.LikeConstant;
import com.socialchat.dao.LikeCountMapper;
import com.socialchat.dao.LikeMapper;
import com.socialchat.model.entity.Like;
import com.socialchat.model.entity.LikeCount;
import com.socialchat.model.remote.like.LikeCountDTO;
import com.socialchat.utils.DistributedLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DubboService
@Slf4j
public class LikeRemoteServiceImpl implements LikeRemoteService {

    @Resource
    private LikeCountMapper likeCountMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private DistributedLockUtil distributedLockUtil;

    @Override
    public Long countLikeData(Date startDate, Date endDate) {
        LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCount::getTargetType, LikeConstant.POST_TYPE);
        queryWrapper.between(LikeCount::getUpdateTime, startDate, endDate);
        return likeCountMapper.selectCount(queryWrapper);
    }

    @Override
    public List<LikeCountDTO> listLikeData(int current, Date startDate, Date endDate) {
        LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCount::getTargetType, LikeConstant.POST_TYPE);
        queryWrapper.between(LikeCount::getUpdateTime, startDate, endDate);

        Page<LikeCount> likeCountPage = likeCountMapper.selectPage(new Page<>(current, LikeConstant.LIKE_PAGE_SIZE), queryWrapper);
        List<LikeCount> likeCountList = likeCountPage.getRecords();

        return likeCountList.stream().map(likeCount -> {
            LikeCountDTO likeCountDTO = new LikeCountDTO();
            likeCountDTO.setPostId(likeCount.getTargetId());
            likeCountDTO.setLikeNum(likeCount.getLikeNum());
            return likeCountDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> listPostIdByLikeNum(int current, int pageSize) {
        LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCount::getTargetType, LikeConstant.POST_TYPE);
        queryWrapper.orderByDesc(LikeCount::getLikeNum);
        Page<LikeCount> likeCountPage = likeCountMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<LikeCount> likeCountList = likeCountPage.getRecords();
        return likeCountList.stream().map(LikeCount::getTargetId).collect(Collectors.toList());
    }

    @Override
    public Integer countLikeByTargetIdAndTargetType(Long targetId, Integer targetType) {
        // 先从 Redis 中读取
        String likeCountRedisKey = String.format(LikeConstant.LIKE_COUNT_KEY, targetType, targetId);
        Integer likeCount = (Integer) redisTemplate.opsForValue().get(likeCountRedisKey);
        if (likeCount != null) {
            return likeCount;
        }

        // 如果 Redis 中没有，则从数据库中读取，并且更新数据到 Redis 中
        likeCount = updateLikeDataToRedis(targetId, targetType);
        return likeCount;
    }

    @Override
    public Boolean checkLike(Long userId, Long targetId, Integer targetType) {
        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getLikeUserId, userId);
        queryWrapper.eq(Like::getTargetId, targetId);
        queryWrapper.eq(Like::getTargetType, targetType);
        Like like = likeMapper.selectOne(queryWrapper);
        return like != null && LikeConstant.LIKE.equals(like.getLikeAction());
    }

    private Integer updateLikeDataToRedis(Long targetId, Integer targetType) {
        // 先加分布式锁
        String likeLockKey = String.format(LikeConstant.LIKE_LOCK_KEY, targetType, targetId);
        String likeLockValue = UUID.randomUUID().toString();
        Integer likeNum = 0;
        if (distributedLockUtil.acquireLock(
                likeLockKey,
                likeLockValue,
                LikeConstant.MAX_LIKE_RETRIES,
                LikeConstant.LIKE_TIME_INTERVAL,
                LikeConstant.TIME_UNIT,
                LikeConstant.LIKE_LOCK_EXPIRE_TIME
        )) {
            try {
                // 加锁成功，更新 Redis 中的数据
                String likeRecordRedisKey = String.format(LikeConstant.LIKE_RECORD_KEY, targetType, targetId);
                String likeCountRedisKey = String.format(LikeConstant.LIKE_COUNT_KEY, targetType, targetId);
                if (redisTemplate.hasKey(likeRecordRedisKey) && redisTemplate.hasKey(likeCountRedisKey)) {
                    return (Integer) redisTemplate.opsForValue().get(likeCountRedisKey);
                }

                // 更新点赞计数缓存
                LambdaQueryWrapper<LikeCount> likeCountLambdaQueryWrapper = new LambdaQueryWrapper<>();
                likeCountLambdaQueryWrapper.eq(LikeCount::getTargetId, targetId);
                likeCountLambdaQueryWrapper.eq(LikeCount::getTargetType, targetType);
                LikeCount likeCount = likeCountMapper.selectOne(likeCountLambdaQueryWrapper);

                if (likeCount != null) {
                    likeNum = likeCount.getLikeNum();
                }
                redisTemplate.opsForValue().set(likeCountRedisKey, likeNum, Duration.ofDays(7));

                // 更新点赞列表缓存
                redisTemplate.opsForZSet().add(likeRecordRedisKey, "test", 0);
                redisTemplate.opsForZSet().remove(likeRecordRedisKey, "test");
                redisTemplate.expire(likeRecordRedisKey, Duration.ofDays(7));

                LambdaQueryWrapper<Like> likeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                likeLambdaQueryWrapper.eq(Like::getTargetId, targetId);
                likeLambdaQueryWrapper.eq(Like::getTargetType, targetType);
                likeLambdaQueryWrapper.eq(Like::getLikeAction, LikeConstant.LIKE);
                Long count = likeMapper.selectCount(likeLambdaQueryWrapper);

                if (count > 0) {
                    List<Like> likeRecordList;
                    if (count > 1000) {
                        Page<Like> likeRecordPage = likeMapper.selectPage(new Page<>(1, 1000), likeLambdaQueryWrapper);
                        likeRecordList = likeRecordPage.getRecords();
                    } else {
                        likeRecordList = likeMapper.selectList(likeLambdaQueryWrapper);
                    }

                    for (Like likeRecord : likeRecordList) {
                        redisTemplate.opsForZSet().add(likeRecordRedisKey, likeRecord.getLikeUserId(), likeRecord.getCreateTime().getTime());
                    }
                }
            } catch (Exception e) {
                log.error("获取点赞记录失败", e);
            } finally {
                // 释放锁
                distributedLockUtil.releaseLock(likeLockKey, likeLockValue);
            }
        } else {
            log.info("获取分布式锁失败，达到重试次数上限");
        }
        return likeNum;
    }
}
