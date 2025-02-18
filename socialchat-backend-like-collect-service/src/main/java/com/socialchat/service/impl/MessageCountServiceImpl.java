package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.MessageCountMapper;
import com.socialchat.model.entity.MessageCount;
import com.socialchat.service.MessageCountService;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_message_count】的数据库操作Service实现
* @createDate 2025-02-15 23:48:01
*/
@Service
public class MessageCountServiceImpl extends ServiceImpl<MessageCountMapper, MessageCount>
    implements MessageCountService{

}




