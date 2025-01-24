package com.socialchat.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.LikeMapper;
import com.socialchat.model.dto.LikeAddDTO;
import com.socialchat.model.entity.Like;
import com.socialchat.model.request.LikeAddRequest;
import com.socialchat.service.LikeService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
* @author macbookpro
* @description 针对表【tb_like】的数据库操作Service实现
* @createDate 2025-01-21 22:53:03
*/
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like>
    implements LikeService{

    @Resource
    private RocketMQTemplate likeRocketMQTemplate;

    @Override
    public boolean like(LikeAddRequest request) {
        LikeAddDTO likeAddDTO = new LikeAddDTO();
        BeanUtils.copyProperties(request, likeAddDTO);
        likeAddDTO.setCreateTime(new Date());
        likeRocketMQTemplate.convertAndSend("like-topic", JSON.toJSONString(likeAddDTO));
        return true;
    }
}




