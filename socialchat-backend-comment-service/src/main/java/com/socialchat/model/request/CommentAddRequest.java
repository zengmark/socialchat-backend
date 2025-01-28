package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String userAvatar;

    private Long postId;

    private Long parentId;

    private Integer targetType;

    private Long targetId;

    private String commentContent;

    private Long targetUserId;

    private String targetUserName;

    private String targetUserAvatar;
}
