package com.socialchat.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

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

    private Integer likeNum;

    private Date createTime;

    private List<CommentVO> innerCommentList;

}
