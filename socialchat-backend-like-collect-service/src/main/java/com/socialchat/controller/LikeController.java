package com.socialchat.controller;

import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.request.LikeAddRequest;
import com.socialchat.service.LikeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "点赞模块")
@RestController
@RequestMapping("/like")
@Slf4j
public class LikeController {

    @Resource
    private LikeService likeService;

    @Resource
    private RocketMQTemplate likeRocketMQTemplate;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        likeRocketMQTemplate.convertAndSend("like-topic", "test");
        return "test";
    }

    @ApiOperation("用户点赞/取消点赞")
    @PostMapping("/like")
    @AuthCheck
    public BaseResponse<Boolean> like(@RequestBody LikeAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean flag = likeService.like(request);
        return ResultUtils.success(flag);
    }

}
