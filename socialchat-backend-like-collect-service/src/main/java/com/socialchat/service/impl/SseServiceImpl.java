package com.socialchat.service.impl;

import com.socialchat.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseServiceImpl implements SseService {
    // 使用 Map 存储每个客户端的连接，key 是客户端标识符（如用户 ID），value 是 SseEmitter 对象
    private final ConcurrentHashMap<String, SseEmitter> clientEmitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createSseEmitter(String userId) {
        log.info("建立SSE连接:{}", userId);
        // 创建一个 SseEmitter 实例
        SseEmitter emitter = new SseEmitter(0L);
        clientEmitters.put(userId, emitter);

        // 设置连接完成时的清理操作
        emitter.onCompletion(() -> clientEmitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            clientEmitters.remove(userId);
        });

        return emitter;
    }

    /**
     * 模拟消息推送：根据用户 ID 向指定用户推送消息
     */
    public void sendNotificationToUser(String userId, String message) {
        SseEmitter emitter = clientEmitters.get(userId);
        if (emitter != null) {
            try {
                // 向指定客户端推送消息
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            } catch (IOException e) {
                // 处理发送异常（例如客户端断开连接）
                clientEmitters.remove(userId);
            }
        }
    }
}
