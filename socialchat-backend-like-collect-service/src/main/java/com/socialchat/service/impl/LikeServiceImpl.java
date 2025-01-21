package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.Like;
import com.socialchat.service.LikeService;
import com.socialchat.dao.LikeMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_like】的数据库操作Service实现
* @createDate 2025-01-21 22:53:03
*/
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like>
    implements LikeService{

}




