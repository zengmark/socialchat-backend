package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CollectAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long targetId;

    private Integer targetType;

    /**
     * 收藏用户ID
     */
    private Long collectUserId;

    private Integer collectAction;

    /**
     * 被收藏帖子/聊天室的用户ID
     */
    private Long userId;

}
