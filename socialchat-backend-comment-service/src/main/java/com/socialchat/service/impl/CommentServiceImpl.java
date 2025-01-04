package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.CommentMapper;
import com.socialchat.model.entity.Comment;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * (tb_comment)表服务实现类
 *
 * @author makejava
 * @since 2024-12-31 17:38:05
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Override
    public boolean addComment(CommentAddRequest request) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(request, comment);

        int result = commentMapper.insert(comment);

        // todo：后期引入消息表后要加事物控制并插入通知信息，并且使用 SSE 作服务端消息推送

        return result > 0;
    }
}

