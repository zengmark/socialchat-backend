package com.socialchat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.PageRequest;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.CommentCountMapper;
import com.socialchat.dao.CommentMapper;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.entity.CommentCount;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.CommentVO;
import com.socialchat.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * (tb_comment)表服务实现类
 *
 * @author makejava
 * @since 2024-12-31 17:38:05
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private CommentCountMapper commentCountMapper;

    @Override
    public boolean addComment(CommentAddRequest request) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(request, comment);

        // 评论记录表添加评论记录
        int insert = commentMapper.insert(comment);

        // 评论计数表更新评论计数
        LambdaQueryWrapper<CommentCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentCount::getTargetId, request.getTargetId());
        queryWrapper.eq(CommentCount::getTargetType, request.getTargetType());
        CommentCount commentCount = commentCountMapper.selectOne(queryWrapper);
        if (commentCount == null) {
            commentCount = new CommentCount();
            commentCount.setTargetId(request.getTargetId());
            commentCount.setTargetType(request.getTargetType());
            commentCount.setUserId(request.getUserId());
            commentCount.setCommentNum(0);
            commentCountMapper.insert(commentCount);
        }
        commentCount.setCommentNum(commentCount.getCommentNum() + 1);
        commentCountMapper.updateById(commentCount);

        // todo：后期引入消息表后要加事物控制并插入通知信息，并且使用 SSE 作服务端消息推送

        return insert > 0;
    }

    @Override
    public boolean deleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        Long targetId = comment.getTargetId();
        Integer targetType = comment.getTargetType();
        // 评论记录表删除评论记录
        int delete = commentMapper.deleteById(commentId);

        // 评论计数表更新评论计数
        LambdaQueryWrapper<CommentCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentCount::getTargetId, targetId);
        queryWrapper.eq(CommentCount::getTargetType, targetType);
        CommentCount commentCount = commentCountMapper.selectOne(queryWrapper);
        if (commentCount != null) {
            commentCount.setCommentNum(commentCount.getCommentNum() - 1);
        }

        // todo：引入消息表，删除未读消息数据

        return delete > 0;
    }

    @Override
    public Page<CommentVO> listOwnCommentHistory(PageRequest pageRequest, HttpServletRequest request) {
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);
        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        if (ObjectUtil.isNull(userSession)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户未登录");
        }

        Long userId = userSession.getId();
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();

        // todo：待定返回数据结构，因此先不做这里的功能
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getUserId, userId);


        return null;
    }
}

