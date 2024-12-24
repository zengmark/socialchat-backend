package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.PostMapper;
import com.socialchat.model.entity.Post;
import com.socialchat.service.PostService;
import org.springframework.stereotype.Service;

/**
 * (Post)表服务实现类
 *
 * @author makejava
 * @since 2024-12-24 22:05:42
 */
@Service("tbPostService")
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

}

