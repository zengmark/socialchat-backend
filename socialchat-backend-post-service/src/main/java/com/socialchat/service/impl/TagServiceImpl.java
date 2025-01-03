package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.TagMapper;
import com.socialchat.model.entity.Tag;
import com.socialchat.service.TagService;
import org.springframework.stereotype.Service;

/**
 * (tb_tag)表服务实现类
 *
 * @author makejava
 * @since 2025-01-01 23:33:57
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

}

