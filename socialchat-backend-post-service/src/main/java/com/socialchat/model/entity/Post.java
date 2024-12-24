package com.socialchat.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * (tb_post)表实体类
 *
 * @author makejava
 * @since 2024-12-24 22:05:40
 */
@Data
@EqualsAndHashCode
@TableName(value = "tb_post")
public class Post implements Serializable {
    //主键ID
    private Long id;
    //创建人ID
    private Long userId;
    //帖子标题
    private String postTitle;
    //帖子内容
    private String postContent;
    //帖子图片数组
    private String postPictures;
    //点赞数
    private Integer likeNum;
    //评论数
    private Integer commentNum;
    //收藏数
    private Integer collectNum;
    //是否可见
    private Integer visible;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

