package com.socialchat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.dao.VoteMapper;
import com.socialchat.model.entity.Vote;
import com.socialchat.service.VoteService;
import org.springframework.stereotype.Service;

/**
 * (tb_vote)表服务实现类
 *
 * @author makejava
 * @since 2024-12-30 01:13:44
 */
@Service
public class VoteServiceImpl extends ServiceImpl<VoteMapper, Vote> implements VoteService {

}

