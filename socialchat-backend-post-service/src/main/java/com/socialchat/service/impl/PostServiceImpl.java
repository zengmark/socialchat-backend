package com.socialchat.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.api.UserRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.dao.PostMapper;
import com.socialchat.dao.PostTagRelationMapper;
import com.socialchat.dao.VoteMapper;
import com.socialchat.es.document.PostDocument;
import com.socialchat.es.repository.PostDocumentRepository;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.dto.Record;
import com.socialchat.model.entity.Post;
import com.socialchat.model.entity.PostTagRelation;
import com.socialchat.model.entity.Tag;
import com.socialchat.model.entity.Vote;
import com.socialchat.model.remote.user.UserDTO;
import com.socialchat.model.request.PostOwnRequest;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostUpdateRequest;
import com.socialchat.model.vo.PostVO;
import com.socialchat.service.PostService;
import com.socialchat.service.PostTagRelationService;
import com.socialchat.service.TagService;
import com.socialchat.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    @DubboReference
    private UserRemoteService userRemoteService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private VoteService voteService;

    @Resource
    private PostTagRelationMapper postTagRelationMapper;

    @Resource
    private PostDocumentRepository postDocumentRepository;

    @Resource
    private VoteMapper voteMapper;

    @Resource
    private TagService tagService;

    @Resource
    private PostTagRelationService postTagRelationService;

    private static int titleNumber = 1;

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
    public boolean savePostBySchedule(List<Record> recordList) {
        log.info("保存数据到数据库和 ES 的 record 数据为:{}", recordList);
        for (Record record : recordList) {
            // 1、保存用户数据
            Record.User user = record.getUser();

            UserDTO userDTO = new UserDTO();
            userDTO.setUserName(user.getUserName());
            userDTO.setUserAccount(user.getId());
            userDTO.setUserPassword("123456");
            userDTO.setUserEmail(user.getId() + "@qq.com");
            userDTO.setUserAvatar(user.getUserAvatar());
            userDTO.setUserProfile(user.getUserProfile());

            Long userId = userRemoteService.saveUser(userDTO);
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // 2、保存帖子数据，根据 record 的 id 做去重
            String sourceId = record.getId();
            LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Post::getSourceId, sourceId);
            Post sourcePost = postMapper.selectOne(queryWrapper);
            if (sourcePost != null) {
                return false;
            }
            String postTitle = StringUtils.isBlank(record.getTitle()) ? ("默认标题" + titleNumber++) : record.getTitle();
            String postContent = record.getContent();
            List<String> pictureList = record.getPictureList();
            Post post = new Post();
            post.setUserId(userId);
            post.setSourceId(sourceId);
            post.setPostTitle(postTitle);
            post.setPostContent(postContent);
            post.setPostPictureList(pictureList);
            post.setVisible(0);
            postMapper.insert(post);

            // 3、保存 tag 标签数据
            List<String> tagNameList = record.getTags();
            List<Tag> tagList = tagNameList.stream().map(item -> {
                Tag tag = new Tag();
                tag.setTagName(item);
                return tag;
            }).collect(Collectors.toList());
            tagService.saveBatch(tagList);

            // 4、保存帖子标签关联表
            Long postId = post.getId();
            List<PostTagRelation> postTagRelationList = tagList.stream().map(item -> {
                PostTagRelation postTagRelation = new PostTagRelation();
                postTagRelation.setPostId(postId);
                postTagRelation.setTagId(item.getId());
                return postTagRelation;
            }).collect(Collectors.toList());
            postTagRelationService.saveBatch(postTagRelationList);

            // 5、保存帖子数据到 ES
            PostSaveRequest postSaveRequest = new PostSaveRequest();
            postSaveRequest.setUserId(userId);
            postSaveRequest.setPostTitle(postTitle);
            postSaveRequest.setPostContent(postContent);
            postSaveRequest.setPostPictureList(pictureList);
            postSaveRequest.setUserAtList(null);
            postSaveRequest.setVisible(0);
            postSaveRequest.setVoteRequest(null);

            savePostToEs(postSaveRequest);

        }
        return true;
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

    @Override
    public Page<PostVO> listOwnPosts(PostOwnRequest request) {
        Long userId = request.getUserId();
        Integer visible = request.getVisible();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();

        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Post::getUserId, userId);
        queryWrapper.eq(visible != null, Post::getVisible, visible);
        Page<Post> postPage = postMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<Post> postList = postPage.getRecords();
        List<PostVO> postVOList = postList.stream().map(post -> {
            PostVO postVO = new PostVO();
            postVO.setId(post.getId());
            postVO.setUserId(post.getUserId());
            postVO.setPostTitle(post.getPostTitle());
            postVO.setPostContent(post.getPostContent());
            postVO.setLikeNum(post.getLikeNum());
            postVO.setCommentNum(post.getCommentNum());
            postVO.setCollectNum(post.getCollectNum());
            postVO.setVisible(post.getVisible());
            postVO.setCreateTime(post.getCreateTime());
            postVO.setUpdateTime(post.getUpdateTime());
            postVO.setPostPictures(post.getPostPictureList());
            postVO.setUserAt(post.getUserAtList());
            return postVO;
        }).collect(Collectors.toList());
        Page<PostVO> postVOPage = new Page<>(current, pageSize);
        postVOPage.setRecords(postVOList);

        return postVOPage;
    }

    // todo：保存到 ES，后期需要确定逻辑，目前只是一个架子，而且 Post 和 PostDocument 中的字段类型并不完全匹配
    //  （userAtList 和 pictureList），要记得做转换
    private void savePostToEs(PostSaveRequest request) {
        PostDocument postDocument = new PostDocument();
        BeanUtils.copyProperties(request, postDocument);
        postDocument.setPostPictures(request.getPostPictureList());
        postDocument.setUserAt(request.getUserAtList());
        postDocument.setCreateTime(new Date());
        postDocument.setUpdateTime(new Date());

        postDocumentRepository.save(postDocument);
    }

    // todo：更新到 ES
    private void updatePostToEs(PostUpdateRequest request) {

    }

    // todo：删除 ES 中的帖子数据
    private void deletePostToEs(Long postId) {

    }
}

