package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.ChatRoom;
import com.socialchat.service.ChatRoomService;
import com.socialchat.dao.ChatRoomMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_chat_room】的数据库操作Service实现
* @createDate 2025-02-23 23:08:35
*/
@Service
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoom>
    implements ChatRoomService{

}




