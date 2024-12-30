package com.socialchat.model.session;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private Long fansNum;

    private Long focusNum;

}