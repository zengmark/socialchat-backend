package com.socialchat.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.model.remote.comment.CommentPostDTO;

public interface CommentRemoteService {

    /**
     * 获取帖子下的评论数据
     *
     * @param postId
     * @param current
     * @param pageSize
     * @return
     */
    Page<CommentPostDTO> listCommentUnderPost(Long postId, Long current, Long pageSize);

    /**
     * 获取评论下的评论数据
     *
     * @param commentId
     * @param current
     * @param pageSize
     * @return
     */
    Page<CommentPostDTO> listCommentUnderComment(Long commentId, Long current, Long pageSize);
}