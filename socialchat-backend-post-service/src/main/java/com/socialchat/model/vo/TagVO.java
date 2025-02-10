package com.socialchat.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class TagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tagName;

    private Long tagId;

}
