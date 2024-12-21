package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.User;
import com.socialchat.model.request.UserLoginRequest;
import com.socialchat.model.request.UserRegisterRequest;
import com.socialchat.model.vo.UserVO;

/**
 * (tb_user)表服务接口
 *
 * @author 清闲
 * @since 2024-12-15 16:24:37
 */
public interface UserService extends IService<User> {

    /**
     * 获取验证码
     *
     * @param userEmail
     * @return
     */
    Boolean getVerifyCode(String userEmail);

    /**
     * 注册
     *
     * @param userRegisterRequest
     * @return
     */
    Boolean register(UserRegisterRequest userRegisterRequest);

    /**
     * 登录
     *
     * @param userLoginRequest
     * @return
     */
    UserVO login(UserLoginRequest userLoginRequest);
}

