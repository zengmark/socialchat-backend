package com.socialchat.api;

import com.socialchat.model.remote.like.LikeCountDTO;

import java.util.Date;
import java.util.List;

public interface LikeRemoteService {

    /**
     * 获取本次需要更新点赞数的帖子数
     *
     * @param startDate
     * @param endDate
     * @return
     */
    Long countLikeData(Date startDate, Date endDate);

    /**
     * 获取对应的点赞数据
     *
     * @param current
     * @param startDate
     * @param endDate
     * @return
     */
    List<LikeCountDTO> listLikeData(int current, Date startDate, Date endDate);

}
