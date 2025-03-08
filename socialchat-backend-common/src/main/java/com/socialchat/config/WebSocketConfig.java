package com.socialchat.config;

import com.socialchat.handler.ChatWebSocketHandler;
import com.socialchat.handler.UploadWebSocketHandler;
import com.socialchat.interceptor.MyHandshakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Bean
    public UploadWebSocketHandler uploadWebSocketHandler() {
        return new UploadWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadWebSocketHandler(), "/ws/upload")
                .addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new MyHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}