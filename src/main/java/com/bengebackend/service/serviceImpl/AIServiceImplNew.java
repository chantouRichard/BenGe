package com.bengebackend.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bengebackend.entity.AIMsgDevide;
import com.bengebackend.entity.SloganRequestEntity;
import com.bengebackend.entity.Slogan;
import com.bengebackend.service.*;
import com.bengebackend.config.XfyunConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service("AIServiceImplNew")
public class AIServiceImplNew implements AIService {

    private final XfyunConfig xfyunConfig;

    @SuppressWarnings("unused")
    private final RestTemplate restTemplate;
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public AIServiceImplNew(RestTemplate restTemplate, ObjectMapper objectMapper, XfyunConfig xfyunConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.xfyunConfig = xfyunConfig;
    }

    // ====================== 单人创作和多人创作AI部分分割线 ==================

    @Override
    public CompletableFuture<AIMsgDevide> GenFramework(List<Map<String, String>> msgs) {
        msgs.add(0, new HashMap<String, String>() {
            {
                put("role", "system");
                put("content", XfyunConfig.SYSTEM_MSG);
            }
        });

        // 获取API输出
        return xfyunConfig.GetAPIOutputAsync(msgs, "x1")
                .thenApply(content -> {
                    AIMsgDevide devidedMsg = new AIMsgDevide();
                    xfyunConfig.DevideScriptContent(devidedMsg, content, true);
                    return devidedMsg;
                });
    }

    @Override
    public CompletableFuture<String> GenDetail(String Frame, String Title) {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(new HashMap<String, String>() {
            {
                put("role", "system");
                put("content", XfyunConfig.GEN_DETAIL_SYS_PROMPT[0]);
            }
        });
        msgs.add(new HashMap<String, String>() {
            {
                put("role", "user");
                put("content", Frame + XfyunConfig.GEN_DETAIL_USER_PROMPT[0]);
            }
        });

        // 获取API输出
        return xfyunConfig.GetAPIOutputAsync(msgs, "x1");
    }

    @Override
    public CompletableFuture<String> AnalyzeScriptContent(String StrScript) {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(new HashMap<String, String>() {
            {
                put("role", "system");
                put("content", XfyunConfig.ANALYZE_PROMPT);
            }
        });
        msgs.add(new HashMap<String, String>() {
            {
                put("role", "user");
                put("content", StrScript);
            }
        });

        // 获取API输出
        return xfyunConfig.GetAPIOutputAsync(msgs, "4.0Ultra");
    }

    @Override
    public CompletableFuture<List<List<List<String>>>> GetThreeTypesOfDesc(String strScript) {
        List<CompletableFuture<List<List<String>>>> allFutures = new ArrayList<>();

        // 1. 生成消息列表
        List<List<Map<String, String>>> messageList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<Map<String, String>> msgs = new ArrayList<>();
            msgs.add(new HashMap<String, String>() {
                {
                    put("role", "system");
                    put("content", XfyunConfig.GET_DESC_PROMPT[i]);
                }
            });
            msgs.add(new HashMap<String, String>() {
                {
                    put("role", "user");
                    put("content", strScript);
                }
            });
            messageList.add(msgs);
        }

        // 2. 分批处理，每批10个避免并发过多
        int batchSize = 10;
        for (int i = 0; i < messageList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, messageList.size());
            List<List<Map<String, String>>> batch = messageList.subList(i, end);

            // 处理当前批次
            List<CompletableFuture<List<List<String>>>> batchFutures = batch.stream()
                    .map(msgs -> xfyunConfig.ParseAIRespOfGetDesc(xfyunConfig.GetAPIOutputAsync(msgs, "4.0Ultra")))
                    .collect(Collectors.toList());

            // 等待当前批次完成
            CompletableFuture<Void> batchDone = CompletableFuture.allOf(
                    batchFutures.toArray(new CompletableFuture[0]));

            // 添加到总列表
            batchFutures.forEach(allFutures::add);

            // 等待当前批次完成再继续下一批
            batchDone.join();
        }

        // 3. 合并所有结果
        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> allFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
        // 返回结果示例：
        // [[[a,b,c(角色名)], [aa,bb,cc]], [[d, e, f(场景名)], [dd,ee,ff]], [[g,h,i(道具名)],
        // [gg,hh,ii]]]
    }

    @Override
    public CompletableFuture<String> GenImage(String Description) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> requestData = new HashMap<>();

                // 构建header
                Map<String, Object> header = new HashMap<>();
                header.put("app_id", xfyunConfig.getAppid_wwb());
                requestData.put("header", header);

                // 构建parameter
                Map<String, Object> parameter = new HashMap<>();
                Map<String, Object> chat = new HashMap<>();
                chat.put("domain", "general");
                chat.put("width", 512);
                chat.put("height", 512);
                parameter.put("chat", chat);
                requestData.put("parameter", parameter);

                // 构建payload
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> message = new HashMap<>();
                List<Map<String, Object>> textList = new ArrayList<>(); // 使用List而不是Map
                Map<String, Object> textItem = new HashMap<>(); // 创建text数组中的项
                textItem.put("role", "user");
                textItem.put("content", Description);
                textList.add(textItem);
                message.put("text", textList);
                payload.put("message", message);
                requestData.put("payload", payload);

                // 生成带签名的URL
                String requestUrl = xfyunConfig.CreateSignedUrl(xfyunConfig.getGenImageRequestURL());
                System.out.println("请求URL: " + requestUrl);
                // 异步发送请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .header("Content-Type", "application/json")
                        .header("app_id", xfyunConfig.getAppid_wwb())
                        .POST(HttpRequest.BodyPublishers.ofString(new JSONObject(requestData).toString()))
                        .build();

                // 发送异步请求并处理响应
                return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> {
                            String responseBody = response.body();
                            System.out.println("API响应: " + responseBody);

                            if (response.statusCode() != 200) {
                                throw new RuntimeException("HTTP请求失败: " + response.statusCode() + " " + responseBody);
                            }

                            JSONObject resp = new JSONObject(responseBody);

                            return resp
                                    .getJSONObject("payload")
                                    .getJSONObject("choices")
                                    .getJSONArray("text")
                                    .getJSONObject(0)
                                    .getString("content");

                        }).join();
            } catch (Exception e) {
                throw new RuntimeException("创建任务失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> GenerateSloganStreamAsync(SloganRequestEntity request, Consumer<String> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 构建请求消息
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", XfyunConfig.SLOGAN_SYSTEM_PROMPT));
                messages.add(Map.of("role", "user", "content", request.getPrompt()));

                // 执行流式请求
                executeStreamRequest(messages, content -> {
                    // 处理内容并生成Slogan对象
                    if (content != null && !content.trim().isEmpty()) {
                        // String coreIdea = extractCoreIdea(content);
                        // Slogan slogan = new Slogan(content.trim(), coreIdea);
                        callback.accept(content);
                    }
                });

            } catch (Exception e) {
                System.err.println("生成Slogan流式输出时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<Slogan>> GenerateSloganAsync(SloganRequestEntity request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求消息
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", XfyunConfig.SLOGAN_SYSTEM_PROMPT));
                messages.add(Map.of("role", "user", "content", request.getPrompt()));

                // 执行非流式请求
                CompletableFuture<String> future = xfyunConfig.GetAPIOutputAsync(messages, "x1");
                String response = future.get();

                // 解析响应内容为Slogan对象数组
                return parseSloganResponse(response);

            } catch (Exception e) {
                System.err.println("生成Slogan时发生错误: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 解析AI响应内容为Slogan对象列表
     */
    private List<Slogan> parseSloganResponse(String response) {
        List<Slogan> slogans = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            return slogans;
        }

        // 按行分割响应内容
        String[] lines = response.split("\n");
        String currentSlogan = "";
        String currentCoreIdea = "";
        boolean isSlogan = false;
        boolean isCoreIdea = false;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("# 标语")) {
                // 如果之前有完整的标语和核心创意，保存它
                if (!currentSlogan.isEmpty() && !currentCoreIdea.isEmpty()) {
                    slogans.add(new Slogan(currentSlogan, currentCoreIdea));
                }
                // 重置状态
                currentSlogan = "";
                currentCoreIdea = "";
                isSlogan = true;
                isCoreIdea = false;
            } else if (line.startsWith("# 核心创意")) {
                isSlogan = false;
                isCoreIdea = true;
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                // 收集内容
                if (isSlogan) {
                    if (!currentSlogan.isEmpty()) {
                        currentSlogan += "\n" + line;
                    } else {
                        currentSlogan = line;
                    }
                } else if (isCoreIdea) {
                    if (!currentCoreIdea.isEmpty()) {
                        currentCoreIdea += "\n" + line;
                    } else {
                        currentCoreIdea = line;
                    }
                }
            }
        }

        // 保存最后一个标语
        if (!currentSlogan.isEmpty() && !currentCoreIdea.isEmpty()) {
            slogans.add(new Slogan(currentSlogan, currentCoreIdea));
        }

        // 如果解析失败或数量不足，返回默认的3个标语
        if (slogans.size() < 3) {
            while (slogans.size() < 3) {
                slogans.add(new Slogan("精彩剧本等你来体验", "沉浸式角色扮演，解锁真相"));
            }
        }

        // 只返回前3个标语
        return slogans.subList(0, Math.min(3, slogans.size()));
    }

    @Override
    public CompletableFuture<Void> ChatStreamAsync(List<Map<String, String>> messages, Consumer<String> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                executeStreamRequest(messages, callback);
            } catch (Exception e) {
                System.err.println("AI助手流式聊天时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 执行流式请求的核心方法 - 改进版使用实际的HTTP流式处理
     */
    private void executeStreamRequest(List<Map<String, String>> messages, Consumer<String> callback) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 构建请求体
            String requestBody = mapper.writeValueAsString(Map.of(
                    "model", "x1",
                    "user", "user_id",
                    "messages", messages,
                    "stream", true,
                    "temperature", 0.7,
                    "top_p", 0.9,
                    "presence_penalty", 4.0,
                    "max_tokens", 32768));

            // 使用HttpURLConnection进行流式处理
            java.net.URL url = new java.net.URL(xfyunConfig.getX1HttpApiUrl());
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + xfyunConfig.getX1HttpApiPassword());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);

            // 发送请求数据
            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 读取流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    processStreamLine(line, callback, mapper);
                }
            }

        } catch (Exception e) {
            System.err.println("执行流式请求时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理单行流式响应
     */
    private void processStreamLine(String line, Consumer<String> callback, ObjectMapper mapper) {
        callback.accept(line);
        // if (line.startsWith("data:")) {
        // String jsonData = line.substring(5).trim();

        // // 检查是否是结束标记
        // if ("[DONE]".equals(jsonData)) {
        // return;
        // }

        // try {
        // // 解析JSON响应
        // StreamResponse streamResponse = mapper.readValue(jsonData,
        // StreamResponse.class);

        // // 检查错误码
        // if (streamResponse.getCode() != 0) {
        // System.err.println("API返回错误: " + streamResponse.getMessage());
        // return;
        // }

        // // 提取流式数据
        // if (streamResponse.getChoices() != null &&
        // !streamResponse.getChoices().isEmpty()) {
        // StreamChoice choice = streamResponse.getChoices().get(0);
        // if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
        // callback.accept(choice.getDelta().getContent());
        // }
        // }

        // } catch (Exception e) {
        // // 忽略解析错误，继续处理下一行
        // }
        // }
    }

    /**
     * 生成框架流式响应
     */
    @Override
    public CompletableFuture<AIMsgDevide> GenFrameworkStream(List<Map<String, String>> msgs,
            Consumer<String> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                msgs.add(0, new HashMap<String, String>() {
                    {
                        put("role", "system");
                        put("content", XfyunConfig.SYSTEM_MSG);
                    }
                });
                // 构建消息数组
                JSONArray messages = new JSONArray();
                for (Map<String, String> msg : msgs) {
                    messages.put(new JSONObject()
                            .put("role", msg.get("role"))
                            .put("content", msg.get("content")));
                }

                // 创建请求连接
                URL url = new URL(xfyunConfig.getX1Url());// X1URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + xfyunConfig.getX1APIPassword());
                conn.setDoOutput(true);

                // 发送请求体
                try (OutputStream os = conn.getOutputStream()) {
                    JSONObject body = new JSONObject()
                            .put("user", "user_id")
                            .put("model", "x1")
                            .put("stream", true)
                            .put("max_tokens", 32768)
                            .put("messages", messages);
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                String StrScript = processStreamResponse(conn, callback);
                AIMsgDevide devidedMsg = new AIMsgDevide();
                xfyunConfig.DevideScriptContent(devidedMsg, StrScript, true);
                return devidedMsg;
            } catch (Exception e) {
                throw new RuntimeException("API请求失败", e);
            }
        });
    }

    private String processStreamResponse(HttpURLConnection conn, Consumer<String> callback) throws Exception {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("API响应错误: " + responseCode);
        }

        StringBuilder fullContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("[DONE]"))
                    break;
                if (line.isBlank() || line.isEmpty())
                    continue;
                callback.accept(line);
                String block = line;
                try {
                    JSONObject json = new JSONObject(block.substring(5));
                    json = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
                    if (!json.has("content")) {
                        continue;
                    } else {
                        block = json.getString("content");
                        fullContent.append(block);
                    }
                } catch (Exception e) {
                    System.err.print(".");
                }
            }
        }
        return fullContent.toString();
    }
}
