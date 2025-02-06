package com.socialchat.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PostCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private PostVO postVO;

    private List<PostComment> commentList;

    private Boolean liked = false;

    private Boolean collected = false;

    @Data
    public static class PostComment implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long commentId;

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

        private Date createTime;

        private Boolean liked = false;

        private Integer likeNum;

        private List<PostComment> innerCommentList;
    }

}
