package com.socialchat.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.request.CommentAddRequest;
import com.socialchat.model.request.CommentPageRequest;
import com.socialchat.model.vo.CommentVO;
import com.socialchat.service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

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

    @ApiOperation("删除评论")
    @PostMapping("/deleteComment")
    @AuthCheck
    public BaseResponse<Boolean> deleteComment(@RequestParam Long commentId) {
        if (ObjectUtil.isNull(commentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除评论数据id不能为空");
        }
        boolean flag = commentService.deleteComment(commentId);
        return ResultUtils.success(flag);
    }

    @ApiOperation("帖子下分页查询评论")
    @PostMapping("/listCommentByPostId")
    public BaseResponse<Page<CommentVO>> listCommentByPostId(@RequestBody CommentPageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询评论参数不能为空");
        }
        Page<CommentVO> commentVOPage = commentService.listCommentByPostId(request);
        return ResultUtils.success(commentVOPage);
    }

    @ApiOperation("评论下分页查询评论")
    @PostMapping("/listCommentByCommentId")
    public BaseResponse<CommentVO> listCommentByCommentId(@RequestBody CommentPageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询评论参数不能为空");
        }
        CommentVO commentVO = commentService.listCommentByCommentId(request);
        return ResultUtils.success(commentVO);
    }

//    @ApiOperation("查询自己评论历史")
//    @PostMapping("/listOwnCommentHistory")
//    @AuthCheck
//    public BaseResponse<Page<CommentVO>> listOwnCommentHistory(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
//        if (request == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不能为空");
//        }
//        Page<CommentVO> commentVOPage = commentService.listOwnCommentHistory(pageRequest, request);
//        return ResultUtils.success(commentVOPage);
//    }
}
