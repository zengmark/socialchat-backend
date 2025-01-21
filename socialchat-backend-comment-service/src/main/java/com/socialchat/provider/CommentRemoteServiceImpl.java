package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.CommentRemoteService;
import com.socialchat.constant.CommentConstant;
import com.socialchat.dao.CommentMapper;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.remote.comment.CommentPostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
                    // todo：注意点赞字段设置
                    BeanUtils.copyProperties(comment, commentPostDTO);

                    // todo：从点赞表计数表里面取数据，组装内层热门评论（只取前两个热门评论）
                    Long commentId = commentPostDTO.getId();
//                    LambdaQueryWrapper<Comment> insideQueryWrapper = new LambdaQueryWrapper<>();
//                    insideQueryWrapper.eq(Comment::getTargetId, commentId);
//                    insideQueryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);
//
//                    Page<Comment> commentInsidePage = commentMapper.selectPage(new Page<>(CommentConstant.HOT_START, CommentConstant.HOT_END), insideQueryWrapper);
//                    List<Comment> commentInsideList = CollectionUtils.isNotEmpty(commentInsidePage.getRecords()) ? commentInsidePage.getRecords() : new ArrayList<>();
//                    List<CommentPostDTO> insideCommentPostDTOList = commentInsideList.stream()
//                            .map(insideComment -> {
//                                CommentPostDTO insideCommentPostDTO = new CommentPostDTO();
//                                BeanUtils.copyProperties(insideComment, insideCommentPostDTO);
//                                return insideCommentPostDTO;
//                            }).collect(Collectors.toList());
//                    commentPostDTO.setBestCommentData(insideCommentPostDTOList);
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
        queryWrapper.eq(Comment::getTargetId, commentId);
        queryWrapper.eq(Comment::getTargetType, CommentConstant.COMMENT);
        queryWrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> commentPage = commentMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<Comment> commentList = CollectionUtils.isNotEmpty(commentPage.getRecords()) ? commentPage.getRecords() : new ArrayList<>();
        List<CommentPostDTO> commentPostDTOList = commentList.stream()
                .map(comment -> {
                    // todo：注意点赞数设置
                    CommentPostDTO commentPostDTO = new CommentPostDTO();
                    BeanUtils.copyProperties(comment, commentPostDTO);
                    return commentPostDTO;
                }).collect(Collectors.toList());
        Page<CommentPostDTO> commentPostDTOPage = new Page<>(current, pageSize);
        commentPostDTOPage.setRecords(commentPostDTOList);
        return commentPostDTOPage;
    }
}
