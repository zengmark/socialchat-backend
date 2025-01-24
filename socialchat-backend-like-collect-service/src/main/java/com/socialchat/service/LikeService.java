package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.Like;
import com.socialchat.model.request.LikeAddRequest;

/**
 * @author macbookpro
 * @description 针对表【tb_like】的数据库操作Service
 * @createDate 2025-01-21 22:53:03
 */
public interface LikeService extends IService<Like> {

    /**
     * 用户点赞/取消点赞
     *
     * @param request
     * @return
     */
    boolean like(LikeAddRequest request);
}
