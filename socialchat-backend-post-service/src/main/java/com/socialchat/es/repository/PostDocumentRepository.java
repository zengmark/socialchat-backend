package com.socialchat.es.repository;

import com.socialchat.es.document.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {

    /**
     * 根据帖子标题、内容进行搜索
     *
     * @param postTitle
     * @param postContent
     * @param pageable
     * @return
     */
    Page<PostDocument> findByPostTitleContainingOrPostContentContaining(String postTitle, String postContent, Pageable pageable);

    /**
     * 根据帖子标签进行搜索
     *
     * @param tags
     * @param pageable
     * @return
     */
    Page<PostDocument> findByTagsIn(List<String> tags, Pageable pageable);

    /**
     * 根据帖子标题、内容和标签进行搜索
     *
     * @param postTitle
     * @param postContent
     * @param tags
     * @param pageable
     * @return
     */
    Page<PostDocument> findByPostTitleContainingOrPostContentContainingAndTagsIn(String postTitle, String postContent, List<String> tags, Pageable pageable);

}
