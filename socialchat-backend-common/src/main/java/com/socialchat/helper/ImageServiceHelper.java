package com.socialchat.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialchat.handler.UploadWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class ImageServiceHelper {

    @Value("${gitee.api.url}")
    private String giteeApiUrl;

    @Value("${gitee.repo.owner}")
    private String repoOwner;

    @Value("${gitee.repo.name}")
    private String repoName;

    @Value("${gitee.token}")
    private String token;

    @Resource
    private ExecutorService imageUploadThreadPoolExecutor;

//    private static final OkHttpClient client = new OkHttpClient();
//    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 使用线程池异步化处理
    public CompletableFuture<Void> uploadImageToGiteeAsync(MultipartFile file, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String imageUrl = uploadImageToGitee(file);

                // 上传完成后通过 WebSocket 通知前端
                log.info("上传成功：{}", sessionId);
                UploadWebSocketHandler.sendNotification(sessionId, imageUrl);
            } catch (Exception e) {
                log.error("上传失败");
                // 上传失败通知前端
                UploadWebSocketHandler.sendNotification(sessionId, "上传失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }, imageUploadThreadPoolExecutor);
    }

    public String uploadImageToGitee(MultipartFile file) throws IOException {
        // Read image file and encode it to Base64
        byte[] imageBytes = file.getBytes();
        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

        // Create JSON payload
        Map<String, String> payload = new HashMap<>();
        payload.put("message", "Upload image " + file.getOriginalFilename());
        payload.put("content", encodedImage);
        payload.put("access_token", token);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Create request
        OkHttpClient client = new OkHttpClient();
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonPayload, MediaType.get("application/json"));
        String uploadImgUrl = giteeApiUrl + "/repos/" + repoOwner + "/" + repoName + "/contents/" + UUID.randomUUID().getLeastSignificantBits() + file.getOriginalFilename();
        Request request = new Request.Builder()
                .url(uploadImgUrl)
                .post(body)
                .build();

        // Execute request
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        // Parse response to get image URL
        String responseBody = response.body().string();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        Map<String, Object> contentMap = (Map<String, Object>) responseMap.get("content");
        String imageUrl = (String) contentMap.get("download_url");
        log.info("上传的图片 url 为：{}", imageUrl);
        return imageUrl;
    }

}
