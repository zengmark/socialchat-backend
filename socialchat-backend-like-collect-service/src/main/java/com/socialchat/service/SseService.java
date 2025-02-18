package com.socialchat.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    /**
     * 建立连接
     *
     * @param userId
     * @return
     */
    SseEmitter createSseEmitter(String userId);

    /**
     * 发送消息
     *
     * @param userId
     * @param message
     */
    void sendNotificationToUser(String userId, String message);
}
