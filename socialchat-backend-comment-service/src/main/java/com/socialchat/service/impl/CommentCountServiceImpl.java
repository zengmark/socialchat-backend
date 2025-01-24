package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.CommentCountMapper;
import com.socialchat.model.entity.CommentCount;
import com.socialchat.service.CommentCountService;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_comment_count】的数据库操作Service实现
* @createDate 2025-01-21 22:59:02
*/
@Service
public class CommentCountServiceImpl extends ServiceImpl<CommentCountMapper, CommentCount>
    implements CommentCountService{

}




