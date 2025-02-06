package com.socialchat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.api.LikeRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.PageRequest;
import com.socialchat.constant.CommentConstant;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.CommentCountMapper;
import com.socialchat.dao.CommentMapper;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.entity.CommentCount;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.model.request.CommentPageRequest;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.CommentVO;
import com.socialchat.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private LikeRemoteService likeRemoteService;

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
    public Page<CommentVO> listCommentByPostId(CommentPageRequest request) {
        Long parentId = request.getParentId();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();

        LambdaQueryWrapper<Comment> outsideQueryWrapper = new LambdaQueryWrapper<>();
        outsideQueryWrapper.eq(Comment::getTargetId, parentId);
        outsideQueryWrapper.eq(Comment::getTargetType, CommentConstant.POST);
        Page<Comment> outsideCommentPage = commentMapper.selectPage(new Page<>(current, pageSize), outsideQueryWrapper);
        List<Comment> outsideCommentList = outsideCommentPage.getRecords();
        // 构造外层评论
        List<CommentVO> outsideCommentVOList = outsideCommentList.stream().map(outsideComment -> {
            CommentVO outsideCommentVO = new CommentVO();
            BeanUtils.copyProperties(outsideComment, outsideCommentVO);
            Integer outsideLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(outsideComment.getId(), CommentConstant.COMMENT);
            outsideCommentVO.setLikeNum(outsideLikeNum);

            // 构造内层评论
            LambdaQueryWrapper<Comment> innerQueryWrapper = new LambdaQueryWrapper<>();
            innerQueryWrapper.eq(Comment::getParentId, outsideComment.getId());
            innerQueryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);
            Page<Comment> innerCommentPage = commentMapper.selectPage(new Page<>(1L, 10L), innerQueryWrapper);
            List<Comment> innerCommentList = innerCommentPage.getRecords();
            List<CommentVO> innerCommentVOList = innerCommentList.stream().map(innerComment -> {
                CommentVO innerCommentVO = new CommentVO();
                BeanUtils.copyProperties(innerComment, innerCommentVO);
                Integer innerLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(innerComment.getId(), CommentConstant.COMMENT);
                innerCommentVO.setLikeNum(innerLikeNum);
                return innerCommentVO;
            }).collect(Collectors.toList());

            outsideCommentVO.setInnerCommentList(innerCommentVOList);
            return outsideCommentVO;
        }).collect(Collectors.toList());

        Page<CommentVO> commentVOPage = new Page<>(current, pageSize);
        commentVOPage.setRecords(outsideCommentVOList);
        commentVOPage.setTotal(outsideCommentPage.getTotal());
        return commentVOPage;
    }

    @Override
    public CommentVO listCommentByCommentId(CommentPageRequest request) {
        Long parentId = request.getParentId();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();

        Comment parentComment = commentMapper.selectById(parentId);
        if (parentComment == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论已不存在");
        }

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, parentId);
        queryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);
        Page<Comment> innerCommentPage = commentMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<Comment> innerCommentList = innerCommentPage.getRecords();

        CommentVO outsideCommentVO = new CommentVO();
        BeanUtils.copyProperties(parentComment, outsideCommentVO);
        Integer parentLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(parentId, CommentConstant.COMMENT);
        outsideCommentVO.setLikeNum(parentLikeNum);
        List<CommentVO> innerCommentVOList = innerCommentList.stream().map(innerComment -> {
            CommentVO innerCommentVO = new CommentVO();
            BeanUtils.copyProperties(innerComment, innerCommentVO);
            Integer innerLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(innerComment.getId(), CommentConstant.COMMENT);
            innerCommentVO.setLikeNum(innerLikeNum);
            return innerCommentVO;
        }).collect(Collectors.toList());
        outsideCommentVO.setInnerCommentList(innerCommentVOList);
        return outsideCommentVO;
    }

//    @Override
//    public Page<CommentVO> listOwnCommentHistory(PageRequest pageRequest, HttpServletRequest request) {
//        String header = request.getHeader(UserConstant.AUTHORIZATION);
//        String token = header.substring(7);
//        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
//        if (ObjectUtil.isNull(userSession)) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户未登录");
//        }
//
//        Long userId = userSession.getId();
//        int current = pageRequest.getCurrent();
//        int pageSize = pageRequest.getPageSize();
//
//        // todo：待定返回数据结构，因此先不做这里的功能
//        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Comment::getUserId, userId);
//
//
//        return null;
//    }

}

