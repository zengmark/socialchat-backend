package com.socialchat.scheduler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.socialchat.model.dto.Record;
import com.socialchat.service.PostService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PostScheduler implements CommandLineRunner {

    @Resource
    private PostService postService;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        System.out.println("执行了");
        XxlJobHelper.log("XXL-JOB, Hello World.");

        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at:" + i);
//            TimeUnit.SECONDS.sleep(2);
        }
        // default success
    }

    @XxlJob("fetchDataJobHandler")
    public void fetchDataJobHandler() throws Exception {
        log.info("开始同步数据");
        try {
            // 初始化 okhttp 客户端
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();

            String url = "https://api.codefather.cn/api/essay/list/page/vo";

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String requestJSONTemplate = "{\"pageSize\":20,\"sortOrder\":\"descend\",\"sortField\":\"createTime\",\"tags\":[],\"current\":%d,\"reviewStatus\":1}";
            int current = 1;
            while (true) {
                // 构造本次请求请求体
                String requestJSON = String.format(requestJSONTemplate, current);
                RequestBody body = RequestBody.create(mediaType, requestJSON);

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        .build();

                // 发起请求
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = JSON.parseObject(responseBody);
                        log.info("编程导航响应数据:{}", jsonObject);

                        // 解析数据
                        JSONArray recordArray = jsonObject.getJSONObject("data").getJSONArray("records");
                        List<Record> recordList = recordArray.toJavaList(Record.class);
                        if (CollectionUtils.isEmpty(recordList)) {
                            break;
                        }

                        // 保存数据
                        boolean flag = postService.savePostBySchedule(recordList);
                        if (!flag) {
                            break;
                        }

                        // 写入文件
//                        writeToFile(responseBody);
                    } else {
                        log.error("请求失败，状态码:{}", response.code());
                    }
                } catch (IOException | JSONException e) {
                    log.error("请求过程中发生异常", e);
                }
                current++;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e);
        } finally {
            log.info("停止同步数据");
        }
    }

    public void fetchData() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "https://api.codefather.cn/api/essay/list/page/vo";

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String requestJSONTemplate = "{\"pageSize\":20,\"sortOrder\":\"descend\",\"sortField\":\"createTime\",\"tags\":[],\"current\":%d,\"reviewStatus\":1}";
        for (int i = 0; i < 1; i++) {
            String requestJSON = String.format(requestJSONTemplate, i + 1);
            RequestBody body = RequestBody.create(mediaType, requestJSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = JSON.parseObject(responseBody);
                    log.info("编程导航响应数据：{}", jsonObject);

                    initData(jsonObject);

                    // 写入文件
                    writeToFile(responseBody);
                } else {
                    log.error("请求失败，状态码：{}", response.code());
                }
            } catch (IOException | JSONException e) {
                log.error("请求过程中发生异常", e);
            }
        }
    }

    @Override
    public void run(String... args) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        String url = "https://api.codefather.cn/api/essay/list/page/vo";

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String requestJSONTemplate = "{\"pageSize\":10,\"sortOrder\":\"descend\",\"sortField\":\"createTime\",\"tags\":[],\"current\":%d,\"reviewStatus\":1}";
        for (int i = 0; i < 1; i++) {
            String requestJSON = String.format(requestJSONTemplate, i + 1);
            RequestBody body = RequestBody.create(mediaType, requestJSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(responseBody);
                    log.info("编程导航响应数据：{}", jsonObject);

                    initData(jsonObject);

                    // 写入文件
                    writeToFile(responseBody);
                } else {
                    log.error("请求失败，状态码：{}", response.code());
                }
            } catch (IOException e) {
                log.error("请求过程中发生异常", e);
            }
        }
    }

    /**
     * 初始化数据，向数据库和 ES 中插入数据
     *
     * @param jsonObject
     */
    private void initData(JSONObject jsonObject) {

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
