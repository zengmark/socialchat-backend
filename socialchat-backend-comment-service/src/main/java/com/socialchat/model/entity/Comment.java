package com.socialchat.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * (tb_comment)表实体类
 *
 * @author makejava
 * @since 2024-12-31 17:38:03
 */
@Data
@EqualsAndHashCode
@TableName(value = "tb_comment")
public class Comment implements Serializable {
    //主键ID
    private Long id;
    //评论人ID
    private Long userId;
    //评论帖子ID
    private Long postId;
    //评论类型，0 代表评论的是帖子，1 代表评论的是评论
    private Integer targetType;
    //评论源ID
    private Long targetId;
    //评论内容
    private String commentContent;
    //被评论人ID
    private Long targetUserId;
    //被评论人头像链接
    private String targetUserAvatar;
    //评论点赞数
    private Integer likeNum;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

