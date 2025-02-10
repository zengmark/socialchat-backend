package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.Tag;
import com.socialchat.model.vo.TagVO;

import java.util.List;

/**
 * (tb_tag)表服务接口
 *
 * @author makejava
 * @since 2025-01-01 23:33:57
 */
public interface TagService extends IService<Tag> {

    /**
     * 获取标签列表
     * @return
     */
    List<TagVO> listTagList();
}

