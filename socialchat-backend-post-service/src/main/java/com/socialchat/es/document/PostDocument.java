package com.socialchat.es.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "posts")
public class PostDocument {

    @Id
    private String id; // Elasticsearch 中的文档ID

    @Field(type = FieldType.Long)
    private Long userId; // 创建人ID

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String postTitle; // 帖子标题

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String postContent; // 帖子内容

    @Field(type = FieldType.Keyword)
    private List<String> postPictures; // 帖子图片数组

    @Field(type = FieldType.Keyword)
    private List<Long> userAt; // @用户的id数组

    @Field(type = FieldType.Integer)
    private Integer likeNum; // 点赞数

    @Field(type = FieldType.Integer)
    private Integer commentNum; // 评论数

    @Field(type = FieldType.Integer)
    private Integer collectNum; // 收藏数

    @Field(type = FieldType.Boolean)
    private Boolean visible; // 是否可见

    @Field(type = FieldType.Date)
    private Date createTime; // 创建时间

    @Field(type = FieldType.Date)
    private Date updateTime; // 更新时间

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted; // 软删除标记

    @Field(type = FieldType.Keyword)
    private List<String> tags; // 标签名称列表
}
