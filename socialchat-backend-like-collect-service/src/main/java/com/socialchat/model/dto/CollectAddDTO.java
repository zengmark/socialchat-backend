package com.socialchat.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CollectAddDTO implements Serializable {

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

    /**
     * 用于判断是否重复收藏消息
     */
    private Date createTime;
}
