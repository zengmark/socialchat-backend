package com.socialchat.model.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * (tb_tag)表实体类
 *
 * @author makejava
 * @since 2025-01-01 23:33:56
 */
@Data
@EqualsAndHashCode
@TableName(value = "tb_tag")
public class Tag implements Serializable {
    //主键ID
    private Long id;
    //标签名称
    private String tagName;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

