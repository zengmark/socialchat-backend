package com.socialchat.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userName;

    private String userProfile;

}
