package com.socialchat.api;

public interface CollectRemoteService {

    /**
     * 根据 targetId、targetType 查询收藏数
     *
     * @param targetId
     * @param targetType
     * @return
     */
    Integer countCollectByTargetIdAndTargetType(Long targetId, Integer targetType);

    /**
     * 判断用户是否收过藏
     *
     * @param userId
     * @param targetId
     * @param targetType
     * @return
     */
    Boolean checkCollect(Long userId, Long targetId, Integer targetType);
}
