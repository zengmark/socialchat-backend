package com.socialchat.controller;

import com.socialchat.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "帖子模块")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }

//    @ApiOperation("/获取首页帖子")
//    @GetMapping("/listPost")

}
