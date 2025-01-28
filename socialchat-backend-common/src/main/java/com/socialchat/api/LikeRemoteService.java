package com.socialchat.api;

import com.socialchat.model.remote.like.LikeCountDTO;

import java.util.Date;
import java.util.List;

public interface LikeRemoteService {

    /**
     * 定时任务获取本次需要更新点赞数的帖子数
     *
     * @param startDate
     * @param endDate
     * @return
     */
    Long countLikeData(Date startDate, Date endDate);

    /**
     * 定时任务获取对应的点赞数据
     *
     * @param current
     * @param startDate
     * @param endDate
     * @return
     */
    List<LikeCountDTO> listLikeData(int current, Date startDate, Date endDate);

    /**
     * 根据点赞数排序，获取分页后的帖子ID
     *
     * @param current
     * @param pageSize
     * @return
     */
    List<Long> listPostIdByLikeNum(int current, int pageSize);

    /**
     * 根据 targetId、targetType 查询点赞数
     *
     * @param targetId
     * @param targetType
     * @return
     */
    Integer countLikeByTargetIdAndTargetType(Long targetId, Integer targetType);
}
