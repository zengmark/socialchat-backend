package com.socialchat.api;

import com.socialchat.model.remote.user.UserDTO;

public interface UserRemoteService {

    Long saveUser(UserDTO userDTO);

}
