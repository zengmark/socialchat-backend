package com.socialchat.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.socialchat.api.LikeRemoteService;
import com.socialchat.constant.LikeConstant;
import com.socialchat.dao.LikeCountMapper;
import com.socialchat.model.entity.LikeCount;
import com.socialchat.model.remote.like.LikeCountDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@DubboService
@Slf4j
public class LikeRemoteServiceImpl implements LikeRemoteService {

    @Resource
    private LikeCountMapper likeCountMapper;

    @Override
    public Long countLikeData(Date startDate, Date endDate) {
        LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCount::getTargetType, LikeConstant.POST_TYPE);
        queryWrapper.between(LikeCount::getUpdateTime, startDate, endDate);
        return likeCountMapper.selectCount(queryWrapper);
    }

    @Override
    public List<LikeCountDTO> listLikeData(int current, Date startDate, Date endDate) {
        LambdaQueryWrapper<LikeCount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LikeCount::getTargetType, LikeConstant.POST_TYPE);
        queryWrapper.between(LikeCount::getUpdateTime, startDate, endDate);

        Page<LikeCount> likeCountPage = likeCountMapper.selectPage(new Page<>(current, LikeConstant.LIKE_PAGE_SIZE), queryWrapper);
        List<LikeCount> likeCountList = likeCountPage.getRecords();

        return likeCountList.stream().map(likeCount -> {
            LikeCountDTO likeCountDTO = new LikeCountDTO();
            likeCountDTO.setPostId(likeCount.getTargetId());
            likeCountDTO.setLikeNum(likeCount.getLikeNum());
            return likeCountDTO;
        }).collect(Collectors.toList());
    }
}
