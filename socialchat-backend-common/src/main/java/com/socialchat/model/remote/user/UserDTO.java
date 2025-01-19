package com.socialchat.model.remote.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userName;

    private String userAccount;

    private String userPassword;

    private String userEmail;

    private String userAvatar;

    private String userProfile;

}
