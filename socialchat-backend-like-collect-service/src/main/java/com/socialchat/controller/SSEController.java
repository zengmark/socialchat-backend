package com.socialchat.controller;

import com.socialchat.api.MessageRemoteService;
import com.socialchat.service.SseService;
import io.swagger.annotations.Api;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

@Api(tags = "sse模块")
@Controller
@RequestMapping("/sse")
public class SSEController {

    @DubboReference
    private MessageRemoteService messageRemoteService;

    @Resource
    private SseService sseService;

    /**
     * 客户端请求 SSE 连接，并通过用户 ID 区分不同的连接
     */
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String userId) {
        return sseService.createSseEmitter(userId);
    }

}
