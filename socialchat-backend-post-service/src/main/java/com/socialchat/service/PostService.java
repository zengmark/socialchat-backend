package com.socialchat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.common.PageRequest;
import com.socialchat.model.dto.Record;
import com.socialchat.model.entity.Post;
import com.socialchat.model.request.PostOwnRequest;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostSearchRequest;
import com.socialchat.model.request.PostUpdateRequest;
import com.socialchat.model.vo.PostSearchPageVO;
import com.socialchat.model.vo.PostVO;

import java.util.List;

/**
 * (tb_post)表服务接口
 *
 * @author makejava
 * @since 2024-12-24 22:05:41
 */
public interface PostService extends IService<Post> {

    /**
     * 保存帖子
     *
     * @param request
     * @return
     */
    boolean savePost(PostSaveRequest request);

    /**
     * 定时任务拉取帖子数据
     *
     * @param recordList
     * @return
     */
    boolean savePostBySchedule(List<Record> recordList);

    /**
     * 更新帖子
     *
     * @param request
     * @return
     */
    boolean updatePost(PostUpdateRequest request);

    /**
     * 删除帖子
     *
     * @param postId
     * @return
     */
    boolean deletePost(Long postId);

    /**
     * 获取自己帖子数据
     *
     * @param request
     * @return
     */
    Page<PostVO> listOwnPosts(PostOwnRequest request);

    /**
     * 获取首页帖子数据
     *
     * @param request
     * @return
     */
    PostSearchPageVO listHomePosts(PageRequest request);

    /**
     * 根据搜索词、标签获取帖子数据
     *
     * @param request
     * @return
     */
    PostSearchPageVO listSearchPosts(PostSearchRequest request);
}

