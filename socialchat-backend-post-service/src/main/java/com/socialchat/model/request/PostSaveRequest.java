package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String postTitle;

    private String postContent;

    private List<String> postPictureList;

    private List<Long> userAtList;

    /**
     * 是否可见
     * 0 所有人可见
     * 1 草稿态
     * 2 隐藏
     */
    private Integer visible;

    private VoteRequest voteRequest;

    /**
     * 投票功能项
     */
    @Data
    public static class VoteRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean hasVote;

        private String voteTitle;

        private List<String> voteItemList;
    }
}
