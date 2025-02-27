package com.socialchat.model.request;

import com.socialchat.common.PageRequest;
import lombok.Data;

import java.util.List;

@Data
public class PostSearchRequest extends PageRequest {

    /**
     * 搜索情况下的搜索词
     */
    private String searchWord;

    /**
     * 标签项
     */
    private List<String> tagList;

}
