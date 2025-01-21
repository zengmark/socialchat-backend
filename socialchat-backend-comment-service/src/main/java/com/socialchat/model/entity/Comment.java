package com.socialchat.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

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
    //评论人用户名
    private String userName;
    //评论人头像链接
    private String userAvatar;
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
    //被评论人用户名
    private String targetUserName;
    //被评论人头像链接
    private String targetUserAvatar;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

