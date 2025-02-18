package com.socialchat.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer targetType;

    private Long targetId;

    private Long sourceUserId;

    private String sourceUserName;

    private String sourceUserAvatar;

    private Long acceptUserId;

    private String acceptUserName;

    private String acceptUserAvatar;

    private Integer visible;

    private Date createTime;

    private String commentContent;

}
