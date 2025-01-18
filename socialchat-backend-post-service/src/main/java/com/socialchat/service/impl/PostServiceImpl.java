package com.socialchat.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.common.ErrorCode;
import com.socialchat.dao.PostMapper;
import com.socialchat.dao.PostTagRelationMapper;
import com.socialchat.dao.TagMapper;
import com.socialchat.dao.VoteMapper;
import com.socialchat.es.document.PostDocument;
import com.socialchat.es.repository.PostDocumentRepository;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.Post;
import com.socialchat.model.entity.PostTagRelation;
import com.socialchat.model.entity.Vote;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostUpdateRequest;
import com.socialchat.service.PostService;
import com.socialchat.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    private PostTagRelationMapper postTagRelationMapper;

    @Resource
    private PostDocumentRepository postDocumentRepository;
    @Autowired
    private VoteMapper voteMapper;

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

        // 1、插入帖子
        Post post = new Post();
        post.setUserId(userId);
        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setVisible(visible);
        post.setUserAtList(userAtList);
        int insert = postMapper.insert(post);

        // 2、插入投票项
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

        // 3、插入帖子标签关联


        // todo：如果是所有人可见，即 visible 为 0 的时候，需要同步数据到 es 中


        return insert > 0;
    }

    @Transactional
    @Override
    public boolean updatePost(PostUpdateRequest request) {
        log.info("更新的帖子内容为{}", JSON.toJSONString(request));

        Long postId = request.getPostId();
        Post post = postMapper.selectById(postId);
        if (ObjectUtil.isNull(post)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新的帖子不存在");
        }

        Long userId = request.getUserId();
        String postTitle = request.getPostTitle();
        String postContent = request.getPostContent();
        List<String> postPictureList = request.getPostPictureList();
        List<Long> userAtList = request.getUserAtList();
        Integer visible = request.getVisible();
        PostSaveRequest.VoteRequest voteRequest = request.getVoteRequest();

        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setUserAtList(userAtList);
        post.setVisible(visible);
        postMapper.updateById(post);

        // 先删除过去的 vote 投票项
        LambdaQueryWrapper<Vote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Vote::getPostId, postId);
        voteService.remove(queryWrapper);

        // 更新投票项
        if (ObjectUtil.isNotNull(voteRequest) && voteRequest.getHasVote()) {
            String voteTitle = voteRequest.getVoteTitle();
            List<String> voteItemList = voteRequest.getVoteItemList();

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

        // 更新 ES 中的帖子数据

        return true;
    }

    @Transactional
    @Override
    public boolean deletePost(Long postId) {
        // 1、删除帖子
        int delete = postMapper.deleteById(postId);

        // 2、删除投票项
        LambdaQueryWrapper<Vote> voteQueryWrapper = new LambdaQueryWrapper<>();
        voteQueryWrapper.eq(Vote::getPostId, postId);
        voteMapper.delete(voteQueryWrapper);

        // 3、删除帖子和标签绑定关系
        LambdaQueryWrapper<PostTagRelation> postTagRelationQueryWrapper = new LambdaQueryWrapper<>();
        postTagRelationQueryWrapper.eq(PostTagRelation::getPostId, postId);
        postTagRelationMapper.delete(postTagRelationQueryWrapper);

        // todo：删除 ES 中的帖子数据

        return delete > 0;
    }

    // todo：保存到 ES，后期需要确定逻辑，目前只是一个架子，而且 Post 和 PostDocument 中的字段类型并不完全匹配
    //  （userAtList 和 pictureList），要记得做转换
    private void savePostToEs(PostSaveRequest request) {
        PostDocument postDocument = new PostDocument();
        BeanUtils.copyProperties(request, postDocument);

        postDocumentRepository.save(postDocument);
    }

    // todo：更新到 ES
    private void updatePostToEs(PostUpdateRequest request) {

    }

    // todo：删除 ES 中的帖子数据
    private void deletePostToEs(Long postId) {

    }
}

