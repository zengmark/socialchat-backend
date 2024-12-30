package com.socialchat.scheduler;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PostScheduler implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "https://api.codefather.cn/api/essay/list/page/vo";

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String json = "{\"pageSize\":10,\"sortOrder\":\"descend\",\"sortField\":\"createTime\",\"tags\":[],\"current\":1,\"reviewStatus\":1}";
        RequestBody body = RequestBody.create(mediaType, json);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                log.info("编程导航响应数据：{}", responseBody);

                // 写入文件
                writeToFile(responseBody);
            } else {
                log.error("请求失败，状态码：{}", response.code());
            }
        } catch (IOException e) {
            log.error("请求过程中发生异常", e);
        }
    }

    /**
     * 将给定的内容写入 resources/test.json 文件
     *
     * @param content 要写入文件的内容
     */
    private void writeToFile(String content) {
        // 获取 resources 目录的路径
        // 这里假设在开发环境中运行，resources 目录位于 src/main/resources
        String resourcesPath = Paths.get("src", "main", "resources", "test.json").toString();

        // 创建 File 对象
        File file = new File(resourcesPath);

        // 确保父目录存在
        file.getParentFile().mkdirs();

        // 写入内容到文件
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            log.info("响应数据已成功写入 {}", resourcesPath);
        } catch (IOException e) {
            log.error("写入文件失败", e);
        }
    }
}
