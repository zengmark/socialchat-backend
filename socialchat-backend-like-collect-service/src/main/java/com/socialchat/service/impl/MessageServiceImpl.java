package com.socialchat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.api.CommentRemoteService;
import com.socialchat.api.UserRemoteService;
import com.socialchat.common.PageRequest;
import com.socialchat.constant.MessageConstant;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.MessageCountMapper;
import com.socialchat.dao.MessageMapper;
import com.socialchat.model.entity.Message;
import com.socialchat.model.entity.MessageCount;
import com.socialchat.model.remote.user.UserDTO;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.MessageVO;
import com.socialchat.service.MessageService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author macbookpro
 * @description 针对表【tb_message】的数据库操作Service实现
 * @createDate 2025-02-15 23:03:59
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private MessageCountMapper messageCountMapper;

    @DubboReference
    private UserRemoteService userRemoteService;

    @DubboReference
    private CommentRemoteService commentRemoteService;

    @Override
    public Page<MessageVO> listMessage(PageRequest pageRequest) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        Long acceptUserId = userSession.getId();

        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();

        LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
        messageQueryWrapper.eq(Message::getAcceptUserId, acceptUserId);
        messageQueryWrapper.orderByDesc(Message::getCreateTime);
        Page<Message> messagePage = messageMapper.selectPage(new Page<>(current, pageSize), messageQueryWrapper);
        List<Message> messageList = messagePage.getRecords();

        List<MessageVO> messageVOList = messageList.stream()
                .map(this::convertMessageToMessageVO).collect(Collectors.toList());
        Page<MessageVO> messageVOPage = new Page<>(current, pageSize, messagePage.getTotal());
        messageVOPage.setRecords(messageVOList);
        return messageVOPage;
    }

    @Override
    public Integer getUnReadCount() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        Long userId = userSession.getId();
        LambdaQueryWrapper<MessageCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessageCount::getUserId, userId);
        MessageCount messageCount = messageCountMapper.selectOne(queryWrapper);
        int messageNum = 0;
        if (messageCount != null) {
            messageNum = messageCount.getMessageCount();
        }
        return messageNum;
    }

    private MessageVO convertMessageToMessageVO(Message message) {
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);

        // 获取发送用户信息
        UserDTO sourceUserDTO = userRemoteService.getUserById(message.getSourceUserId());
        String sourceUserName = sourceUserDTO.getUserName();
        String sourceUserAvatar = sourceUserDTO.getUserAvatar();
        messageVO.setSourceUserName(sourceUserName);
        messageVO.setSourceUserAvatar(sourceUserAvatar);

        // 如果是评论，获取评论内容
        if (MessageConstant.COMMENT.equals(message.getTargetType())) {
            Long targetId = message.getTargetId();
            String commentContent = commentRemoteService.getCommentContentById(targetId);
            messageVO.setCommentContent(commentContent);
        }

        return messageVO;
    }
}




