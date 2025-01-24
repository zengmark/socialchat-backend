package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.Collect;
import com.socialchat.model.request.CollectAddRequest;

/**
 * @author macbookpro
 * @description 针对表【tb_collect】的数据库操作Service
 * @createDate 2025-01-21 22:53:03
 */
public interface CollectService extends IService<Collect> {

    /**
     * 用户收藏/取消收藏
     *
     * @param request
     * @return
     */
    boolean collect(CollectAddRequest request);
}
