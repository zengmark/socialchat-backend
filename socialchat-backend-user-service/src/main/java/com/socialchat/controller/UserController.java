package com.socialchat.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.socialchat.annotation.AuthCheck;
import com.socialchat.common.BaseResponse;
import com.socialchat.common.ErrorCode;
import com.socialchat.common.ResultUtils;
import com.socialchat.constant.UserConstant;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.User;
import com.socialchat.model.request.UserLoginRequest;
import com.socialchat.model.request.UserRegisterRequest;
import com.socialchat.model.request.UserUpdateAvatarRequest;
import com.socialchat.model.request.UserUpdateRequest;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.UserVO;
import com.socialchat.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@Api(tags = "用户模块")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private StringEncryptor encryptor;

    @ApiOperation("获取登录用户信息 token 值，仅测试使用，生产关闭")
    @GetMapping("/test")
    public SaTokenInfo test() {
        return StpUtil.getTokenInfo();
    }

    @ApiOperation("jasypt加密接口")
    @GetMapping("/jasypt")
    public String jasypt(@RequestParam("encryptContent") String encryptContent) {
        String encrypt = "ENC(" + encryptor.encrypt(encryptContent) + ")";
        log.info("加密后的为：" + encrypt);
        return encrypt;
    }

    @ApiOperation("获取验证码")
    @PostMapping("/getVerifyCode")
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

    @ApiOperation("登录")
    @PostMapping("/login")
    public BaseResponse<String> login(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录参数为空");
        }
        String token = userService.login(userLoginRequest);
        return ResultUtils.success(token);
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        StpUtil.getTokenSessionByToken(token).logout();
        return ResultUtils.success(true);
    }

    @ApiOperation("注销用户")
    @PostMapping("/deleteUser")
    public BaseResponse<Boolean> deleteUser() {
        boolean flag = userService.deleteUser();
        return ResultUtils.success(flag);
    }

    @ApiOperation("获取登录用户信息")
    @PostMapping("/getLoginUser")
    @AuthCheck
    public BaseResponse<UserVO> getLoginUser() {
        UserVO userVO = userService.getLoginUser();
        return ResultUtils.success(userVO);
    }

    @ApiOperation("根据用户ID获取用户信息")
    @PostMapping("/getUserInfoByUserId")
    public BaseResponse<User> getUserInfoByUserId(@RequestParam Long userId) {
        User user = userService.getById(userId);
        return ResultUtils.success(user);
    }

    @ApiOperation("修改用户信息")
    @PostMapping("/updateUserInfo")
    @AuthCheck
    public BaseResponse<UserSession> updateUserInfo(@RequestBody UserUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改用户信息参数不能为空");
        }
        UserSession userSession = userService.updateUserInfo(request);
        return ResultUtils.success(userSession);
    }

    @ApiOperation("修改用户头像")
    @PostMapping("/updateUserAvatar")
    @AuthCheck
    public BaseResponse<UserSession> updateUserAvatar(@RequestBody UserUpdateAvatarRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改用户头像参数不能为空");
        }
        String userAvatar = request.getUserAvatar();
        UserSession userSession = userService.updateUserAvatar(userAvatar);
        return ResultUtils.success(userSession);
    }
}
