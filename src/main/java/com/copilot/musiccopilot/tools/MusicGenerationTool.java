package com.copilot.musiccopilot.tools;

import cn.hutool.core.io.FileUtil;
import com.copilot.musiccopilot.constant.FileConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@Component
@Slf4j
public class MusicGenerationTool {

    private final RestTemplate restTemplate = new  RestTemplate();
    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/music";

    private final String apiKey;

    private static final String COMPOSE_URL = "https://public-api.beatoven.ai/api/v1/tracks/compose";
    private static final String TASK_STATUS_URL_TEMPLATE = "https://public-api.beatoven.ai/api/v1/tasks/%s";

    public MusicGenerationTool(@Value("${spring.ai.beatoven.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Generate a short music clip based on user description")
    public String generateMusic(@ToolParam(description = "User's description about the music") String description) {
        try {
            FileUtil.mkdir(FILE_DIR);

            // Step 1: Trigger composition
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of(
                            "prompt", Map.of("text", description),
                            "format", "mp3",
                            "looping", false
                    ), headers
            );

            ResponseEntity<Map<String, Object>> composeResponse =
                    restTemplate.exchange(
                            COMPOSE_URL,
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<>() {
                            }
                    );
            if (!composeResponse.getStatusCode().is2xxSuccessful()) {
                return "音乐生成请求失败，状态码: " + composeResponse.getStatusCode();
            }

            var taskId = getResponseField(composeResponse, "task_id");
            log.info("Music generation started, taskId={}", taskId);

            // Step 2: Poll status
            String trackUrl = waitForTrackUrl(taskId);
            if (trackUrl == null) {
                return "音乐生成超时或失败。";
            }

            // Step 3: Download MP3
            String fileName = "music_" + System.currentTimeMillis() + ".mp3";
            String filePath = FILE_DIR + "/" + fileName;

            try (InputStream in = URI.create(trackUrl).toURL().openStream();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                in.transferTo(fos);
            }

            return "音乐已生成，保存路径为: " + filePath;

        } catch (Exception e) {
            log.error("生成音乐时出错", e);
            return "生成音乐失败: " + e.getMessage();
        }
    }

    @NotNull
    private static String getResponseField(ResponseEntity<Map<String, Object>> composeResponse, String fieldName) {
        Map<String, Object> body = composeResponse.getBody();
        if (body == null) {
            // Handle null body case
            throw new IllegalStateException("Response body is null");
        }
        Object taskIdObj = body.get(fieldName);
        if (!(taskIdObj instanceof String)) {
            // Handle unexpected type or missing task_id
            throw new IllegalStateException("task_id is missing or not a string");
        }
        return (String) taskIdObj;
    }

    private String waitForTrackUrl(String taskId) throws InterruptedException {
        String statusUrl = String.format(TASK_STATUS_URL_TEMPLATE, taskId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int maxRetries = 20;
        int retryDelayMs = 3000;

        for (int i = 0; i < maxRetries; i++) {
            ResponseEntity<Map<String, Object>> statusResponse = restTemplate.exchange(
                    statusUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            String status = getResponseField(statusResponse,  "status");
            Map<String, Object> body = statusResponse.getBody();

            log.info("Polling task {}, attempt {}: status = {}", taskId, i + 1, status);

            if ("composed".equals(status)) {
                Map<String, Object> meta = (Map<String, Object>) body.get("meta");
                return (String) meta.get("track_url");
            }

            Thread.sleep(retryDelayMs);
        }

        return null; // timeout
    }
}
