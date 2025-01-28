package com.socialchat.model.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
    //爬虫来源ID
    private String sourceId;
    //创建人ID
    private Long userId;
    //帖子标题
    private String postTitle;
    //帖子内容
    private String postContent;
    //帖子图片数组
    private String postPictures;
    //@用户的id数组
    private String userAt;
    //是否可见
    private Integer visible;
    //创建时间
    private Date createTime;
    //更新时间
    @TableField(value = "update_time", update = "now()")
    private Date updateTime;
    //软删除标记
    private Integer isDeleted;

    public void setPostPictureList(List<String> pictureList) {
        this.postPictures = JSON.toJSONString(pictureList);
    }

    public List<String> getPostPictureList() {
        return JSON.parseArray(this.postPictures, String.class);
    }

    public void setUserAtList(List<Long> userIdList) {
        this.userAt = JSON.toJSONString(userIdList);
    }

    public List<Long> getUserAtList() {
        return JSON.parseArray(this.userAt, Long.class);
    }

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

