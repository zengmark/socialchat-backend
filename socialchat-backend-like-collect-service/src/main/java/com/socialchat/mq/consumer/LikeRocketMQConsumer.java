package com.socialchat.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.PostRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.LikeConstant;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.dto.LikeAddDTO;
import com.socialchat.model.entity.Like;
import com.socialchat.model.entity.LikeCount;
import com.socialchat.service.LikeCountService;
import com.socialchat.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
@RocketMQMessageListener(topic = "like-topic", consumerGroup = "like-consumer-group")
@Slf4j
public class LikeRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private LikeService likeService;

    @Resource
    private LikeCountService likeCountService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @DubboReference
    private PostRemoteService postRemoteService;

    @Transactional
    @Override
    public void onMessage(String likeAddDTOJSON) {
        LikeAddDTO likeAddDTO = JSON.parseObject(likeAddDTOJSON, LikeAddDTO.class);
        log.info("点赞服务收到消息:{}", likeAddDTO);

        Long targetId = likeAddDTO.getTargetId();
        Integer targetType = likeAddDTO.getTargetType();
        Long likeUserId = likeAddDTO.getLikeUserId();
        Integer likeAction = likeAddDTO.getLikeAction();
        Long userId = likeAddDTO.getUserId();
        Date createTime = likeAddDTO.getCreateTime();

        // 1、查询点赞记录表中是否存在点赞记录
        LambdaQueryWrapper<Like> likeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likeLambdaQueryWrapper.eq(Like::getTargetId, targetId);
        likeLambdaQueryWrapper.eq(Like::getTargetType, targetType);
        likeLambdaQueryWrapper.eq(Like::getLikeUserId, likeUserId);
        Like like = likeService.getOne(likeLambdaQueryWrapper);

        // 2.1、如果不存在点赞记录，那么直接保存用户的点赞记录到记录表中，并且插入点赞计数表中的计数数据
        if (like == null) {
            if (LikeConstant.DISLIKE.equals(likeAction)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未存在点赞记录，无法取消点赞");
            }
            like = new Like();
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            like.setLikeUserId(likeUserId);
            like.setLikeAction(likeAction);
            likeService.save(like);

            LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LikeCount::getTargetId, targetId);
            queryWrapper.eq(LikeCount::getTargetType, targetType);
            LikeCount likeCount = likeCountService.getOne(queryWrapper);
            // 如果记录不存在，插入
            if (likeCount == null) {
                likeCount = new LikeCount();
                likeCount.setTargetId(targetId);
                likeCount.setTargetType(targetType);
                likeCount.setUserId(userId);
                likeCount.setLikeNum(0);
                likeCountService.save(likeCount);
            }

            // 更新点赞计数表
            Integer likeNum = likeCount.getLikeNum();
            if (LikeConstant.LIKE.equals(likeAction)) {
                likeNum += 1;
            } else {
                likeNum -= 1;
            }
            likeCount.setLikeNum(likeNum);
            likeCountService.updateById(likeCount);

            // 直接更新 Redis
            updateRedisData(targetType, targetId, likeUserId, likeAction, likeNum - 1);
            return;
        }

        // 2.2、如果存在点赞记录，判断数据库中记录的点赞时间是否大于MQ中的点赞时间
        if (like.getCreateTime().after(createTime)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞时间错误，无法重复点赞");
        }

        // 2.3、如果数据库中的点赞时间小于MQ中的点赞时间，判断当前操作的 likeAction 是否跟数据库中的状态一致，
        // 不一致则更新
        if (like.getLikeAction().equals(likeAction)) {
            log.info("点赞状态一致，无需更新，点赞记录为{}", like);
            return;
        }

        // 查询点赞计数表中对应的计数数据
        LambdaQueryWrapper<LikeCount> likeCountQueryWrapper = new LambdaQueryWrapper<>();
        likeCountQueryWrapper.eq(LikeCount::getTargetId, targetId);
        likeCountQueryWrapper.eq(LikeCount::getTargetType, targetType);
        LikeCount likeCount = likeCountService.getOne(likeCountQueryWrapper);

        if (likeCount == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞计数表中不存在该点赞记录");
        }

        // 更新点赞记录表
        like.setLikeAction(likeAction);
        likeService.updateById(like);

        // 更新点赞计数表
        Integer likeNum = likeCount.getLikeNum();
        if (LikeConstant.LIKE.equals(likeAction)) {
            likeNum += 1;
        } else {
            likeNum -= 1;
        }
        likeCount.setLikeNum(likeNum);
        likeCountService.updateById(likeCount);

        // 更新 Redis
        updateRedisData(targetType, targetId, likeUserId, likeAction, likeNum);
    }

    /**
     * 同步数据到 Redis
     *
     * @param targetType
     * @param targetId
     * @param likeUserId
     * @param likeAction
     * @param likeNum
     */
    private void updateRedisData(Integer targetType, Long targetId, Long likeUserId, Integer likeAction, Integer likeNum) {
        String likeRecordRedisKey = String.format(LikeConstant.LIKE_RECORD_KEY, targetType, targetId);
        String likeCountRedisKey = String.format(LikeConstant.LIKE_COUNT_KEY, targetType, targetId);

        // 3.1、判断 redisKey 是否存在，不存在先从 MySQL 中获取数据
        if (!redisTemplate.hasKey(likeCountRedisKey)) {
            // 为点赞计数缓存添加默认值
            redisTemplate.opsForValue().set(likeCountRedisKey, likeNum, Duration.ofDays(7));

            // 为点赞列表缓存添加默认值
            redisTemplate.opsForZSet().add(likeRecordRedisKey, "test", 0);
            redisTemplate.opsForZSet().remove(likeRecordRedisKey, "test");
            redisTemplate.expire(likeRecordRedisKey, Duration.ofDays(7));

            // 从 MySQL 中查出点赞记录
            LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Like::getTargetType, targetType);
            queryWrapper.eq(Like::getTargetId, targetId);
            queryWrapper.eq(Like::getLikeAction, LikeConstant.LIKE);
            long count = likeService.count(queryWrapper);

            if (count > 0) {
                List<Like> likeRecordList;
                if (count > 1000) {
                    Page<Like> likeRecordPage = likeService.page(new Page<>(1, 1000), queryWrapper);
                    likeRecordList = likeRecordPage.getRecords();
                } else {
                    likeRecordList = likeService.list(queryWrapper);
                }

                for (Like likeRecord : likeRecordList) {
                    redisTemplate.opsForZSet().add(likeRecordRedisKey, likeRecord.getLikeUserId(), likeRecord.getCreateTime().getTime());
                }
            }
        }

        // 3.2、更新点赞计数缓存
        Long likeCountRedis = redisTemplate.opsForValue().increment(likeCountRedisKey, LikeConstant.LIKE.equals(likeAction) ? 1 : -1);

        // 判断点赞数是否超过阈值，超过阈值，如果是帖子数据，直接同步到ES
        if (likeCountRedis != null && likeCountRedis % 10 == 0 && LikeConstant.POST_TYPE.equals(targetType)) {
            postRemoteService.syncLikeToESPartialUpdate(targetId, likeCountRedis);
        }

        // 3.3、更新点赞列表缓存
        if (LikeConstant.LIKE.equals(likeAction)) {
            Long size = redisTemplate.opsForZSet().zCard(likeRecordRedisKey);
            // 先判断当前 zset 长度是否超过 1000，如果超过，那么就移除最早点赞的记录，并且添加新的点赞记录
            if (size != null && size >= LikeConstant.MAX_LIKE_RECORD_LENGTH) {
                redisTemplate.opsForZSet().removeRange(likeRecordRedisKey, 0, 0);
            }
            redisTemplate.opsForZSet().add(likeRecordRedisKey, likeUserId, System.currentTimeMillis());
        } else {
            redisTemplate.opsForZSet().remove(likeRecordRedisKey, likeUserId);
        }
    }


}
