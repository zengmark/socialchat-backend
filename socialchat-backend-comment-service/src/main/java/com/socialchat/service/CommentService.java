package com.socialchat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.common.PageRequest;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.model.vo.CommentVO;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * 删除评论
     *
     * @param commentId
     * @return
     */
    boolean deleteComment(Long commentId);

    /**
     * 查看自己历史评论
     *
     * @param pageRequest
     * @param request
     * @return
     */
    Page<CommentVO> listOwnCommentHistory(PageRequest pageRequest, HttpServletRequest request);
}

