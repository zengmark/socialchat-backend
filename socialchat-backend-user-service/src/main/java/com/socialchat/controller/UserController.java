package com.socialchat.controller;

import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.request.UserRegisterRequest;
import com.socialchat.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "用户模块")
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @ApiOperation("获取验证码")
    @GetMapping("/getVerifyCode")
    public BaseResponse<Boolean> getVerifyCode(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null || userRegisterRequest.getUserEmail() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        String userEmail = userRegisterRequest.getUserEmail();
        return ResultUtils.success(userService.getVerifyCode(userEmail));
    }

    @ApiOperation("注册")
    @PostMapping("/register")
    public BaseResponse<Boolean> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册参数为空");
        }
        return ResultUtils.success(userService.register(userRegisterRequest));
    }
}
