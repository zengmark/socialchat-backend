package com.socialchat.model.request;

import com.socialchat.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommentPageRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 查询评论的父级ID
     */
    private Long parentId;

}
