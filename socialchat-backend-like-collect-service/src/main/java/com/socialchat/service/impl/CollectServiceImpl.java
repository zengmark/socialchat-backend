package com.socialchat.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.CollectMapper;
import com.socialchat.model.dto.CollectAddDTO;
import com.socialchat.model.entity.Collect;
import com.socialchat.model.request.CollectAddRequest;
import com.socialchat.service.CollectService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
* @author macbookpro
* @description 针对表【tb_collect】的数据库操作Service实现
* @createDate 2025-01-21 22:53:03
*/
@Service
public class CollectServiceImpl extends ServiceImpl<CollectMapper, Collect>
    implements CollectService{

    @Resource
    private RocketMQTemplate collectRocketMQTemplate;

    @Override
    public boolean collect(CollectAddRequest request) {
        CollectAddDTO collectAddDTO = new CollectAddDTO();
        BeanUtils.copyProperties(request, collectAddDTO);
        collectAddDTO.setCreateTime(new Date());
        collectRocketMQTemplate.convertAndSend("collect-topic", JSON.toJSONString(collectAddDTO));
        return true;
    }
}




