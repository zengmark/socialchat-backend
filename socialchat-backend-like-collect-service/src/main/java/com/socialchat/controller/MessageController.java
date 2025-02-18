package com.socialchat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.PageRequest;
import com.socialchat.common.ResultUtils;
import com.socialchat.model.vo.MessageVO;
import com.socialchat.service.MessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "消息模块")
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @ApiOperation(value = "获取消息列表")
    @PostMapping("/listMessage")
    @AuthCheck
    public BaseResponse<Page<MessageVO>> listMessage(@RequestBody PageRequest pageRequest) {
        Page<MessageVO> messageVOPage = messageService.listMessage(pageRequest);
        return ResultUtils.success(messageVOPage);
    }

    @ApiOperation(value = "获取未读消息数")
    @PostMapping("/getUnReadCount")
    @AuthCheck
    public BaseResponse<Integer> getUnReadCount() {
        Integer unReadCount = messageService.getUnReadCount();
        return ResultUtils.success(unReadCount);
    }

}
