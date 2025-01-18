package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.Post;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostUpdateRequest;

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
}

