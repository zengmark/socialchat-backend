package com.socialchat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.api.CollectRemoteService;
import com.socialchat.api.CommentRemoteService;
import com.socialchat.api.LikeRemoteService;
import com.socialchat.api.UserRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.PageRequest;
import com.socialchat.constant.CollectConstant;
import com.socialchat.constant.LikeConstant;
import com.socialchat.constant.PostConstant;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.PostMapper;
import com.socialchat.dao.PostTagRelationMapper;
import com.socialchat.dao.TagMapper;
import com.socialchat.dao.VoteMapper;
import com.socialchat.es.document.PostDocument;
import com.socialchat.es.repository.PostDocumentRepository;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.dto.Record;
import com.socialchat.model.entity.Post;
import com.socialchat.model.entity.PostTagRelation;
import com.socialchat.model.entity.Tag;
import com.socialchat.model.entity.Vote;
import com.socialchat.model.remote.comment.CommentPostDTO;
import com.socialchat.model.remote.user.UserDTO;
import com.socialchat.model.request.PostOwnRequest;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostSearchRequest;
import com.socialchat.model.request.PostUpdateRequest;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.PostCommentVO;
import com.socialchat.model.vo.PostSearchPageVO;
import com.socialchat.model.vo.PostVO;
import com.socialchat.service.PostService;
import com.socialchat.service.PostTagRelationService;
import com.socialchat.service.TagService;
import com.socialchat.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.script.Script;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @DubboReference
    private CommentRemoteService commentRemoteService;

    @DubboReference
    private LikeRemoteService likeRemoteService;

    @DubboReference
    private CollectRemoteService collectRemoteService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private VoteService voteService;

    @Resource
    private PostTagRelationMapper postTagRelationMapper;

    @Resource
    private PostDocumentRepository postDocumentRepository;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private VoteMapper voteMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private TagService tagService;

    @Resource
    private PostTagRelationService postTagRelationService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
        List<Long> tagIds = request.getTagIds();

        // 1、插入帖子
        Post post = new Post();
        post.setUserId(userId);
        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setVisible(visible);
        post.setUserAtList(userAtList);
        int insert = postMapper.insert(post);
        Long postId = post.getId();

        // 2、插入投票项
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

        // 3、插入帖子标签关联
        if (CollectionUtils.isNotEmpty(tagIds)) {
            List<PostTagRelation> postTagRelationList = tagIds.stream().map(tagId -> {
                PostTagRelation postTagRelation = new PostTagRelation();
                postTagRelation.setPostId(postId);
                postTagRelation.setTagId(tagId);
                return postTagRelation;
            }).collect(Collectors.toList());
            postTagRelationService.saveBatch(postTagRelationList);
        }

        // 4、如果是所有人可见，即 visible 为 0 的时候，需要同步数据到 es 中
        if (PostConstant.ALL_PEOPLE.equals(visible)) {
            savePostToEs(request, postId);
        }

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
            Long postId = post.getId();

            // 3、保存 tag 标签数据
            List<String> tagNameList = record.getTags();
            List<Tag> tagList = tagNameList.stream().map(item -> {
                Tag tag = new Tag();
                tag.setTagName(item);
                return tag;
            }).collect(Collectors.toList());
            tagService.saveBatch(tagList);

            // 4、保存帖子标签关联表
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
            postSaveRequest.setTags(tagNameList);
            postSaveRequest.setVoteRequest(null);

            savePostToEs(postSaveRequest, postId);

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
        List<Long> tagIds = request.getTagIds();

        // 1、更新数据库中的帖子数据
        post.setPostTitle(postTitle);
        post.setPostContent(postContent);
        post.setPostPictureList(postPictureList);
        post.setUserAtList(userAtList);
        post.setVisible(visible);
        postMapper.updateById(post);

        // 2、更新数据库中的投票项数据
        // 先删除过去的 vote 投票项
        LambdaQueryWrapper<Vote> voteLambdaQueryWrapper = new LambdaQueryWrapper<>();
        voteLambdaQueryWrapper.eq(Vote::getPostId, postId);
        voteService.remove(voteLambdaQueryWrapper);

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

        // 3、更新数据库中的帖子标签关联项数据
        // 删除帖子标签关联项
        LambdaQueryWrapper<PostTagRelation> postTagRelationLambdaQueryWrapper = new LambdaQueryWrapper<>();
        postTagRelationLambdaQueryWrapper.eq(PostTagRelation::getPostId, postId);
        postTagRelationService.remove(postTagRelationLambdaQueryWrapper);

        // 更新帖子标签关联项
        if (CollectionUtils.isNotEmpty(tagIds)) {
            List<PostTagRelation> postTagRelationList = tagIds.stream().map(tagId -> {
                PostTagRelation postTagRelation = new PostTagRelation();
                postTagRelation.setPostId(postId);
                postTagRelation.setTagId(tagId);
                return postTagRelation;
            }).collect(Collectors.toList());
            postTagRelationService.saveBatch(postTagRelationList);
        }

        // 4、更新 es 中的帖子投票项数据
        // 更新 ES 中的帖子数据
        if (PostConstant.ALL_PEOPLE.equals(visible)) {
            // 保存 post 到 ES
            savePostToEs(request, postId);
        } else if (PostConstant.HIDE.equals(visible)) {
            // 删除 es 中对应的 post
            postDocumentRepository.deleteById(String.valueOf(postId));
        }

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

        // 3、删除帖子和标签关联项
        LambdaQueryWrapper<PostTagRelation> postTagRelationQueryWrapper = new LambdaQueryWrapper<>();
        postTagRelationQueryWrapper.eq(PostTagRelation::getPostId, postId);
        postTagRelationMapper.delete(postTagRelationQueryWrapper);

        // 4、删除 ES 中的帖子数据
        postDocumentRepository.deleteById(String.valueOf(postId));

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
        queryWrapper.eq(Post::getVisible, visible);
        Page<Post> postPage = postMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<Post> postList = postPage.getRecords();
        List<PostVO> postVOList = postList.stream().map(this::convertPostToPostVO).collect(Collectors.toList());
        Page<PostVO> postVOPage = new Page<>(current, pageSize);
        postVOPage.setRecords(postVOList);

        return postVOPage;
    }

    @Override
    public PostSearchPageVO listHomePosts(PageRequest request) {
        int current = request.getCurrent();
        int pageSize = request.getPageSize();
        int start = (current - 1) * pageSize;
        String sortField = request.getSortField();

        PostSearchPageVO postSearchPageVO = new PostSearchPageVO();
        postSearchPageVO.setCurrent(current);
        postSearchPageVO.setSize(pageSize);

        // 如果是查找热点数据
        if (PostConstant.HOT.equals(sortField)) {
            // 判断是否超过 es 的上限
            if (start >= PostConstant.LIMIT) {
                // 从点赞服务获取帖子ID
                List<Long> postIdList = likeRemoteService.listPostIdByLikeNum(current, pageSize);

                // 根据帖子ID，从 MySQL 中查询数据
                LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.in(Post::getId, postIdList);
                queryWrapper.eq(Post::getVisible, PostConstant.ALL_PEOPLE);
                queryWrapper.orderByDesc(Post::getCreateTime);
                List<Post> postList = postMapper.selectList(queryWrapper);
                List<PostVO> postVOList = postList.stream().map(this::convertPostToPostVO).collect(Collectors.toList());

                // 查出总数，组装结果
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Post::getVisible, PostConstant.ALL_PEOPLE);
                Long total = postMapper.selectCount(queryWrapper);
                postSearchPageVO.setRecords(postVOList);
                postSearchPageVO.setTotal(total);
            } else {
                // 从 ES 中直接查询帖子数据
                Sort likeSort = Sort.by(Sort.Direction.DESC, LikeConstant.LIKE_NUM);
                org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(current - 1, pageSize, likeSort);
                org.springframework.data.domain.Page<PostDocument> postDocumentPage = postDocumentRepository.findAll(pageRequest);
                List<PostDocument> postDocumentList = postDocumentPage.getContent();
                long total = postDocumentPage.getTotalElements();

                List<PostVO> postVOList = postDocumentList.stream().map(this::convertPostDocumentToPostVO).collect(Collectors.toList());
                postSearchPageVO.setRecords(postVOList);
                postSearchPageVO.setTotal(total);
            }
        } else {
            // 判断是否超过 es 的上限
            if (start >= PostConstant.LIMIT) {
                // 根据帖子ID，从 MySQL 中查询数据
                LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Post::getVisible, PostConstant.ALL_PEOPLE);
                queryWrapper.orderByDesc(Post::getCreateTime);
                List<Post> postList = postMapper.selectList(queryWrapper);
                List<PostVO> postVOList = postList.stream().map(this::convertPostToPostVO).collect(Collectors.toList());

                // 查出总数，组装结果
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Post::getVisible, PostConstant.ALL_PEOPLE);
                Long total = postMapper.selectCount(queryWrapper);
                postSearchPageVO.setRecords(postVOList);
                postSearchPageVO.setTotal(total);
            } else {
                // 从 ES 中直接查询帖子数据
                org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(current - 1, pageSize);
                org.springframework.data.domain.Page<PostDocument> postDocumentPage = postDocumentRepository.findAllByOrderByCreateTimeDesc(pageRequest);
                List<PostDocument> postDocumentList = postDocumentPage.getContent();
                long total = postDocumentPage.getTotalElements();

                List<PostVO> postVOList = postDocumentList.stream().map(this::convertPostDocumentToPostVO).collect(Collectors.toList());
                postSearchPageVO.setRecords(postVOList);
                postSearchPageVO.setTotal(total);
            }
        }
        return postSearchPageVO;
    }

    @Override
    public PostSearchPageVO listSearchPosts(PostSearchRequest request) {
        String searchWord = request.getSearchWord();
        List<String> tagList = request.getTagList();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();
        int start = (current - 1) * pageSize;
        String sortField = request.getSortField();

        if (StringUtils.isBlank(searchWord)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索词不能为空");
        }

        // 判断是否超过 es 的上限
        if (start >= PostConstant.LIMIT) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "您已经滑到底了，无法展示更多数据");
        }

        PostSearchPageVO postSearchPageVO = new PostSearchPageVO();
        postSearchPageVO.setCurrent(current);
        postSearchPageVO.setSize(pageSize);

        // 如果是查找热点数据
        Sort sort;
        if (PostConstant.HOT.equals(sortField)) {
            sort = Sort.by(Sort.Order.desc(LikeConstant.LIKE_NUM), Sort.Order.desc(PostConstant.CREATE_TIME));
        } else {
            sort = Sort.by(Sort.Order.desc(PostConstant.CREATE_TIME));
        }

        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(current - 1, pageSize, sort);
        org.springframework.data.domain.Page<PostDocument> postDocumentPage;
        List<PostDocument> postDocumentList;
        long total;
        // 如果标签项不为空
        if (CollectionUtils.isNotEmpty(tagList)) {
//            postDocumentPage = postDocumentRepository.findByPostTitleContainingOrPostContentContainingAndTagsIn(searchWord, searchWord, tagList, pageRequest);
            PageImpl<PostDocument> postDocumentPageImpl = searchPosts(searchWord, tagList, current, pageSize);
            postDocumentList = postDocumentPageImpl.getContent();
            total = postDocumentPageImpl.getTotalElements();
        } else {
            postDocumentPage = postDocumentRepository.findByPostTitleContainingOrPostContentContaining(searchWord, searchWord, pageRequest);
            postDocumentList = postDocumentPage.getContent();
            total = postDocumentPage.getTotalElements();
        }

//        postDocumentList = postDocumentPage.getContent();
//        long total = postDocumentPage.getTotalElements();
        List<PostVO> postVOList = postDocumentList.stream().map(this::convertPostDocumentToPostVO).collect(Collectors.toList());
        postSearchPageVO.setRecords(postVOList);
        postSearchPageVO.setTotal(total);

        return postSearchPageVO;
    }

    @Override
    public PostCommentVO getPostByPostId(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "帖子不存在");
        }
        PostCommentVO postCommentVO = new PostCommentVO();
        // todo：判断用户是否点过赞、收过藏
        UserSession userSession = null;
        if (StpUtil.isLogin()) {
            userSession = (UserSession) StpUtil.getTokenSession().get(UserConstant.USERINFO);
            Long userId = userSession.getId();
            Boolean liked = likeRemoteService.checkLike(userId, postId, LikeConstant.POST_TYPE);
            Boolean collected = collectRemoteService.checkCollect(userId, postId, CollectConstant.POST_TYPE);
            postCommentVO.setLiked(liked);
            postCommentVO.setCollected(collected);
        }
        PostVO postVO = convertPostToPostVO(post);
        List<PostCommentVO.PostComment> commentList = constructPostCommentList(post, userSession);
        postCommentVO.setPostVO(postVO);
        postCommentVO.setCommentList(commentList);
        return postCommentVO;
    }

    public PageImpl<PostDocument> searchPosts(String keyword, List<String> tagNames, int current, int pageSize) {
        Pageable pageable = Pageable.ofSize(pageSize).withPage(current - 1);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 构建标题和内容的OR条件
        BoolQueryBuilder titleContentQuery = QueryBuilders.boolQuery();
        titleContentQuery.should(QueryBuilders.matchQuery("postTitle", keyword));
        titleContentQuery.should(QueryBuilders.matchQuery("postContent", keyword));
        boolQuery.must(titleContentQuery);

        // 如果tags不为空，添加tags的匹配条件
        if (tagNames != null && !tagNames.isEmpty()) {
            boolQuery.must(QueryBuilders.termsQuery("tags", tagNames));
        }

        NativeSearchQuery searchQuery = new NativeSearchQuery(boolQuery);
        searchQuery.setPageable(pageable);

        SearchHits<PostDocument> searchHits = elasticsearchRestTemplate.search(searchQuery, PostDocument.class);
        List<PostDocument> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(results, pageable, searchHits.getTotalHits());

//        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
//
//        // 构建 must 部分
//        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
//        // 添加 should 条件
//        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
//        shouldQueryBuilder.should(QueryBuilders.matchQuery("postTitle", keyword));
//        shouldQueryBuilder.should(QueryBuilders.matchQuery("postContent", keyword));
//        boolQueryBuilder.must(shouldQueryBuilder);
//
//
//        // 使用脚本查询
//        if (tagNames != null && !tagNames.isEmpty()) {
//            Script script = new Script(
//                    Script.DEFAULT_SCRIPT_TYPE,
//                    Script.DEFAULT_SCRIPT_LANG,
//                    "doc['tags'].stream().anyMatch(s -> params.tagNames.contains(s))",
//                    Collections.singletonMap("tagNames", tagNames)
//            );
//            ScriptQueryBuilder scriptQueryBuilder = QueryBuilders.scriptQuery(script);
//            boolQueryBuilder.must(scriptQueryBuilder);
//
//            Script filterScript = new Script(
//                    Script.DEFAULT_SCRIPT_TYPE,
//                    Script.DEFAULT_SCRIPT_LANG,
//                    "params.tagNames.size() > 0 ? true : doc['tags'].size() > 0",
//                    Collections.singletonMap("tagNames", tagNames)
//            );
//            ScriptQueryBuilder filterQueryBuilder = QueryBuilders.scriptQuery(filterScript);
//            queryBuilder.withFilter(filterQueryBuilder);
//        } else {
//            // 如果没有标签，则不需要过滤，但是为了不报错，添加一个空filter
//            queryBuilder.withFilter(QueryBuilders.existsQuery("tags"));
//        }
//
//        queryBuilder.withQuery(boolQueryBuilder);
//        NativeSearchQuery searchQuery = queryBuilder.withPageable(pageable).build();
//
//        SearchHits<PostDocument> searchHits = elasticsearchRestTemplate.search(searchQuery, PostDocument.class);
//        List<PostDocument> postDocuments = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
//        long totalHits = searchHits.getTotalHits();
//        return new PageImpl<>(postDocuments, pageable, totalHits);
    }

    // 保存到 ES， Post 和 PostDocument 中的字段类型并不完全匹配（userAtList 和 pictureList），要记得做转换
    private void savePostToEs(PostSaveRequest request, Long postId) {
        PostDocument postDocument = new PostDocument();
        BeanUtils.copyProperties(request, postDocument);
        postDocument.setId(String.valueOf(postId));
        postDocument.setPostPictures(request.getPostPictureList());
        postDocument.setUserAt(request.getUserAtList());
        postDocument.setCreateTime(new Date());
        postDocument.setUpdateTime(new Date());
        postDocument.setLikeNum(0);
        postDocument.setTags(request.getTags());
        postDocumentRepository.save(postDocument);
    }

    // todo：更新到 ES
    private void updatePostToEs(PostUpdateRequest request) {

    }

    // todo：删除 ES 中的帖子数据
    private void deletePostToEs(Long postId) {

    }

    /**
     * 将 Post 转换为 PostVO
     *
     * @param post
     * @return
     */
    private PostVO convertPostToPostVO(Post post) {
        PostVO postVO = new PostVO();
        postVO.setId(post.getId());
        postVO.setUserId(post.getUserId());
        postVO.setPostTitle(post.getPostTitle());
        postVO.setPostContent(post.getPostContent());
        postVO.setVisible(post.getVisible());
        postVO.setCreateTime(post.getCreateTime());
        postVO.setUpdateTime(post.getUpdateTime());
        postVO.setPostPictures(post.getPostPictureList());
        postVO.setUserAt(post.getUserAtList());

        // 设置评论数、收藏数、tag 标签
        Long postId = post.getId();
        CompletableFuture<Void> commentNumFuture = CompletableFuture.runAsync(() -> {
            Integer commentNum = commentRemoteService.countCommentByPostId(postId);
            postVO.setCommentNum(commentNum);
        });
        CompletableFuture<Void> collectNumFuture = CompletableFuture.runAsync(() -> {
            Integer collectNum = collectRemoteService.countCollectByTargetIdAndTargetType(postId, CollectConstant.POST_TYPE);
            postVO.setCollectNum(collectNum);
        });
        CompletableFuture<Void> tagFuture = CompletableFuture.runAsync(() -> {
            LambdaQueryWrapper<PostTagRelation> postTagRelationLambdaQueryWrapper = new LambdaQueryWrapper<>();
            postTagRelationLambdaQueryWrapper.eq(PostTagRelation::getPostId, postId);
            List<PostTagRelation> postTagRelationList = postTagRelationService.list(postTagRelationLambdaQueryWrapper);
            List<Long> tagIdList = postTagRelationList.stream().map(PostTagRelation::getTagId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(tagIdList)) {
                postVO.setTags(new ArrayList<>());
                return;
            }

            List<String> tagNameList = tagMapper.selectBatchIds(tagIdList).stream().map(Tag::getTagName).collect(Collectors.toList());
            postVO.setTags(tagNameList);
        });
        CompletableFuture.allOf(commentNumFuture, collectNumFuture, tagFuture).join();
        return postVO;
    }

    private PostVO convertPostDocumentToPostVO(PostDocument postDocument) {
        PostVO postVO = new PostVO();
        postVO.setId(Long.valueOf(postDocument.getId()));
        postVO.setUserId(postDocument.getUserId());
        postVO.setPostTitle(postDocument.getPostTitle());
        postVO.setPostContent(postDocument.getPostContent());
        postVO.setPostPictures(postDocument.getPostPictures());
        postVO.setUserAt(postDocument.getUserAt());
        postVO.setLikeNum(postDocument.getLikeNum());
        postVO.setVisible(PostConstant.ALL_PEOPLE);
        postVO.setTags(postDocument.getTags());
        postVO.setCreateTime(postDocument.getCreateTime());
        postVO.setUpdateTime(postDocument.getUpdateTime());

        // 设置评论数、收藏数
        Long postId = Long.valueOf(postDocument.getId());
        CompletableFuture<Void> commentNumFuture = CompletableFuture.runAsync(() -> {
            Integer commentNum = commentRemoteService.countCommentByPostId(postId);
            postVO.setCommentNum(commentNum);
        });
        CompletableFuture<Void> collectNumFuture = CompletableFuture.runAsync(() -> {
            Integer collectNum = collectRemoteService.countCollectByTargetIdAndTargetType(postId, CollectConstant.POST_TYPE);
            postVO.setCollectNum(collectNum);
        });
        CompletableFuture.allOf(commentNumFuture, collectNumFuture).join();
        return postVO;
    }

    private List<PostCommentVO.PostComment> constructPostCommentList(Post post, UserSession userSession) {
        Page<CommentPostDTO> commentPostDTOPage = commentRemoteService.listCommentUnderPost(post.getId(), 1L, 10L);
        List<CommentPostDTO> commentPostDTOList = commentPostDTOPage.getRecords();
        return commentPostDTOList.stream().map(commentPostDTO -> {
            PostCommentVO.PostComment postComment = new PostCommentVO.PostComment();
            postComment.setCommentId(commentPostDTO.getId());
            postComment.setUserId(commentPostDTO.getUserId());
            postComment.setUserName(commentPostDTO.getUserName());
            postComment.setUserAvatar(commentPostDTO.getUserAvatar());
            postComment.setPostId(commentPostDTO.getPostId());
            postComment.setParentId(commentPostDTO.getParentId());
            postComment.setTargetType(commentPostDTO.getTargetType());
            postComment.setTargetId(commentPostDTO.getTargetId());
            postComment.setCommentContent(commentPostDTO.getCommentContent());
            postComment.setTargetUserId(commentPostDTO.getTargetUserId());
            postComment.setTargetUserName(commentPostDTO.getTargetUserName());
            postComment.setTargetUserAvatar(commentPostDTO.getTargetUserAvatar());
            postComment.setCreateTime(commentPostDTO.getCreateTime());

            // 判断是否点过赞
            if (userSession != null) {
                Long userId = userSession.getId();
                Boolean liked = likeRemoteService.checkLike(userId, commentPostDTO.getId(), LikeConstant.COMMENT_TYPE);
                postComment.setLiked(liked);
            }

            // 设置点赞数
            Integer likeNum = likeRemoteService.countLikeByTargetIdAndTargetType(commentPostDTO.getId(), LikeConstant.COMMENT_TYPE);
            postComment.setLikeNum(likeNum);

            List<CommentPostDTO> bestCommentData = commentPostDTO.getBestCommentData();
            List<PostCommentVO.PostComment> innerCommentList = bestCommentData.stream().map(innerCommentPostDTO -> {
                PostCommentVO.PostComment innerPostComment = new PostCommentVO.PostComment();
                innerPostComment.setCommentId(innerCommentPostDTO.getId());
                innerPostComment.setUserId(innerCommentPostDTO.getUserId());
                innerPostComment.setUserName(innerCommentPostDTO.getUserName());
                innerPostComment.setUserAvatar(innerCommentPostDTO.getUserAvatar());
                innerPostComment.setPostId(innerCommentPostDTO.getPostId());
                innerPostComment.setParentId(innerCommentPostDTO.getParentId());
                innerPostComment.setTargetType(innerCommentPostDTO.getTargetType());
                innerPostComment.setTargetId(innerCommentPostDTO.getTargetId());
                innerPostComment.setCommentContent(innerCommentPostDTO.getCommentContent());
                innerPostComment.setTargetUserId(innerCommentPostDTO.getTargetUserId());
                innerPostComment.setTargetUserName(innerCommentPostDTO.getTargetUserName());
                innerPostComment.setTargetUserAvatar(innerCommentPostDTO.getTargetUserAvatar());
                innerPostComment.setCreateTime(innerCommentPostDTO.getCreateTime());
                innerPostComment.setInnerCommentList(new ArrayList<>());

                // 判断是否点过赞
                if (userSession != null) {
                    Long userId = userSession.getId();
                    Boolean insideLiked = likeRemoteService.checkLike(userId, innerCommentPostDTO.getId(), LikeConstant.COMMENT_TYPE);
                    innerPostComment.setLiked(insideLiked);
                }

                // 设置点赞数
                Integer innerLikeNum = likeRemoteService.countLikeByTargetIdAndTargetType(innerCommentPostDTO.getId(), LikeConstant.COMMENT_TYPE);
                innerPostComment.setLikeNum(innerLikeNum);

                return innerPostComment;
            }).collect(Collectors.toList());
            postComment.setInnerCommentList(innerCommentList);

            return postComment;
        }).collect(Collectors.toList());

    }
}

