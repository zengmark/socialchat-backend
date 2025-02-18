package com.socialchat.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.CollectConstant;
import com.socialchat.constant.MessageConstant;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.dto.CollectAddDTO;
import com.socialchat.model.entity.Collect;
import com.socialchat.model.entity.CollectCount;
import com.socialchat.model.entity.Message;
import com.socialchat.model.entity.MessageCount;
import com.socialchat.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
@RocketMQMessageListener(topic = "collect-topic", consumerGroup = "collect-consumer-group")
@Slf4j
public class CollectRocketMQConsumer implements RocketMQListener<String> {

    @Resource
    private CollectService collectService;

    @Resource
    private CollectCountService collectCountService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SseService sseService;

    @Resource
    private MessageService messageService;

    @Resource
    private MessageCountService messageCountService;

    @Override
    public void onMessage(String collectAddDTOJSON) {
        CollectAddDTO collectAddDTO = JSON.parseObject(collectAddDTOJSON, CollectAddDTO.class);
        log.info("收藏服务收到消息:{}", collectAddDTO);

        Long targetId = collectAddDTO.getTargetId();
        Integer targetType = collectAddDTO.getTargetType();
        Long collectUserId = collectAddDTO.getCollectUserId();
        Integer collectAction = collectAddDTO.getCollectAction();
        Long userId = collectAddDTO.getUserId();
        Date createTime = collectAddDTO.getCreateTime();

        // 1、查询收藏记录表中是否存在收藏记录
        LambdaQueryWrapper<Collect> collectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        collectLambdaQueryWrapper.eq(Collect::getTargetId, targetId);
        collectLambdaQueryWrapper.eq(Collect::getTargetType, targetType);
        collectLambdaQueryWrapper.eq(Collect::getCollectUserId, collectUserId);
        Collect collect = collectService.getOne(collectLambdaQueryWrapper);

        // 2.1、如果不存在收藏记录，那么直接保存用户的收藏记录到记录表中，并且插入收藏计数表中的计数数据
        if (collect == null) {
            if (CollectConstant.DISCOLLECT.equals(collectAction)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未存在收藏记录，无法取消收藏");
            }
            collect = new Collect();
            collect.setTargetId(targetId);
            collect.setTargetType(targetType);
            collect.setCollectUserId(collectUserId);
            collect.setCollectAction(collectAction);
            collectService.save(collect);

            LambdaQueryWrapper<CollectCount> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CollectCount::getTargetId, targetId);
            queryWrapper.eq(CollectCount::getTargetType, targetType);
            CollectCount collectCount = collectCountService.getOne(queryWrapper);
            // 如果记录不存在，插入
            if (collectCount == null) {
                collectCount = new CollectCount();
                collectCount.setTargetId(targetId);
                collectCount.setTargetType(targetType);
                collectCount.setUserId(userId);
                collectCount.setCollectNum(0);
                collectCountService.save(collectCount);
            }

            // 更新收藏计数表
            Integer collectNum = collectCount.getCollectNum();
            if (CollectConstant.COLLECT.equals(collectAction)) {
                collectNum += 1;
            } else {
                collectNum -= 1;
            }
            collectCount.setCollectNum(collectNum);
            collectCountService.updateById(collectCount);

            // 直接更新 Redis
            updateRedisData(targetType, targetId, collectUserId, collectAction, collectNum - 1);
            return;
        }

        // 2.2、如果存在收藏记录，判断数据库中记录的收藏时间是否大于MQ中的收藏时间
        if (collect.getCreateTime().after(createTime)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏时间错误，无法重复收藏");
        }

        // 2.3、如果数据库中的收藏时间小于MQ中的收藏时间，判断当前操作的 collectAction 是否跟数据库中的状态一致，
        // 不一致则更新
        if (collect.getCollectAction().equals(collectAction)) {
            log.info("收藏状态一致，无需更新，收藏记录为{}", collect);
            return;
        }

        // 查询收藏计数表中对应的计数数据
        LambdaQueryWrapper<CollectCount> collectCountQueryWrapper = new LambdaQueryWrapper<>();
        collectCountQueryWrapper.eq(CollectCount::getTargetId, targetId);
        collectCountQueryWrapper.eq(CollectCount::getTargetType, targetType);
        CollectCount collectCount = collectCountService.getOne(collectCountQueryWrapper);

        if (collectCount == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏计数表中不存在该收藏记录");
        }

        // 更新收藏记录表
        collect.setCollectAction(collectAction);
        collectService.updateById(collect);

        // 更新收藏计数表
        Integer collectNum = collectCount.getCollectNum();
        if (CollectConstant.COLLECT.equals(collectAction)) {
            collectNum += 1;
        } else {
            collectNum -= 1;
        }
        collectCount.setCollectNum(collectNum);
        collectCountService.updateById(collectCount);

        // 更新 Redis
        updateRedisData(targetType, targetId, collectUserId, collectAction, collectNum);

        // 初始化消息记录表
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper.eq(Message::getTargetType, MessageConstant.COLLECT);
        messageQueryWrapper.eq(Message::getTargetId, collect.getId());
        messageQueryWrapper.eq(Message::getSourceUserId, collectUserId);
        messageQueryWrapper.eq(Message::getAcceptUserId, userId);
        Message message = messageService.getOne(messageQueryWrapper);
        if (message == null) {
            message = new Message();
            message.setTargetType(MessageConstant.COLLECT);
            message.setTargetId(collect.getId());
            message.setSourceUserId(collectUserId);
            message.setAcceptUserId(userId);
            messageService.save(message);
        }

        message = messageService.getById(message.getId());

        // 初始化消息计数表
        LambdaQueryWrapper<MessageCount> messageCountQueryWrapper = new LambdaQueryWrapper<>();
        messageCountQueryWrapper.eq(MessageCount::getUserId, userId);
        MessageCount messageCount = messageCountService.getOne(messageCountQueryWrapper);
        if (messageCount == null) {
            messageCount = new MessageCount();
            messageCount.setUserId(userId);
            messageCountService.save(messageCount);
        }

        messageCount = messageCountService.getById(messageCount.getId());

        // 收藏处理
        int messageNum = messageCount.getMessageCount();
        if (CollectConstant.COLLECT.equals(collectAction)) {
            // 更新消息记录
            message.setVisible(MessageConstant.NEW);
            messageService.updateById(message);
            // 更新消息计数
            messageNum++;
            messageCount.setMessageCount(messageNum);
        }

        // 取消收藏处理
        if (CollectConstant.DISCOLLECT.equals(collectAction)) {
            // 删除消息记录表
            messageService.removeById(message.getId());
            // 更新消息计数
            messageNum--;
            messageCount.setMessageCount(messageNum);
        }

        // 更新消息计数表
        messageCountService.updateById(messageCount);

        // 推送 SSE 消息
        sseService.sendNotificationToUser(String.valueOf(userId), String.valueOf(messageNum));
    }

    /**
     * 同步数据到 Redis
     *
     * @param targetType
     * @param targetId
     * @param collectUserId
     * @param collectAction
     * @param collectNum
     */
    private void updateRedisData(Integer targetType, Long targetId, Long collectUserId, Integer collectAction, Integer collectNum) {
        String collectRecordRedisKey = String.format(CollectConstant.COLLECT_RECORD_KEY, targetType, targetId);
        String collectCountRedisKey = String.format(CollectConstant.COLLECT_COUNT_KEY, targetType, targetId);

        // 3.1、判断 redisKey 是否存在，不存在先从 MySQL 中获取数据
        if (!redisTemplate.hasKey(collectCountRedisKey)) {
            // 为收藏计数缓存添加默认值
            redisTemplate.opsForValue().set(collectCountRedisKey, collectNum, Duration.ofDays(7));

            // 为收藏列表缓存添加默认值
            redisTemplate.opsForZSet().add(collectRecordRedisKey, "test", 0);
            redisTemplate.opsForZSet().remove(collectRecordRedisKey, "test");
            redisTemplate.expire(collectRecordRedisKey, Duration.ofDays(7));

            // 从 MySQL 中查出收藏记录
            LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Collect::getTargetType, targetType);
            queryWrapper.eq(Collect::getTargetId, targetId);
            queryWrapper.eq(Collect::getCollectAction, CollectConstant.COLLECT);
            long count = collectService.count(queryWrapper);

            if (count > 0) {
                List<Collect> collectRecordList;
                if (count > 1000) {
                    Page<Collect> collectRecordPage = collectService.page(new Page<>(1, 1000), queryWrapper);
                    collectRecordList = collectRecordPage.getRecords();
                } else {
                    collectRecordList = collectService.list(queryWrapper);
                }

                for (Collect collectRecord : collectRecordList) {
                    redisTemplate.opsForZSet().add(collectRecordRedisKey, collectRecord.getCollectUserId(), collectRecord.getCreateTime().getTime());
                }
            }
        }

        // 3.2、更新收藏计数缓存
        redisTemplate.opsForValue().increment(collectCountRedisKey, CollectConstant.COLLECT.equals(collectAction) ? 1 : -1);

        // 3.3、更新收藏列表缓存
        if (CollectConstant.COLLECT.equals(collectAction)) {
            Long size = redisTemplate.opsForZSet().zCard(collectRecordRedisKey);
            // 先判断当前 zset 长度是否超过 1000，如果超过，那么就移除最早的收藏的记录，并且添加新的收藏记录
            if (size != null && size >= CollectConstant.MAX_COLLECT_RECORD_LENGTH) {
                redisTemplate.opsForZSet().removeRange(collectRecordRedisKey, 0, 0);
            }
            redisTemplate.opsForZSet().add(collectRecordRedisKey, collectUserId, System.currentTimeMillis());
        } else {
            redisTemplate.opsForZSet().remove(collectRecordRedisKey, collectUserId);
        }
    }
}
