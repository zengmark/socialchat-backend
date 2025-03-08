package com.socialchat.handler;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson2.JSON;
import com.socialchat.api.ChatRemoteService;
import com.socialchat.api.MessageRemoteService;
import com.socialchat.constant.ChatMessageConstant;
import com.socialchat.model.websocket.ChatMessagePayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 单聊
    private static ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 群聊
    private static ConcurrentHashMap<Long, ConcurrentHashSet<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    @DubboReference
    private MessageRemoteService messageRemoteService;

    @DubboReference
    private ChatRemoteService chatRemoteService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接建立");
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");

        // 群聊
        if (roomId != null) {
            log.info("用户{}加入聊天室{}", userId, roomId);
            groupSessions.putIfAbsent(roomId, new ConcurrentHashSet<>());
            groupSessions.get(roomId).add(session);

            // 发起 rpc 请求，如果用户不存在聊天室用户表，则添加
            chatRemoteService.joinChatRoom(roomId, userId);
        }
        // 单聊
        if (userId != null) {
            log.info("用户{}建立单聊连接", userId);
            userSessions.put(userId, session);

            // 发起 rpc 请求，如果单聊聊天室不存在，新建一个
            chatRemoteService.initChatRoom(roomId, userId);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload(); // 假设前端发送的是 sessionId 作为纯文本
        ChatMessagePayload chatMessagePayload = JSON.parseObject(payload, ChatMessagePayload.class);

        Integer type = chatMessagePayload.getType();
        String content = chatMessagePayload.getContent();
        Long targetId = chatMessagePayload.getTargetId();
        Long senderId = chatMessagePayload.getSenderId();
        String senderName = chatMessagePayload.getSenderName();
        String senderAvatar = chatMessagePayload.getSenderAvatar();

        if (ChatMessageConstant.SINGLE.equals(type)) {
            // 单聊：直接查找接收者的会话
            WebSocketSession targetSession = userSessions.get(targetId);
            if (targetSession != null && targetSession.isOpen()) {
                log.info("发送单聊消息给用户{}: {}", targetId, content);
                targetSession.sendMessage(new TextMessage(JSON.toJSONString(chatMessagePayload)));
            } else {
                log.warn("用户{}当前不在线，消息未送达", targetId);
            }
        } else if (ChatMessageConstant.GROUP.equals(type)) {
            Set<WebSocketSession> groupSession = groupSessions.get(targetId);
            if (groupSession != null) {
                for (WebSocketSession groupReceiverSession : groupSession) {
                    if (groupReceiverSession != session && groupReceiverSession.isOpen()) {
                        groupReceiverSession.sendMessage(new TextMessage(JSON.toJSONString(
                                new ChatMessagePayload(senderId, senderName, senderAvatar, content)
                        )));
                    }
                }
                log.info("消息发送给聊天室{},内容为{}", targetId, content);
            }
        } else {
            log.error("消息格式有误{}", chatMessagePayload);
        }

        // 持久化消息到数据库，待用户上线后推送
        chatRemoteService.addChatMessage(chatMessagePayload);

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket 连接关闭");
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");

        // 单聊会话清理
        if (roomId == null) {
            WebSocketSession existingSession = userSessions.get(userId);
            if (existingSession == session) { // 确保仅清理当前会话
                userSessions.remove(userId);
                log.info("用户{}的单聊会话已关闭", userId);
            }
        }
        // 群聊会话清理（保持不变）
        else {
            Set<WebSocketSession> group = groupSessions.get(roomId);
            if (group != null) {
                group.remove(session);
                if (group.isEmpty()) {
                    groupSessions.remove(roomId);
                }
            }
        }

        // 发起 rpc 请求，将对应聊天室用户表数据清除
        chatRemoteService.removeUserFromChatRoom(roomId, userId);
    }

}
