package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.CommentRemoteService;
import com.socialchat.api.LikeRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.CommentConstant;
import com.socialchat.dao.CommentCountMapper;
import com.socialchat.dao.CommentMapper;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.entity.CommentCount;
import com.socialchat.model.remote.comment.CommentPostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@DubboService
@Slf4j
public class CommentRemoteServiceImpl implements CommentRemoteService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private CommentCountMapper commentCountMapper;

    @DubboReference
    private LikeRemoteService likeRemoteService;

    @Override
    public Page<CommentPostDTO> listCommentUnderPost(Long postId, Long current, Long pageSize) {
        log.info("获取的帖子ID为{}，页码为{}, 页大小为{}", postId, current, pageSize);

        // 1、根据 postId 获取评论数据
        LambdaQueryWrapper<Comment> outsideQueryWrapper = new LambdaQueryWrapper<>();
        outsideQueryWrapper.eq(Comment::getTargetId, postId);
        outsideQueryWrapper.eq(Comment::getTargetType, CommentConstant.POST);
        outsideQueryWrapper.orderByDesc(Comment::getCreateTime);

        // 2、获取帖子下的外层评论
        Page<Comment> commentOutsidePage = commentMapper.selectPage(new Page<>(current, pageSize), outsideQueryWrapper);
        List<Comment> commentOutsideList = CollectionUtils.isNotEmpty(commentOutsidePage.getRecords()) ? commentOutsidePage.getRecords() : new ArrayList<>();

        // 3、组装外层每个评论的内层评论
        List<CommentPostDTO> outsideCommentPostDTOList = commentOutsideList.stream()
                .map(comment -> {
                    CommentPostDTO commentPostDTO = new CommentPostDTO();
                    BeanUtils.copyProperties(comment, commentPostDTO);
                    // 点赞数字段设置
                    Integer outsideLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(comment.getId(), CommentConstant.COMMENT);
                    commentPostDTO.setLikeNum(outsideLikeNum);

                    // 组装内层评论
                    Long commentId = comment.getId();
                    LambdaQueryWrapper<Comment> insideQueryWrapper = new LambdaQueryWrapper<>();
                    insideQueryWrapper.eq(Comment::getParentId, commentId);
                    insideQueryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);

                    Page<Comment> commentInsidePage = commentMapper.selectPage(new Page<>(1, 10), insideQueryWrapper);
                    List<Comment> commentInsideList = CollectionUtils.isNotEmpty(commentInsidePage.getRecords()) ? commentInsidePage.getRecords() : new ArrayList<>();
                    List<CommentPostDTO> insideCommentPostDTOList = commentInsideList.parallelStream()
                            .map(insideComment -> {
                                CommentPostDTO insideCommentPostDTO = new CommentPostDTO();
                                BeanUtils.copyProperties(insideComment, insideCommentPostDTO);
                                // 点赞数字段设置
                                Integer insideLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(insideComment.getId(), CommentConstant.COMMENT);
                                commentPostDTO.setLikeNum(insideLikeNum);
                                return insideCommentPostDTO;
                            }).collect(Collectors.toList());
                    commentPostDTO.setBestCommentData(insideCommentPostDTOList);
                    return commentPostDTO;
                }).collect(Collectors.toList());
        Page<CommentPostDTO> outsideCommentPostDTOPage = new Page<>(current, pageSize);
        outsideCommentPostDTOPage.setRecords(outsideCommentPostDTOList);

        return outsideCommentPostDTOPage;
    }

    @Override
    public Page<CommentPostDTO> listCommentUnderComment(Long commentId, Long current, Long pageSize) {
        log.info("获取的评论ID为{}，页码为{}, 页大小为{}", commentId, current, pageSize);

        // 根据 commentId 获取这个评论下的评论数据
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, commentId);
        queryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);
        queryWrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<Comment> commentList = CollectionUtils.isNotEmpty(commentPage.getRecords()) ? commentPage.getRecords() : new ArrayList<>();
        List<CommentPostDTO> commentPostDTOList = commentList.parallelStream()
                .map(comment -> {
                    CommentPostDTO commentPostDTO = new CommentPostDTO();
                    BeanUtils.copyProperties(comment, commentPostDTO);
                    // 设置点赞数
                    Integer likeNum = likeRemoteService.countLikeByTargetIdAndTargetType(comment.getId(), CommentConstant.COMMENT);
                    commentPostDTO.setLikeNum(likeNum);
                    return commentPostDTO;
                }).collect(Collectors.toList());
        Page<CommentPostDTO> commentPostDTOPage = new Page<>(current, pageSize);
        commentPostDTOPage.setRecords(commentPostDTOList);
        return commentPostDTOPage;
    }

    @Override
    public Integer countCommentByPostId(Long postId) {
        log.info("获取帖子ID为{}下的评论数", postId);

        LambdaQueryWrapper<CommentCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CommentCount::getTargetId, postId);
        queryWrapper.eq(CommentCount::getTargetType, CommentConstant.POST);
        CommentCount commentCount = commentCountMapper.selectOne(queryWrapper);
        if (commentCount == null) {
            return 0;
        }

        return commentCount.getCommentNum();
    }

    @Override
    public String getCommentContentById(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论不存在");
        }
        return comment.getCommentContent();
    }
}
