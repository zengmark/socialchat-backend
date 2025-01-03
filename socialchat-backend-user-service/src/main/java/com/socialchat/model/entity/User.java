package com.socialchat.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * (tb_user)表实体类
 *
 * @author 清闲
 * @since 2024-12-15 16:24:35
 */
@Data
@EqualsAndHashCode
@TableName(value = "tb_user")
public class User implements Serializable {
    //主键ID
    private Long id;
    //用户名
    private String userName;
    //账号
    private String userAccount;
    //密码
    private String userPassword;
    //邮箱
    private String userEmail;
    //头像链接
    private String userAvatar;
    //简介
    private String userProfile;
    //粉丝数
    private Long fansNum;
    //关注数
    private Long focusNum;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

