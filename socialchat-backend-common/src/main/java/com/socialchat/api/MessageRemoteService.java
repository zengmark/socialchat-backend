package com.socialchat.api;

import com.socialchat.model.remote.message.MessageCommentDTO;

public interface MessageRemoteService {

    /**
     * 发送评论消息
     *
     * @param messageCommentDTO
     */
    void sendCommentMessage(MessageCommentDTO messageCommentDTO);

}
