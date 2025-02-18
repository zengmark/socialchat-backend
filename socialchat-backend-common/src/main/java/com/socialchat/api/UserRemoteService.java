package com.socialchat.api;

import com.socialchat.model.remote.user.UserDTO;

public interface UserRemoteService {

    /**
     * 保存用户信息
     *
     * @param userDTO
     * @return
     */
    Long saveUser(UserDTO userDTO);

    /**
     * 获取用户信息
     */
    UserDTO getUserById(Long userId);

}
