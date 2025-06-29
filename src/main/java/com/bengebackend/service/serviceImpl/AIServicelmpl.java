package com.bengebackend.service.serviceImpl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.bengebackend.entity.AIStruct.*;

import com.bengebackend.entity.AIAnalysisResult;
import com.bengebackend.entity.ImageParameters;
import com.bengebackend.entity.AIMsgDevide;
import com.bengebackend.entity.AIStruct.*;
import com.bengebackend.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import okhttp3.*;

@Service
public class AIServicelmpl implements AIService {
    @Autowired
    private final OkHttpClient _httpClient;

    @Autowired
    private final OkHttpClient _client;

    @Autowired
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private static final String API_KEY = "ZxztQfIySwHMqrLvRLGa:gZdZClyqiPxeoHVTZQbO";

    private static final String API_BASE_URL = "https://api.deepseek.com/";
    private static final String PICTURE_API_KEY = "sk-817d27e9c883406a87d98eb23fc4b1a9";
    private static final String PICTURE_URL = "https://dashscope.aliyuncs.com";
    private final ExecutorService executorService;

    public AIServicelmpl(OkHttpClient client) {
        // Stage 1 - Configure first client
        this._client = client.newBuilder()
                .addInterceptor(chain -> {
                    HttpUrl newUrl = HttpUrl.parse("https://api.chatanywhere.tech")
                            .newBuilder()
                            .addPathSegments(chain.request().url().encodedPath().substring(1))
                            .build();

                    Request newRequest = chain.request().newBuilder()
                            .url(newUrl)
                            .header("Authorization", "Bearer sk-itFhufoh4wQKGkvyfiFE7BjyzL4aZAjMMqJIKQZWAwsULDm9")
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();

        // Configure Jackson ObjectMapper
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .setSerializationInclusion(JsonInclude.Include.ALWAYS)
                .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        // Stage 2 - Create second client
        this._httpClient = new OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.MINUTES)
                .readTimeout(6, TimeUnit.MINUTES)
                .writeTimeout(6, TimeUnit.MINUTES)
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .header("Authorization", "Bearer sk-8dbe5de96f5644849250648772aff5e2")
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    public CompletableFuture<AIMsgDevide> GenFramework(List<Map<String, String>> msgs) {
        msgs.add(new HashMap<String, String>() {
            {
                put("role", "system");
                put("content", System_MSG);
            }
        });

        // 获取API输出
        return GetAPIOutputAsync(msgs)
                .thenApply(content -> {
                    AIMsgDevide devidedMsg = new AIMsgDevide();
                    DevideScriptContent(devidedMsg, content, true);
                    return devidedMsg;
                });
    }

    public static void DevideScriptContent(AIMsgDevide devidedMsg, String content, boolean setReplyAndTitle) {
        String[] parts;
        if (setReplyAndTitle) {
            parts = content.split("》》》", 2);
            if (parts.length == 2)
                devidedMsg.setMsgForUser(parts[0]);
            else
                System.out.println("未找到分隔符》》》，回答与剧本内容分割失败");

            parts = content.split("#", 2);
            parts = parts[1].split("---", 2);
            devidedMsg.setTitle(parts[0].replaceAll("\\s+", ""));
            if (parts.length != 2)
                System.out.println("未找到分隔符---，分割失败");
            devidedMsg.setStrScript(parts[1]);
        } else {
            devidedMsg.setStrScript(content);
        }

        parts = content.split("## 背景\\s*", 2);
        parts = parts[1].split("---", 2);
        devidedMsg.setBackground(parts[0]);
        if (parts.length != 2)
            System.out.println("未找到分隔符 ## 背景 ，分割失败");

        parts = parts[1].split("## 人物剧本", 2);
        parts = parts[1].split("---", 2);
        String temp_str[] = parts[0].split("-CHR");
        if (temp_str.length == 1)
            System.out.println("未找到分隔符 -CHR ，分割失败");
        devidedMsg.setChrScript(new ArrayList<>(Arrays.asList(temp_str).subList(1, temp_str.length)));

        parts = parts[1].split("## 线索", 2);
        parts = parts[1].split("---", 2);
        temp_str = parts[0].split("-C>");
        if (temp_str.length == 1)
            System.out.println("未找到分隔符 -C> ，分割失败");
        devidedMsg.setClues(new ArrayList<>(Arrays.asList(temp_str).subList(1, temp_str.length)));

        parts = parts[1].split("## 真相", 2);
        parts = parts[1].split("---", 2);
        devidedMsg.setTrues(parts[0]);
        if (parts.length != 2)
            System.out.println("未找到分隔符 --- ，分割失败");

        parts = parts[1].split("## 组织者手册", 2);
        parts = parts[1].split("---", 2);
        devidedMsg.setGoBook(parts[0]);
    }

    @Override
    public static CompletableFuture<String> GenDetail(String Frame, String Title) {
        AIMsgDevide devidedMsg = new AIMsgDevide(Title, Frame);
        DevideScriptContent(devidedMsg, Frame, false);
        List<List<Map<String, String>>> messagesList = new ArrayList<>(); // 0背景 4-n人物剧本 1线索 2真相 3组织者手册
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", GenDetailSysPrompt[0]));
        messages.add(Map.of(
                "role", "user",
                "content", devidedMsg.getStrScript() + "\n\n" + GenDetailUserPrompt[0]));
        messagesList.add(messages);
        for (int i = 0; i < devidedMsg.getChrScript().size(); i += 1) {
            messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", GenDetailSysPrompt[1]));
            messages.add(Map.of(
                    "role", "user",
                    "content", devidedMsg.getStrScript() + "\n\n" + GenDetailUserPrompt[1] + "\n\n"
                            + devidedMsg.getChrScript().get(i)));
            messagesList.add(messages);
        }
        for (int i = 2; i < 5; i += 1) {
            messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", GenDetailSysPrompt[i]));
            messages.add(Map.of(
                    "role", "user",
                    "content", devidedMsg.getStrScript() + "\n\n" + GenDetailUserPrompt[i]));
            messagesList.add(messages);
        }

        return CompletableFuture.supplyAsync(() -> {
            StringBuilder resultBuilder = new StringBuilder();
            AtomicInteger addTime = new AtomicInteger(0);
            // 分批处理
            for (int i = 0; i < messagesList.size(); i += 2) {
                int end = Math.min(i + 2, messagesList.size());
                List<List<Map<String, String>>> batch = messagesList.subList(i, end);

                // 并行处理当前批次
                List<CompletableFuture<String>> futures = batch.stream()
                        .map(msgs -> GetAPIOutputAsync(msgs, "x1"))
                        .collect(Collectors.toList());

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 收集当前批次结果
                futures.forEach(future -> {
                    try {
                        resultBuilder.append(future.join());
                        if (addTime.get() == 0) {
                            resultBuilder.append("\n---\n## 人物剧本\n");
                        }
                        if (addTime.get() != 0 && addTime.get() < devidedMsg.getChrScript().size()) {
                            resultBuilder.append("\n\n");
                        } else {
                            resultBuilder.append("\n---\n");
                        }
                        addTime.incrementAndGet();
                    } catch (CompletionException e) {
                        resultBuilder.append("[Error: ").append(e.getCause().getMessage()).append("]");
                    }
                });
            }
            return resultBuilder.toString();
        });
    }

    @Override
    public CompletableFuture<String> GetAPIOutputAsync(List<Map<String, String>> msgs) {
        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(Map.of(
                    "model", "x1",
                    "user", "user_id",
                    "messages", msgs,
                    "stream", false,
                    "tools", List.of(Map.of(
                            "type", "web_search",
                            "web_search", Map.of(
                                    "enable", true,
                                    "search_mode", "deep")))));
        } catch (JsonProcessingException e) {
            System.err.println("JSON处理失败: " + e.getMessage());
            return null;
        }

        // 2. 创建 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("content-type", "application/json")
                .POST(BodyPublishers.ofString(requestBody))
                .build();

        // 3. 发送请求
        HttpClient client = HttpClient.newHttpClient();
        return client.sendAsync(request, BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    // 4. 处理响应
                    try {
                        JsonNode rootNode = mapper.readTree(response.body());

                        // 检查错误码
                        if (rootNode.has("code") && rootNode.get("code").asInt() != 0) {
                            String errorMsg = rootNode.get("message").asText();
                            System.err.println("API请求失败: " + errorMsg);
                            throw new RuntimeException("API Error: " + errorMsg);
                        }

                        // 提取content内容
                        return rootNode.path("choices")
                                .path(0)
                                .path("message")
                                .path("content")
                                .asText();
                    } catch (JsonProcessingException e) {
                        System.err.println("JSON解析失败: " + e.getMessage());
                        throw new RuntimeException("JSON Parse Error", e);
                    }
                })
                .exceptionally(e -> {
                    System.err.println("请求过程中发生异常: " + e.getMessage());
                    return null; // 或者返回默认值/抛出特定异常
                });
    }

    @Override
    public CompletableFuture<String> GetV3Output(List<Message> msgs) {
        Supplier<CompletableFuture<String>> asyncTask = () -> {
            try {
                // 2. 准备请求（同步部分）
                String json = objectMapper.writeValueAsString(
                        new ApiRequest("deepseek-chat",
                                msgs.stream()
                                        .map(m -> new Message(m.getRole(), m.getContent()))
                                        .collect(Collectors.toList()),
                                8192));

                // 3. 创建异步HTTP调用
                CompletableFuture<Response> responseFuture = new CompletableFuture<>();
                _httpClient.newCall(
                        new Request.Builder()
                                .url(API_BASE_URL + "chat/completions")
                                .post(RequestBody.create(json, MediaType.get("application/json")))
                                .build())
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                responseFuture.completeExceptionally(e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) {
                                responseFuture.complete(response);
                            }
                        });

                // 4. 处理响应（异步部分）
                return responseFuture.thenApply(response -> {
                    try (response) {
                        String body = response.body().string();
                        return objectMapper.readValue(body, DeepSeekResponse.class)
                                .getChoices().get(0).getMes().getContent()
                                .replaceAll("^`+|`+$", "")
                                .replaceAll("^json\\s*", "")
                                .trim();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });

            } catch (JsonProcessingException e) {
                throw new CompletionException(e);
            }
        };

        // 5. 显式指定类型参数并提交任务
        return CompletableFuture.supplyAsync(asyncTask, executorService)
                .thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<String> AnalyzeScriptContent(String scriptContent) {
        // 1. 准备消息列表
        List<Message> msgs = List.of(
                new Message("system", AnalyzePrompt),
                new Message("user", scriptContent));

        // 2. 链式处理异步结果
        return GetApiOutput(msgs)
                .thenApply(apiResponse -> {
                    try {
                        // 3. 反序列化结果
                        AIAnalysisResult analysisResult = objectMapper.readValue(apiResponse, AIAnalysisResult.class);
                        // 4. 转换为Markdown
                        return ConvertToMarkdown(analysisResult);
                    } catch (JsonProcessingException e) {
                        throw new CompletionException("Failed to parse API response", e);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("分析失败: " + ex.getMessage());
                    return "分析失败: " + ex.getMessage();
                });
    }

    @Override
    public String ConvertToMarkdown(AIAnalysisResult result) {
        if (result == null) {
            return "无分析结果";
        }

        StringBuilder markdown = new StringBuilder();

        // 1. 添加标题
        markdown.append("## 游戏脚本分析报告\n\n");

        // 2. 亮点部分
        markdown.append("### ✨ 亮点\n");
        if (result.getPoint() != null && !result.getPoint().isEmpty()) {
            for (String point : result.getPoint()) {
                markdown.append("- ").append(point).append("\n");
            }
        } else {
            markdown.append("- 无特别亮点\n");
        }
        markdown.append("\n");

        // 3. 难点部分
        markdown.append("### ⚠️ 难点\n");
        if (result.getDifficulty() != null && !result.getDifficulty().isEmpty()) {
            for (String difficulty : result.getDifficulty()) {
                markdown.append("- ").append(difficulty).append("\n");
            }
        } else {
            markdown.append("- 无明显难点\n");
        }
        markdown.append("\n");

        // 4. 改进建议
        markdown.append("### 💡 改进建议\n");
        if (result.getSuggestion() != null && !result.getSuggestion().isEmpty()) {
            for (String suggestion : result.getSuggestion()) {
                markdown.append("- ").append(suggestion).append("\n");
            }
        } else {
            markdown.append("- 无改进建议\n");
        }
        markdown.append("\n");

        // 5. 评分部分（表格形式）
        markdown.append("### ⭐ 综合评分\n");
        markdown.append("| 维度 | 评分（满分100） |\n");
        markdown.append("|------|--------------|\n");
        markdown.append("| 逻辑性 | ").append(result.getLogicality() != -1 ? result.getLogicality() : 0).append(" |\n");
        markdown.append("| 故事性 | ").append(result.getStoriness() != -1 ? result.getStoriness() : 0).append(" |\n");
        markdown.append("| 体验感 | ").append(result.getExperience() != -1 ? result.getExperience() : 0).append(" |\n");
        markdown.append("\n");

        // 6. 添加总结
        double avgScore = (result.getLogicality() +
                result.getStoriness() +
                result.getExperience()) / 3.0;
        markdown.append(String.format("**综合平均分：%.1f/100**", avgScore));

        return markdown.toString();
    }

    @Override
    public CompletableFuture<ImageMsgs> GenDescription(String finalScript) {
        // 准备三个不同的消息列表
        List<Message> chrMsgs = new ArrayList<>();
        chrMsgs.add(new Message("system", DescChr));
        chrMsgs.add(new Message("user", finalScript + "\n\n" + "请帮我描述并补充以上剧本中的人物外貌，并以json的格式输出"));

        List<Message> sceneMsgs = new ArrayList<>();
        sceneMsgs.add(new Message("system", DescScene));
        sceneMsgs.add(new Message("user", finalScript + "\n\n" + "请帮我提取并补充以上剧本中的场景描绘，并以json的格式输出"));

        List<Message> propMsgs = new ArrayList<>();
        propMsgs.add(new Message("system", DescProp));
        propMsgs.add(new Message("user", finalScript + "\n\n" + "请帮我提取并补充以上剧本对关键道具的外观描写，并以json的格式输出"));

        // 创建三个异步任务
        CompletableFuture<String> chrTask = GetV3Output(chrMsgs);
        CompletableFuture<String> sceneTask = GetV3Output(sceneMsgs);
        CompletableFuture<String> propTask = GetV3Output(propMsgs);

        // 使用ObjectMapper进行JSON反序列化
        ObjectMapper objectMapper = new ObjectMapper();

        // 合并所有任务
        return CompletableFuture.allOf(chrTask, sceneTask, propTask)
                .thenApplyAsync(ignored -> {
                    try {
                        ThingDesc chrDesc = objectMapper.readValue(chrTask.join(), ThingDesc.class);
                        ThingDesc sceneDesc = objectMapper.readValue(sceneTask.join(), ThingDesc.class);
                        ThingDesc propDesc = objectMapper.readValue(propTask.join(), ThingDesc.class);

                        List<ThingDesc> allDesc = new ArrayList<>();
                        allDesc.add(chrDesc);
                        allDesc.add(sceneDesc);
                        allDesc.add(propDesc);

                        return new ImageMsgs(allDesc);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("API请求失败: " + ex.getCause().getMessage());
                    return null;
                });
    }

    @Override
    public CompletableFuture<String> GenerateImageUrl(String type, String name, String imageDesc) {
        CompletableFuture<String> result;

        switch (type) {
            case "Character":
                result = GenerateImagesAsync(imageDesc + "  写实主义", null)
                        .thenApply(strtemp -> {
                            if (strtemp == null || strtemp.isEmpty()) {
                                return null;
                            }
                            return strtemp;
                        });
                break;
            case "Scene":
                result = GenerateImagesAsync(name + "：" + imageDesc, "不要生成人物、人、人类、男人、女人、小孩、脸、行人")
                        .thenApply(strtemp -> {
                            if (strtemp == null || strtemp.isEmpty()) {
                                return null;
                            }
                            return strtemp;
                        });
                break;
            default:
                result = GenerateImagesAsync(name + "：" + imageDesc, "不要生成人物、人、人类、男人、女人、小孩、脸、行人")
                        .thenApply(strtemp -> {
                            if (strtemp == null || strtemp.isEmpty()) {
                                return null;
                            }
                            return strtemp;
                        });
                break;
        }

        return result;
    }

    @Override
    public CompletableFuture<String> GenerateImagesAsync(String prompt, String negativePrompt) {
        String size = "1024*1024";
        int n = 1;
        ImageParameters parameters = new ImageParameters();
        parameters.setSize(size);
        parameters.setN(n < 1 ? 1 : (n > 4) ? 4 : n);
        parameters.setPromptExtend(true);
        parameters.setWatermark(false);

        return SubmitImageGenerationTask(prompt, negativePrompt, parameters)
                .thenCompose(taskId -> WaitForTaskCompletion(taskId, 3, 5)
                        .thenApply(imageUrls -> {
                            if (imageUrls == null || imageUrls.isEmpty()) {
                                throw new RuntimeException("未获取到任何图片URL");
                            }
                            return imageUrls.get(0);
                        }));
    }

    @Override
    public CompletableFuture<String> SubmitImageGenerationTask(String prompt, String negativePrompt,
            ImageParameters p) {
        String apiUrl = PICTURE_URL + "/api/v1/services/aigc/text2image/image-synthesis";

        // 构建请求数据对象
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("model", "wanx2.1-t2i-turbo");

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        input.put("negative_prompt", negativePrompt == null || negativePrompt.isEmpty() ? null : negativePrompt);
        requestData.put("input", input);

        requestData.put("parameters", p);

        try {
            // 使用Jackson序列化为JSON（驼峰命名）
            ObjectMapper mapper = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
            String requestBody = mapper.writeValueAsString(requestData);

            // 创建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + PICTURE_API_KEY)
                    .header("X-DashScope-Async", "enable")
                    .header("Accept", "application/json")
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            // 发送异步请求
            return HttpClient.newHttpClient()
                    .sendAsync(request, BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                JsonNode root = mapper.readTree(response.body());
                                JsonNode output = root.path("output");
                                if (!output.isMissingNode()) {
                                    String taskId = output.path("task_id").asText();
                                    if (taskId != null && !taskId.isEmpty()) {
                                        return taskId;
                                    }
                                }
                                throw new RuntimeException("无法从响应中获取任务ID");
                            } catch (IOException e) {
                                throw new RuntimeException("解析响应失败", e);
                            }
                        } else {
                            throw new RuntimeException("API请求失败，状态码: " + response.statusCode());
                        }
                    });
        } catch (Exception e) {
            CompletableFuture<String> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    @Override
    public CompletableFuture<List<String>> WaitForTaskCompletion(String taskId, int maxRetries, int delaySeconds) {
        String apiUrl = PICTURE_URL + "/api/v1/tasks/" + taskId;
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + PICTURE_API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

        return CompletableFuture.supplyAsync(() -> {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < maxRetries; i++) {
                try {
                    System.out.print(".");
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        JsonNode root = mapper.readTree(response.body());
                        JsonNode output = root.path("output");

                        if (!output.isMissingNode()) {
                            String status = output.path("task_status").asText();

                            if ("SUCCEEDED".equals(status)) {
                                List<String> imageUrls = new ArrayList<>();
                                JsonNode results = output.path("results");
                                if (results.isArray()) {
                                    for (JsonNode result : results) {
                                        String url = result.path("url").asText();
                                        if (url != null && !url.isEmpty()) {
                                            imageUrls.add(url);
                                        }
                                    }
                                }
                                if (!imageUrls.isEmpty()) {
                                    return imageUrls;
                                }
                                throw new RuntimeException("任务成功但未获取到任何图片URL");
                            } else if ("FAILED".equals(status)) {
                                String code = output.path("code").asText();
                                String message = output.path("message").asText();
                                throw new RuntimeException("任务处理失败\ncode: " + code + "\nmessage: " + message);
                            }
                        }
                    } else {
                        throw new RuntimeException("API请求失败，状态码: " + response.statusCode());
                    }

                    // 延迟等待
                    try {
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("延迟等待被中断", e);
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("请求处理失败", e);
                }
            }
            throw new RuntimeException("任务处理超时");
        });
    }

    private static final String[] GenDetailSysPrompt = {
            """
                    你是剧本杀创作的一员，你的任务是完善剧本背景，要求如下：
                    	1、请以如下模板输出
                    	## 背景
                    	...（纯文本）

                    	2、除了”## 背景“这部分内容使用MarkDown格式，剩余部分请用纯文本回答，不要使用任何 Markdown 格式（如 ```、**粗体**、*斜体*、标题等）。直接输出内容，无需装饰。

                    	3、请根据用户给出的剧本，完善和丰富背景部分的内容，并且只输出背景部分的内容
                                        """,
            """
                    你是一名小说家，你将根据用户提供的剧本杀框架完成用户指定的人物剧本，要求如下：
                                	1、请以 CHR 角色姓名 的形式开头
                                	示例：
                                	CHR 王小明
                                	...（王小明的详细剧本内容）

                                	2、请用纯文本回答，不要使用任何 Markdown 格式（如 ```、**粗体**、*斜体*、标题等）。直接输出内容，无需装饰。

                                	3、请根据用户给出的剧本，尽可能完善和丰富用户指定角色的剧本内容，并且只输出用户指定角色的剧本内容

                                	4、请使用线性叙事，严格采用第一人称限知视角，仅通过主角的眼睛观察事件。禁止切换其他角色视角，所有信息必须来自：（1）主角亲眼所见/听闻；（2）主角的回忆；（3）主角对物理证据的推理
                                                                """,
            """
                    你是剧本杀创作的一员，你的任务是完整描述剧本中已有的线索，要求如下：
                    	1、请以如下模板输出
                            	## 线索
                                -C> ...
                    		-C> ...
                    		-C> ...

                        2、除了”## 线索“这部分内容使用MarkDown格式，剩余部分请用纯文本回答，不要使用任何 Markdown 格式（如 ```、**粗体**、*斜体*、标题等）。直接输出内容，无需装饰。

                        3、请根据用户给出的剧本，完善和丰富线索部分的内容，并且只输出线索部分的内容

                        4、禁止增加线索数量，请详细描述每一条已有线索
                                        """,
            """
                    你是剧本杀创作的一员，你的任务是完善剧本真相，要求如下：
                        1、请以如下模板输出
                     	## 真相
                        ...（纯文本）

                        2、除了”## 真相“这部分内容使用MarkDown格式，剩余部分请用纯文本回答，不要使用任何 Markdown 格式（如 ```、**粗体**、*斜体*、标题等）。直接输出内容，无需装饰。

                        3、请根据用户给出的剧本，完善和丰富真相部分的内容，并且只输出真相部分的内容
                                        """,
            """
                    你是剧本杀创作的一员，你的任务是完善组织者手册，要求如下：
                    	1、请以 ## 组织者手册\n 开头

                    	2、请用纯文本回答，不要使用任何 Markdown 格式（如 ```、**粗体**、*斜体*、标题等）。直接输出内容，无需装饰。

                    	3、请根据用户给出的剧本，完善和丰富组织者手册部分的内容，并且只输出组织者手册部分的内容
                                        """
    };

    private static final String[] GenDetailUserPrompt = {
            "\n\n请不遗余力地详细描述上述剧本中的 ## 背景 部分",
            "\n\n请根据上述剧本内容不遗余力地详细描述下述人物的剧本",
            "\n\n请不遗余力地详细描述上述剧本中的 ## 线索 部分",
            "\n\n请不遗余力地详细补充上述剧本中的 ## 真相 部分",
            "\n\n请不遗余力地详细描述上述剧本中的 ## 组织者手册 部分"
    };

    private static final String System_MSG = """
            你是一名逻辑严谨的剧本杀作家，必须严格遵循以下规则：
            1. 必须严格按以下模板输出,不要包含额外文本！:
                {
                "作为作家的礼貌性回答":"...",
                "标题":"...",
                "背景":"...",
                "人物剧本": ["...","...",...],
                "线索":["...","...",...],
                "真相":"...",
                "组织者手册": "..."
                }

            2.若有玩家人物是凶手，则需要在其剧本中写明身份；若未写明身份，需在真相处表明原因（如失忆、人格扭曲...）

            3. 当用户要求细化某部分时：
               - 保持原有内容的基础上更新指定部分
               - 未修改的部分需保留原有内容
               - 始终维持完整JSON结构

            4. 格式要求：
               - 严格遵循JSON语法
               - 每次响应必须完整包含所有JSON字段（作为作家的礼貌性回答、标题、背景、人物剧本、线索、真相、组织者手册），不可遗漏
               - 禁止输出\n或者\\n

            5. 请按以下内容深化剧本内容：
                （1）背景：
                    需包含必要的事件背景、人物来历
                    事件的叙述需按时间线描述，要求详细具体
                    必须提供细节描写，以帮助案件推理
                （2）人物剧本：每个角色必须包含：
                    价值观在其生活中的体现
                    人物背景描述
                    动机（角色在本次事件中的目标）
                    时间线（案发前后的行动轨迹）
                    信息差（角色间掌握的信息不尽相同）
                    与其他角色的关系（可以是表面也可以有不为人知的一面）
                （3）线索：
                    关键性线索必须有明确的指向
                    每条线索需要有足够多的细节
                （4）真相：
                    必须包含凶手作案过程
                    详细描述每个细节

            6. 针对上述规则的示例：
                违规示例（1）：
            生成人物剧本部分时，返回
            "人物剧本": {
                "价值观": "...",
                "背景": "...",
                "角色": "...",
                "动机": "...",
                "时间线": [
                "19:00 ...",
                "20:15 ...",
                "21:00 ..."],
                "信息差": "...",
                "关系": "..."},
                正确示例（1）：
            "人物剧本":"甲是一名...，甲...（体现价值观），甲的出身...，甲是为了...来到这里，19：00 甲做了...；20：15 甲在...；21：00 甲...；...。甲知道...的事。甲和...有...的关系，和...有...的关系..."

                违规示例（2）：
            用户要求细化手册时，若仅返回{
              "组织者手册": "..."
            } → 视为违规
                正确示例（2）：
            用户要求细化手册时，应返回完整结构{
              "作为作家的礼貌性回答": "好的，已细化手册部分，其他部分保持现有内容...",
              "标题": "...",
              ...
              "组织者手册": "新细化内容..."
            }

                违规示例（3）：
            生成组织者手册部分，若返回错误结构
            "组织者手册":[
                "...",
                "...",
                "...",
                ...]  →  视为违规
                正确示例（3）：
            "组织者手册":"..."

                违规示例（4）：
            生成作为作家的礼貌性回答部分，若回答
            ...，以下是...的完整剧本
             →  视为违规
                正确示例（4）：
            ...，左侧是...的完整剧本

                违规示例（5）：
            用户要求细化线索时，若其它部分未返回原有部分：
             {"作为作家的礼貌性回答": "...",
              "标题": "...",
              "背景": "维持原有详细内容",
              "人物剧本": "维持原有详细内容",
              "线索": "...",
              "组织者手册": "维持原有详细内容"
            } → 视为违规
                正确示例（5）：
            用户要求细化线索时，应返回完整内容，且不替换原有部分为“维持原有详细内容”
            {"作为作家的礼貌性回答": "...",
              "标题": "...",
              "背景": "...（原有内容）",
              "人物剧本": "...（原有内容）",
              "线索": "...",
              ...}

                违规示例（6）：
            输出“维持原有详细内容”及其相近意义的语段视为违规
            }

                违规示例（7）：
            生成内容时违反JSON语法规则：
             {"作为作家的礼貌性回答": "...",
              "标题": "...",
              "背景": "..."电梯"...于12:13坠落{一些解释}...\n死者...",
                ....
            } → 视为违规 （解释：共有4处错误，1、"电梯"使用了"而不是“，2、12:13使用了:而不是：，3、{一些解释}使用了{}而不是（）,4、\n死者中使用的\n非法，应改为。）
                正确示例（7）：
            生成内容应符合JSON语法：
             {"作为作家的礼貌性回答": "...",
              "标题": "...",
              "背景": "...“电梯”...于12：13坠落（一些解释）...。死者...",
                ....
            }


            现在请根据用户请求进行处理：
                        """;

    private static final String DescChr = """
            你擅长于提取剧本中人物的外貌描写。请遵循以下规则：
            1、严格使用JSON格式，包含名称、描绘两个字段，描绘需包含人物给人的整体感受。

            2、若原文有明确人物外貌描写，提取人物外貌特征到描绘中；若原文描绘不完整可适当补充。

            3、若原文无人物外貌描写，则生成符合人物身份（年龄/职业/性格）的合理特征到描绘中

            5、若输出与外貌描绘无关的信息，视为违规（例如输出人物关系、背景、原因等信息，是违规的）

            4、必须严格按以下模板输出,不要包含额外文本！：
                {
                "名称":["...","...","...",...]
                "描绘":[
                    "...",
                    "...",
                    "...",
                    ...
                ]}

            5、输出示例：
                {
                "名称":["小明","小红","小王",...]
                "描绘":[
                    "小明的外貌描绘...",
                    "小红的外貌描绘...",
                    "小王的外貌描绘...",
                    ...
                ]}

            请输出JSON：
            """;

    private static final String DescScene = """
                        你擅长于提取剧本中场景的环境描写。请遵循以下规则：
                        1、严格使用JSON格式，包含名称、描绘两个字段。

                        2、若原文有明确场景描写，则提取场景特征到描绘中。

                        3、若输出与场景描绘无关的信息，则视为违规

                        4、场景描绘中不能出现人物名称以及人物动作等与人物相关的描述

                        5、输出的场景 名称 和场景 描绘 必须一一对应

                        6、必须严格按以下模板输出,不要包含额外文本！：
                            {
                            "名称":["...","...","...",...]
                            "描绘":[
                                "...",
                                "...",
                                "...",
                                ...
                            ]}

                        7、输出示例：
                            {
                            "名称":["浴室","悬崖","厨房",...]
                            "描绘":[
                                "浴室的场景描绘...",
                                "悬崖的场景描绘...",
                                "厨房的场景描绘...",
                                ...
                            ]}

                        8、错误示例（1）：
                                {
                            "名称":["浴室","悬崖","厨房"]
                            "描绘":[
                                "浴室的场景描绘...",
                                "悬崖的场景描绘...",
                            ]}
                            解释：名称与描绘必须一一对应，示例中输出3个名称却只有2个描绘

                            错误示例（2）：
                                {
                            "名称":["浴室","悬崖","厨房"]
                            "描绘":[
                                "浴室的场景描绘...",
                                "悬崖连接着...李萌依靠在栏杆上...",
                                "厨房的场景描绘..."
                            ]}
                            解释：悬崖的场景描绘出现人物李萌（或者描述成玩家、众人等对人的抽象），这是违规的

                        请输出JSON：
            """;
    private static final String DescProp = """
            你擅长于提取剧本中关键道具的静态外观描写。请遵循以下规则：

            1、若原文有关键道具的明确的状物描写，则将其提取到描绘中；若无原文描写，则基于物品类型进行专业级外观推理

            2、描绘字段只能包含物品的视觉特征，例如（不需要全部包含）：
               - 颜色、形状、材质、尺寸比例
               - 表面纹理/装饰/特殊标记
               - 磨损/氧化/使用痕迹
               - 光影反射特征

            3、绝对禁止出现：
               - 物品用途或功能描述
               - 背景故事或象征意义
               - 与人物相关的任何信息
               - 非视觉特征（如气味、声音）

            4、名称与描绘必须一一对应且数量相等

            5、必须严格按以下模板输出,不要包含额外文本！：
                {
                "名称":["...","...","...",...]
                "描绘":[
                    "...",
                    "...",
                    "...",
                    ...
                ]}

            6、输出示例：
                {
                "名称":["皮医生的药箱","陈夫人的项链","王大爷的皮带",...]
                "描绘":[
                    "方方正正，约莫一尺来长，半尺宽...",
                    "链身极细，每一环都打磨得溜光水滑，在灯下泛着冷冽的银光...",
                    "褐色的牛皮表面布满细密的纹路，带身约莫三指宽，边缘处已经被磨得发亮...",
                    ...
                ]}

            7、错误示例（1）：
                {
                "名称":["皮医生的药箱","陈夫人的项链","王大爷的皮带"]
                "描绘":[
                    "方方正正，约莫一尺来长，半尺宽...",
                    "链身极细，每一环都打磨得溜光水滑，在灯下泛着冷冽的银光...",
                ]}
                解释：名称与描绘必须一一对应，示例中输出3个名称却只有2个描绘

            错误示例（2）：
                    {
                "名称":["DNA检测报告"...]
                "描绘":[
                    "显示你与陈天明存在生物学父女关系",
                    ...
                ]}
                解释：输出内容涉及人物，不是纯粹的外观描写

            请输出JSON：
                            """;

    private static final String AnalyzePrompt = """
            你是一名剧本杀质量评估员，请遵循以下规则对用户给与的剧本杀进行评估：
            1、按指定JSON格式输出以下字段：
                亮点（数组）
                难点（数组）
                改进建议（数组）
                综合评分（包含逻辑性、故事性、体验感三项整数评分）

            输出需符合如下结构：
            {
                "analysis": {
                "point": ["..."],
                "difficulty": ["..."],
                "suggestion": ["..."],
                "score": {
                    "logicality": 0,
                    "storiness": 0,
                    "experience": 0
                    }
                }

            2、评分部分满分为100
                logicality为对剧本故事、人物行为的逻辑性评分
                storiness为对剧本故事性的评分
                experience是对玩家体验性的预测评分

            3、亮点、难点、改进意见的表达需要简单明了，足够详尽
                        """;
}