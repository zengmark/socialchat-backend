package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.model.entity.ChatRoomMember;
import com.socialchat.service.ChatRoomMemberService;
import com.socialchat.dao.ChatRoomMemberMapper;
import org.springframework.stereotype.Service;

/**
* @author macbookpro
* @description 针对表【tb_chat_room_member】的数据库操作Service实现
* @createDate 2025-02-23 23:08:35
*/
@Service
public class ChatRoomMemberServiceImpl extends ServiceImpl<ChatRoomMemberMapper, ChatRoomMember>
    implements ChatRoomMemberService{

}




