package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.TagMapper;
import com.socialchat.model.entity.Tag;
import com.socialchat.model.vo.TagVO;
import com.socialchat.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * (tb_tag)表服务实现类
 *
 * @author makejava
 * @since 2025-01-01 23:33:57
 */
@Service
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private static final List<TagVO> tagVOList;

    static {
        log.info("初始化标签列表");
        tagVOList = new ArrayList<>();
        tagVOList.add(new TagVO("学习打卡", 1L));
        tagVOList.add(new TagVO("码神挑战", 48L));
        tagVOList.add(new TagVO("健康打卡", 86L));
        tagVOList.add(new TagVO("提问", 88L));
        tagVOList.add(new TagVO("求职", 47L));
        tagVOList.add(new TagVO("开发交流", 1478L));
        tagVOList.add(new TagVO("学习", 108L));
    }

    @Override
    public List<TagVO> listTagList() {
        return tagVOList;
    }
}

