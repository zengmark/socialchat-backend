package com.socialchat.model.request;

import com.socialchat.common.PageRequest;
import lombok.Data;

@Data
public class MessagePageRequest extends PageRequest {

    /**
     * 0 代表获取评论，1 代表获取点赞，2 代表获取收藏
     */
    private Integer type;
}
