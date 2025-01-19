package com.socialchat.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class PostVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String postTitle;

    private String postContent;

    private List<String> postPictures;

    private List<Long> userAt;

    private Integer likeNum;

    private Integer commentNum;

    private Integer collectNum;

    private Integer visible;

    private Date createTime;

    private Date updateTime;

}
