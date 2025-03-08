package com.socialchat.model.websocket;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessagePayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，0 是单聊，1 是群聊
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 目标ID，单聊就是目标用户ID，群聊就是群聊ID
     */
    private Long targetId;

    /**
     * 接受者ID，如果是群聊为-1
     */
    private Long receiverId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    public ChatMessagePayload(Long senderId, String senderName, String senderAvatar, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.content = content;
    }
}
