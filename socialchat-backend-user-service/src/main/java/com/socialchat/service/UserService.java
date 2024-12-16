package com.socialchat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.socialchat.model.entity.User;
import com.socialchat.model.request.UserRegisterRequest;

/**
 * (tb_user)表服务接口
 *
 * @author 清闲
 * @since 2024-12-15 16:24:37
 */
public interface UserService extends IService<User> {

    Boolean getVerifyCode(String userEmail);

    Boolean register(UserRegisterRequest userRegisterRequest);

}

