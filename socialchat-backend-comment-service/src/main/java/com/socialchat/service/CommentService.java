package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.request.CommentAddRequest;

/**
 * (tb_comment)表服务接口
 *
 * @author makejava
 * @since 2024-12-31 17:38:04
 */
public interface CommentService extends IService<Comment> {

    /**
     * 添加评论
     *
     * @param request
     * @return
     */
    boolean addComment(CommentAddRequest request);
}

