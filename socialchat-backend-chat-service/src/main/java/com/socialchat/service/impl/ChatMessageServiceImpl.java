package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.ChatMessage;
import com.socialchat.service.ChatMessageService;
import com.socialchat.dao.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_chat_message】的数据库操作Service实现
* @createDate 2025-02-23 23:08:35
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{

}




