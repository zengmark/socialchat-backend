package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.PostTagRelationMapper;
import com.socialchat.model.entity.PostTagRelation;
import com.socialchat.service.PostTagRelationService;
import org.springframework.stereotype.Service;

/**
 * (tb_post_tag_relation)表服务实现类
 *
 * @author makejava
 * @since 2025-01-01 23:33:58
 */
@Service
public class PostTagRelationServiceImpl extends ServiceImpl<PostTagRelationMapper, PostTagRelation> implements PostTagRelationService {

}

