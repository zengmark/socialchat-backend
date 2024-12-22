package com.socialchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        // 创建 CORS 配置
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedMethod("*"); // 允许的 HTTP 方法
        config.addAllowedHeader("*"); // 允许的请求头
        config.addAllowedOriginPattern("*"); // 允许的域名，可以改成具体域名
        config.setAllowCredentials(true); // 允许携带 Cookie

        // 注册 CORS 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对所有路径生效
        return new CorsWebFilter(source);
    }
}
