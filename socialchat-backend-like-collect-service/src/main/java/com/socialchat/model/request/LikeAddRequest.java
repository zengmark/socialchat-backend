package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long targetId;

    private Integer targetType;

    /**
     * 点赞用户ID
     */
    private Long likeUserId;

    private Integer likeAction;

    /**
     * 被点赞帖子/评论的用户ID
     */
    private Long userId;

}
