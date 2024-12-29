package com.socialchat.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * (tb_vote)表实体类
 *
 * @author makejava
 * @since 2024-12-30 01:13:41
 */
@Data
public class Vote {
    //主键ID
    private Long id;
    //创建人ID
    private Long userId;
    //归属帖子ID
    private Long postId;
    //投票标题
    private String voteTitle;
    //投票项内容
    private String voteContent;
    //投票数
    private Integer voteNum;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;
}

