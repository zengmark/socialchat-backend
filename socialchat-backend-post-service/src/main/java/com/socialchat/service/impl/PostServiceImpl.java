package com.socialchat.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.PostMapper;
import com.socialchat.es.document.PostDocument;
import com.socialchat.es.repository.PostDocumentRepository;
import com.socialchat.model.entity.Post;
import com.socialchat.model.entity.Vote;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.service.PostService;
import com.socialchat.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    @Resource
    private PostDocumentRepository postDocumentRepository;

    @Transactional
    @Override
    public boolean savePost(PostSaveRequest request) {
        log.info("保存的帖子内容为{}", JSON.toJSONString(request));
        Long userId = request.getUserId();
        String postTitle = request.getPostTitle();
        String postContent = request.getPostContent();
        List<String> postPictureList = request.getPostPictureList();
        List<Long> userAtList = CollectionUtils.isEmpty(request.getUserAtList()) ? new ArrayList<>() : request.getUserAtList();
        Integer visible = request.getVisible();
        PostSaveRequest.VoteRequest voteRequest = request.getVoteRequest();

        Post post = new Post();
        post.setUserId(userId);
        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setVisible(visible);
        post.setUserAtList(userAtList);
        int insert = postMapper.insert(post);

        if (ObjectUtil.isNotNull(voteRequest) && voteRequest.getHasVote()) {
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

        // todo：如果是所有人可见，即 visible 为 0 的时候，需要同步数据到 es 中


        return insert > 0;
    }

    // todo：保存到 ES，后期需要确定逻辑，目前只是一个架子
    private void savePostToEs(PostSaveRequest request) {
        PostDocument postDocument = new PostDocument();
        BeanUtils.copyProperties(request, postDocument);

        postDocumentRepository.save(postDocument);
    }
}

