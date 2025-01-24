package com.socialchat.api;

public interface PostRemoteService {

    /**
     * 同步点赞数据到ES中
     */
    void syncLikeToESPartialUpdate(Long postId, Long likeNum);

}
