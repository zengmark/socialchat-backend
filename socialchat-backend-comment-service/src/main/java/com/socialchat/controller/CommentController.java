package com.socialchat.controller;

import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "评论服务")
public class CommentController {

    @Resource
    private CommentService commentService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @ApiOperation("新增评论")
    @PostMapping("/addComment")
    @AuthCheck
    public BaseResponse<Boolean> addComment(@RequestBody CommentAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加评论参数不能为空");
        }
        boolean flag = commentService.addComment(request);
        return ResultUtils.success(flag);
    }
}
