package com.socialchat.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.PostMapper;
import com.socialchat.model.entity.Post;
import com.socialchat.model.entity.Vote;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.service.PostService;
import com.socialchat.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (tb_post)表服务实现类
 *
 * @author makejava
 * @since 2024-12-24 22:05:42
 */
@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private PostMapper postMapper;

    @Resource
    private VoteService voteService;

    @Transactional
    @Override
    public boolean savePost(PostSaveRequest request) {
        log.info("保存的帖子内容为{}", JSON.toJSONString(request));
        Long userId = request.getUserId();
        String postTitle = request.getPostTitle();
        String postContent = request.getPostContent();
        List<String> postPictureList = request.getPostPictureList();
        List<Long> userAtList = request.getUserAtList();
        Integer visible = request.getVisible();
        PostSaveRequest.VoteRequest voteRequest = request.getVoteRequest();

        Post post = new Post();
        post.setUserId(userId);
        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setUserAtList(userAtList);
        post.setVisible(visible);
        int insert = postMapper.insert(post);

        if (ObjectUtil.isNotNull(voteRequest)) {
            String voteTitle = voteRequest.getVoteTitle();
            List<String> voteItemList = voteRequest.getVoteItemList();
            Long postId = post.getId();

            List<Vote> voteList = voteItemList.stream().map(content -> {
                Vote vote = new Vote();
                vote.setUserId(userId);
                vote.setPostId(postId);
                vote.setVoteTitle(voteTitle);
                vote.setVoteContent(content);
                return vote;
            }).collect(Collectors.toList());

            voteService.saveBatch(voteList);
        }

        return insert > 0;
    }
}

