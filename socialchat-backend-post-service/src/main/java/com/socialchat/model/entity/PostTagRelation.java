package com.socialchat.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * (tb_post_tag)表实体类
 *
 * @author makejava
 * @since 2025-01-01 23:33:58
 */
@Data
@EqualsAndHashCode
@TableName(value = "tb_post_tag")
public class PostTagRelation implements Serializable {
    //主键ID
    private Long id;
    //帖子ID
    private Long postId;
    //标签ID
    private Long tagId;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

