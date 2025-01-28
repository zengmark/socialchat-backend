package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PostUpdateRequest extends PostSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long postId;

}
