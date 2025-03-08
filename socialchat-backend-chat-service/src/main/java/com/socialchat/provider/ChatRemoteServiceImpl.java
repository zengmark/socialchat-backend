package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.socialchat.api.ChatRemoteService;
import com.socialchat.api.MessageRemoteService;
import com.socialchat.constant.ChatMessageConstant;
import com.socialchat.dao.ChatMessageMapper;
import com.socialchat.dao.ChatRoomMapper;
import com.socialchat.dao.ChatRoomMemberMapper;
import com.socialchat.model.entity.ChatMessage;
import com.socialchat.model.entity.ChatRoom;
import com.socialchat.model.entity.ChatRoomMember;
import com.socialchat.model.remote.message.MessageChatDTO;
import com.socialchat.model.websocket.ChatMessagePayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@DubboService
@Slf4j
public class ChatRemoteServiceImpl implements ChatRemoteService {

    @Resource
    private ChatRoomMapper chatRoomMapper;

    @Resource
    private ChatRoomMemberMapper chatRoomMemberMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @DubboReference
    private MessageRemoteService messageRemoteService;

    @Transactional
    @Override
    public void initChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomMapper.selectById(roomId);
        if (chatRoom != null) {
            return;
        }
        log.info("聊天室不存在，新建聊天室{}", roomId);
        chatRoom = new ChatRoom();
        chatRoom.setId(roomId);
        chatRoom.setRoomName("单聊" + roomId);
        chatRoom.setRoomDescription("单聊" + roomId);
        chatRoomMapper.insert(chatRoom);

        LambdaQueryWrapper<ChatRoomMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRoomMember::getRoomId, roomId);
        queryWrapper.eq(ChatRoomMember::getUserId, userId);
        ChatRoomMember chatRoomMember = chatRoomMemberMapper.selectOne(queryWrapper);
        if (chatRoomMember != null) {
            return;
        }
        log.info("聊天室成员不存在，加入聊天室成员{}", userId);
        chatRoomMember = new ChatRoomMember();
        chatRoomMember.setRoomId(roomId);
        chatRoomMember.setUserId(userId);
        chatRoomMemberMapper.insert(chatRoomMember);
    }

    @Override
    public void joinChatRoom(Long roomId, Long userId) {
        LambdaQueryWrapper<ChatRoomMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRoomMember::getRoomId, roomId);
        queryWrapper.eq(ChatRoomMember::getUserId, userId);
        ChatRoomMember chatRoomMember = chatRoomMemberMapper.selectOne(queryWrapper);
        if (chatRoomMember == null) {
            log.info("聊天室成员不存在，加入聊天室成员{}", userId);
            chatRoomMember = new ChatRoomMember();
            chatRoomMember.setRoomId(roomId);
            chatRoomMember.setUserId(userId);
            chatRoomMemberMapper.insert(chatRoomMember);
        }
    }

    @Override
    public void removeUserFromChatRoom(Long roomId, Long userId) {
        LambdaQueryWrapper<ChatRoomMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRoomMember::getRoomId, roomId);
        queryWrapper.eq(ChatRoomMember::getUserId, userId);
        chatRoomMemberMapper.delete(queryWrapper);
    }

    @Override
    public void addChatMessage(ChatMessagePayload chatMessagePayload) {
        Integer type = chatMessagePayload.getType();
        String content = chatMessagePayload.getContent();
        Long targetId = chatMessagePayload.getTargetId();
        Long senderId = chatMessagePayload.getSenderId();
        Long receiverId = chatMessagePayload.getReceiverId();

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageType(type);
        chatMessage.setSenderId(senderId);
        chatMessage.setReceiverId(receiverId);
        chatMessage.setRoomId(targetId);
        chatMessage.setChatContent(content);
        chatMessageMapper.insert(chatMessage);

        // 单聊类型的时候写入消息表
        if (ChatMessageConstant.SINGLE.equals(type)) {
            MessageChatDTO messageChatDTO = new MessageChatDTO();
            messageChatDTO.setTargetId(chatMessage.getId());
            messageChatDTO.setSourceUserId(senderId);
            messageChatDTO.setAcceptUserId(receiverId);
            messageRemoteService.sendChatMessage(messageChatDTO);
        }

    }
}
