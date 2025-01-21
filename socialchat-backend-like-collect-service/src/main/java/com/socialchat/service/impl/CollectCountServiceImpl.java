package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.CollectCount;
import com.socialchat.service.CollectCountService;
import com.socialchat.dao.CollectCountMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_collect_count】的数据库操作Service实现
* @createDate 2025-01-21 22:53:03
*/
@Service
public class CollectCountServiceImpl extends ServiceImpl<CollectCountMapper, CollectCount>
    implements CollectCountService{

}




