package com.socialchat.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostSearchPageVO {

    private long current;

    private long size;

    private long total;

    private List<PostVO> records;

}
