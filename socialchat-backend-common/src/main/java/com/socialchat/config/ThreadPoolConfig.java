package com.socialchat.config;

import com.socialchat.common.ImageThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "imageUploadThreadPoolExecutor")
    public ThreadPoolExecutor imageUploadExecutor() {
        return new ThreadPoolExecutor(
                5, // 核心线程数
                20, // 最大线程数
                60L, // 空闲线程最大存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // 队列容量
                new ImageThreadFactory("imageUpload"),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }

}
