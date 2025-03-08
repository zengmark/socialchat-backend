package com.socialchat.interceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class MyHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
        HttpServletRequest servletRequest = serverRequest.getServletRequest();
        // 从 URL 查询参数中获取 userId 和 roomId
        String userId = servletRequest.getParameter("userId");
        String roomId = servletRequest.getParameter("roomId");

        // 将 userId 和 roomId 添加到 WebSocketSession 的 attributes 中
        if (userId != null) {
            attributes.put("userId", Long.parseLong(userId));
        }
        if (roomId != null) {
            attributes.put("roomId", Long.parseLong(roomId));
        }

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}