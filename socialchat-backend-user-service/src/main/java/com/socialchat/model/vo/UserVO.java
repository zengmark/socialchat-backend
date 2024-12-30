package com.socialchat.model.vo;

import com.socialchat.model.session.UserSession;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserVO extends UserSession implements Serializable {
    private static final long serialVersionUID = 1L;
}
