package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.CommentMapper;
import com.socialchat.model.entity.Comment;
import com.socialchat.service.CommentService;
import org.springframework.stereotype.Service;

/**
 * (tb_comment)表服务实现类
 *
 * @author makejava
 * @since 2024-12-31 17:38:05
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

}

