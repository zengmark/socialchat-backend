package com.socialchat.provider;

import com.socialchat.api.PostRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import javax.annotation.Resource;
import java.util.Collections;

@DubboService
@Slf4j
public class PostRemoteServiceImpl implements PostRemoteService {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void syncLikeToESPartialUpdate(Long postId, Long likeNum) {
        log.info("同步点赞数据到ES，帖子为:{}，点赞数为:{}", postId, likeNum);

        String postDocumentId = String.valueOf(postId);
        Integer likeCount = Math.toIntExact(likeNum);

        // 构建部分更新请求
        UpdateQuery updateQuery = UpdateQuery.builder(postDocumentId)
                .withScript("ctx._source.likeNum = params.likeNum")
                .withParams(Collections.singletonMap("likeNum", likeCount))
                .build();

        // 执行部分更新
        elasticsearchRestTemplate.update(updateQuery, IndexCoordinates.of("posts"));
    }
}
