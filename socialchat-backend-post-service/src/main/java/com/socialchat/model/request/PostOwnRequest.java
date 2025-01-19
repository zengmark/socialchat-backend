package com.socialchat.model.request;

import com.socialchat.common.PageRequest;
import lombok.Data;

@Data
public class PostOwnRequest extends PageRequest {

    private Long userId;

    private Integer visible;
}
