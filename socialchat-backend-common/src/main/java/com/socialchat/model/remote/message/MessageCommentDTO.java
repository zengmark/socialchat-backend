package com.socialchat.model.remote.message;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageCommentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long targetId;

    private Long sourceUserId;

    private Long acceptUserId;

    /**
     * 0 是新增，1 是删除
     */
    private Integer commentAction;
}
