package com.socialchat.controller;

import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.request.CollectAddRequest;
import com.socialchat.service.CollectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "收藏模块")
@RestController
@RequestMapping("/collect")
@Slf4j
public class CollectController {

    @Resource
    private CollectService collectService;

    @Resource
    private RocketMQTemplate collectRocketMQTemplate;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        collectRocketMQTemplate.convertAndSend("collect-topic", "test");
        return "test";
    }

    @ApiOperation("用户收藏/取消收藏")
    @PostMapping("/collect")
    @AuthCheck
    public BaseResponse<Boolean> collect(@RequestBody CollectAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean flag = collectService.collect(request);
        return ResultUtils.success(flag);
    }

}
