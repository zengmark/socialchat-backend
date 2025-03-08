package com.socialchat.api;

import com.socialchat.model.remote.message.MessageChatDTO;
import com.socialchat.model.remote.message.MessageCommentDTO;

public interface MessageRemoteService {

    /**
     * 发送评论消息
     *
     * @param messageCommentDTO
     */
    void sendCommentMessage(MessageCommentDTO messageCommentDTO);

    /**
     * 发送聊天（单聊）消息
     *
     * @param messageChatDTO
     */
    void sendChatMessage(MessageChatDTO messageChatDTO);
}
