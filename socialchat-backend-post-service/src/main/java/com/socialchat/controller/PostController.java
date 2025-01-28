package com.socialchat.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.PageRequest;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.helper.ImageServiceHelper;
import com.socialchat.model.request.PostOwnRequest;
import com.socialchat.model.request.PostSaveRequest;
import com.socialchat.model.request.PostSearchRequest;
import com.socialchat.model.request.PostUpdateRequest;
import com.socialchat.model.vo.PostSearchPageVO;
import com.socialchat.model.vo.PostVO;
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
    @PostMapping("/savePost")
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
    @AuthCheck
    public BaseResponse<Boolean> uploadPostImage(@RequestParam MultipartFile file, @RequestParam("sessionId") String sessionId) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片文件不能为空");
        }
        imageServiceHelper.uploadImageToGiteeAsync(file, sessionId);
        return ResultUtils.success(true);
    }

    @ApiOperation("更新帖子")
    @PostMapping("/updatePost")
    public BaseResponse<Boolean> updatePost(@RequestBody PostUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新帖子餐宿不能为空");
        }
        boolean flag = postService.updatePost(request);
        return ResultUtils.success(flag);
    }

    @ApiOperation("删除帖子")
    @PostMapping("/deletePost")
    @AuthCheck
    public BaseResponse<Boolean> deletePost(@RequestParam Long postId) {
        if (ObjectUtil.isNull(postId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        boolean flag = postService.deletePost(postId);
        return ResultUtils.success(flag);
    }

    @ApiOperation("获取自己帖子数据")
    @PostMapping("/listOwnPosts")
    @AuthCheck
    public BaseResponse<Page<PostVO>> listOwnPosts(@RequestBody PostOwnRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        Page<PostVO> postVOPage = postService.listOwnPosts(request);
        return ResultUtils.success(postVOPage);
    }

    @ApiOperation("获取首页帖子数据")
    @PostMapping("/listHomePosts")
    public BaseResponse<PostSearchPageVO> listHomePosts(@RequestBody PageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        PostSearchPageVO postVOPage = postService.listHomePosts(request);
        return ResultUtils.success(postVOPage);
    }

    @ApiOperation("根据搜索词、标签获取帖子数据")
    @PostMapping("/listSearchPosts")
    public BaseResponse<PostSearchPageVO> listSearchPosts(@RequestBody PostSearchRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        PostSearchPageVO postVOPage = postService.listSearchPosts(request);
        return ResultUtils.success(postVOPage);
    }

}
