package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.LikeCount;
import com.socialchat.service.LikeCountService;
import com.socialchat.dao.LikeCountMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_like_count】的数据库操作Service实现
* @createDate 2025-01-21 22:53:03
*/
@Service
public class LikeCountServiceImpl extends ServiceImpl<LikeCountMapper, LikeCount>
    implements LikeCountService{

}




