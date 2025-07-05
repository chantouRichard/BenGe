package com.bengebackend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
public class SparkApiUtil {

    @Value("DdlQeCkcZDLAokJMOAaF:xaDOtCuGjiJIEsyptVzs")
    private String apiPassword;

    private static final String API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private static final RestTemplate restTemplate = new RestTemplate();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String userPrompt) {
        try {
            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiPassword);

            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "x1");
            requestBody.put("user", UUID.randomUUID().toString());
            requestBody.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "user",
                    "content", userPrompt
            ));
            requestBody.put("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    return choices.get(0).get("message").get("content").asText();
                }
            }

            log.warn("星火模型返回内容无法解析: {}", response.getBody());
            return "模型未返回有效内容";

        } catch (Exception e) {
            log.error("星火模型调用失败", e);
            return "调用失败: " + e.getMessage();
        }
    }
}