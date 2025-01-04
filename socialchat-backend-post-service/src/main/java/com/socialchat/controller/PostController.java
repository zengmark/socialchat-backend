package com.socialchat.controller;

import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.helper.ImageServiceHelper;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.service.PostService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@Api(tags = "帖子模块")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private ImageServiceHelper imageServiceHelper;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @ApiOperation("保存帖子")
    @PostMapping("/save")
    @AuthCheck
    public BaseResponse<Boolean> savePost(@RequestBody PostSaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存帖子参数不能为空");
        }
        boolean flag = postService.savePost(request);
        return ResultUtils.success(flag);
    }

    @ApiOperation("上传图片")
    @PostMapping("/uploadPostImage")
    public BaseResponse<Boolean> uploadPostImage(@RequestParam MultipartFile file, @RequestParam("sessionId") String sessionId) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片文件不能为空");
        }
//        try {
//            imageServiceHelper.uploadImageToGitee(file);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        imageServiceHelper.uploadImageToGiteeAsync(file, sessionId);
        return ResultUtils.success(true);
    }

//    @ApiOperation("获取首页帖子")
//    @PostMapping("/listPost")

}
