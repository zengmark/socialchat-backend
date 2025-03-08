package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.socialchat.api.MessageRemoteService;
import com.socialchat.constant.CommentConstant;
import com.socialchat.constant.MessageConstant;
import com.socialchat.model.entity.Message;
import com.socialchat.model.entity.MessageCount;
import com.socialchat.model.remote.message.MessageChatDTO;
import com.socialchat.model.remote.message.MessageCommentDTO;
import com.socialchat.service.MessageCountService;
import com.socialchat.service.MessageService;
import com.socialchat.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

@DubboService
@Slf4j
public class MessageRemoteServiceImpl implements MessageRemoteService {

    @Resource
    private MessageService messageService;

    @Resource
    private MessageCountService messageCountService;

    @Resource
    private SseService sseService;

    @Override
    public void sendCommentMessage(MessageCommentDTO messageCommentDTO) {
        Long targetId = messageCommentDTO.getTargetId();
        Long sourceUserId = messageCommentDTO.getSourceUserId();
        Long acceptUserId = messageCommentDTO.getAcceptUserId();
        Integer commentAction = messageCommentDTO.getCommentAction();

        // 初始化消息记录表
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper.eq(Message::getTargetType, MessageConstant.COMMENT);
        messageQueryWrapper.eq(Message::getTargetId, targetId);
        messageQueryWrapper.eq(Message::getSourceUserId, sourceUserId);
        messageQueryWrapper.eq(Message::getAcceptUserId, acceptUserId);
        Message message = messageService.getOne(messageQueryWrapper);
        if (message == null) {
            message = new Message();
            message.setTargetType(MessageConstant.COMMENT);
            message.setTargetId(targetId);
            message.setSourceUserId(sourceUserId);
            message.setAcceptUserId(acceptUserId);
            messageService.save(message);
        }

        message = messageService.getById(message.getId());

        // 初始化消息计数表
        LambdaQueryWrapper<MessageCount> messageCountQueryWrapper = new LambdaQueryWrapper<>();
        messageCountQueryWrapper.eq(MessageCount::getUserId, acceptUserId);
        MessageCount messageCount = messageCountService.getOne(messageCountQueryWrapper);
        if (messageCount == null) {
            messageCount = new MessageCount();
            messageCount.setUserId(acceptUserId);
            messageCountService.save(messageCount);
        }

        messageCount = messageCountService.getById(messageCount.getId());

        // 评论处理
        int messageNum = messageCount.getMessageCount();
        if (CommentConstant.NEW.equals(commentAction)) {
            // 更新消息记录
            message.setVisible(MessageConstant.NEW);
            messageService.updateById(message);
            // 更新消息计数
            messageNum++;
            messageCount.setMessageCount(messageNum);
        }

        // 取消评论处理
        if (CommentConstant.DELETE.equals(commentAction)) {
            // 删除消息记录表
            messageService.removeById(message.getId());
            // 更新消息计数
            messageNum--;
            messageCount.setMessageCount(messageNum);
        }

        // 更新消息计数表
        messageCountService.updateById(messageCount);

        // 推送 SSE 消息
        sseService.sendNotificationToUser(String.valueOf(acceptUserId), String.valueOf(messageNum));
    }

    @Override
    public void sendChatMessage(MessageChatDTO messageChatDTO) {
        Long targetId = messageChatDTO.getTargetId();
        Long sourceUserId = messageChatDTO.getSourceUserId();
        Long acceptUserId = messageChatDTO.getAcceptUserId();

        // 初始化消息记录表
        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper.eq(Message::getTargetType, MessageConstant.CHAT);
        messageQueryWrapper.eq(Message::getTargetId, targetId);
        messageQueryWrapper.eq(Message::getSourceUserId, sourceUserId);
        messageQueryWrapper.eq(Message::getAcceptUserId, acceptUserId);
        Message message = messageService.getOne(messageQueryWrapper);
        if (message == null) {
            message = new Message();
            message.setTargetType(MessageConstant.CHAT);
            message.setTargetId(targetId);
            message.setSourceUserId(sourceUserId);
            message.setAcceptUserId(acceptUserId);
            messageService.save(message);
        }

        // 初始化消息计数表
        LambdaQueryWrapper<MessageCount> messageCountQueryWrapper = new LambdaQueryWrapper<>();
        messageCountQueryWrapper.eq(MessageCount::getUserId, acceptUserId);
        MessageCount messageCount = messageCountService.getOne(messageCountQueryWrapper);
        if (messageCount == null) {
            messageCount = new MessageCount();
            messageCount.setUserId(acceptUserId);
            messageCountService.save(messageCount);
        }

        messageCount = messageCountService.getById(messageCount.getId());
        messageCount.setMessageCount(messageCount.getMessageCount() + 1);

        // 更新消息计数表
        messageCountService.updateById(messageCount);

        // 推送 SSE 消息
        sseService.sendNotificationToUser(String.valueOf(acceptUserId), String.valueOf(messageCount.getMessageCount() + 1));
    }
}
