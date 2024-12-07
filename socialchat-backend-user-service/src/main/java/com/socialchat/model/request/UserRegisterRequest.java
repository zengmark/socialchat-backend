package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 账号
    private String userAccount;

    // 密码
    private String userPassword;

    // 邮箱
    private String userEmail;

    // 验证码
    private String verifyCode;
}
