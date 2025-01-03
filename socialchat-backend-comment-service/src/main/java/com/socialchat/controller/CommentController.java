package com.socialchat.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "评论服务")
public class CommentController {

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
