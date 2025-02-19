package com.socialchat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.common.PageRequest;
import com.socialchat.model.entity.Message;
import com.socialchat.model.vo.MessageVO;

/**
 * @author macbookpro
 * @description 针对表【tb_message】的数据库操作Service
 * @createDate 2025-02-15 23:03:59
 */
public interface MessageService extends IService<Message> {

    /**
     * 获取消息列表
     *
     * @param pageRequest
     * @return
     */
    Page<MessageVO> listMessage(PageRequest pageRequest);

    /**
     * 获取未读消息数
     *
     * @return
     */
    Integer getUnReadCount();

    /**
     * 已读消息
     *
     * @return
     */
    boolean readMessage();
}
