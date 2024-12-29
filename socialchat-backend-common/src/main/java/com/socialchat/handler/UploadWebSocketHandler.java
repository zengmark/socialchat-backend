package com.socialchat.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UploadWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        log.info("WebSocket 连接已建立，Session ID: {}", session.getId());
//        String sessionId = session.getId();
//        sessions.put(sessionId, session); // 存储 WebSocket 会话
        log.info("WebSocket 连接建立");
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 接收客户端的消息（例如任务绑定等）
        String sessionId = message.getPayload(); // 假设前端发送的是 sessionId 作为纯文本
        log.info("收到 sessionId: {}", sessionId);
        sessions.put(sessionId, session); // 将用户的 sessionId 与 WebSocketSession 关联
        // 可选：发送确认消息给客户端
//        session.sendMessage(new TextMessage("Session ID 已注册: " + sessionId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket 连接关闭");
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }

    public static void sendNotification(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}