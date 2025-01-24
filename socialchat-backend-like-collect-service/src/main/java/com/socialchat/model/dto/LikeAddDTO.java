package com.socialchat.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class LikeAddDTO implements Serializable {

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

    /**
     * 用于判断是否重复点赞消息
     */
    private Date createTime;
}
