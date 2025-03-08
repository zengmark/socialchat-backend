package com.socialchat.api;

import com.socialchat.model.websocket.ChatMessagePayload;

public interface ChatRemoteService {

    /**
     * 如果聊天室不存在，初始化单聊聊天室
     *
     * @param roomId
     * @param userId
     */
    void initChatRoom(Long roomId, Long userId);

    /**
     * 如果用户不存在聊天室用户表，则加入
     *
     * @param roomId
     * @param userId
     */
    void joinChatRoom(Long roomId, Long userId);

    /**
     * 用户退出聊天室，将其从聊天室用户表清除
     *
     * @param roomId
     * @param userId
     */
    void removeUserFromChatRoom(Long roomId, Long userId);

    /**
     * 持久化聊天消息
     *
     * @param chatMessagePayload
     */
    void addChatMessage(ChatMessagePayload chatMessagePayload);
}
