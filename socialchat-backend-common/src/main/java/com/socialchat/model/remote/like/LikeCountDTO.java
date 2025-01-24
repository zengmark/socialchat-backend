package com.socialchat.model.remote.like;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long postId;

    private Integer likeNum;

}
