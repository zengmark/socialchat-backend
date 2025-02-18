package com.socialchat.provider;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.socialchat.api.UserRemoteService;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.UserMapper;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.User;
import com.socialchat.model.remote.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.List;

@DubboService
@Slf4j
public class UserRemoteServiceImpl implements UserRemoteService {

    @Resource
    private UserMapper userMapper;

    @Override
    public Long saveUser(UserDTO userDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        String userAccount = userDTO.getUserAccount();
        String userPassword = userDTO.getUserPassword();
        String userEmail = userDTO.getUserEmail();
        String userName = userDTO.getUserName();
        String userAvatar = userDTO.getUserAvatar();
        String userProfile = userDTO.getUserProfile();

        String encryptUserPassword = DigestUtil.md5Hex(UserConstant.SALT + userPassword);
        queryWrapper
                .eq(StringUtils.isNotBlank(userAccount), User::getUserAccount, userAccount)
                .or()
                .eq(StringUtils.isNotBlank(userEmail), User::getUserEmail, userEmail);
        List<User> userList = userMapper.selectList(queryWrapper);
        // 如果用户名或者账号其中一个存在数据库中，则注册失败
        if (CollectionUtils.isNotEmpty(userList)) {
            return userList.get(0).getId();
        }

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptUserPassword);
        user.setUserEmail(userEmail);
        user.setUserName(userName);
        user.setUserAvatar(userAvatar);
        user.setUserProfile(userProfile);
        userMapper.insert(user);

        return user.getId();
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return userDTO;
    }
}
