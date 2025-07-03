package com.bengebackend.controller;

import com.bengebackend.entity.SloganRequestEntity;
import com.bengebackend.entity.AIMsgDevide;
import com.bengebackend.model.ScriptHistory;
import com.bengebackend.service.AIService;
import com.bengebackend.util.ContextDataProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
import com.bengebackend.service.ScriptService;
import com.bengebackend.dto.ScriptDetailDto;
import com.bengebackend.entity.ScriptReplyRequestEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@Slf4j
public class AIStreamController {

    @Autowired
    private AIService aiService;

    @Autowired
    private QwenChatModel qwenChatModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private ContextDataProcessor contextDataProcessor;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Slogan流式生成接口
     */
    @PostMapping(value = "/slogan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSloganStream(@RequestBody SloganRequestEntity request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                aiService.GenerateSloganStreamAsync(request, slogan -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("slogan")
                                .data(slogan));
                    } catch (IOException e) {
                        System.err.println("发送Slogan事件失败: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }).get(); // 等待完成

                emitter.complete();
            } catch (Exception e) {
                System.err.println("生成Slogan流式输出失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * AI助手流式聊天接口
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.execute(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");

                if (messages == null) {
                    emitter.completeWithError(new IllegalArgumentException("messages参数不能为空"));
                    return;
                }

                aiService.ChatStreamAsync(messages, content -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(content));
                    } catch (IOException e) {
                        System.err.println("发送聊天事件失败: " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }).get(); // 等待完成

                emitter.complete();
            } catch (Exception e) {
                System.err.println("AI助手流式聊天失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * GenFramework流式输出接口
     */
    @PostMapping(value = "/genframework/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateFrameworkStream(@RequestBody ScriptReplyRequestEntity request) {
        SseEmitter emitter = new SseEmitter(0L);
        // 准备消息
        ScriptDetailDto sdd = scriptService.getScriptByIdAsync(request.getScriptId());
        List<ScriptHistory> history = sdd.getHistory();
        List<Map<String, String>> messages = new ArrayList<>();
        for (ScriptHistory h : history) {
            Map<String, String> msg = new HashMap<>();
            if (h.getResponse() == "" && h.getMessage() != "") {
                msg.put("role", "user");
                msg.put("content", h.getMessage());
            } else {
                msg.put("role", "assistant");
                msg.put("content", h.getResponse());
            }
            messages.add(msg);
        }
        messages.add(new HashMap<String, String>() {
            {
                put("role", "user");
                put("content", request.getMessage());
            }
        });

        // 启动异步流式处理
        CompletableFuture<AIMsgDevide> future = aiService.GenFrameworkStream(messages, chunk -> {

            try {
                // 发送数据块到客户端
                emitter.send(SseEmitter.event()
                        .name("message") // 事件名称
                        .data(chunk) // 实际内容
                        .reconnectTime(3000));// 重连时间
            } catch (IOException e) {
                // 发送失败时取消任务
                throw new RuntimeException("SSE发送失败", e);
            }
        });

        // 处理完成/异常情况
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // 异常处理
                emitter.completeWithError(ex);
            } else {
                // 正常完成
                emitter.complete();
                scriptService.updateScriptAsync(request.getScriptId(), result.getTitle(), result.getStrScript(), 2);
            }
        });

        // 处理连接关闭
        emitter.onCompletion(() -> {
            if (!future.isDone()) {
                future.cancel(true); // 取消仍在进行的任务
            }
        });

        emitter.onTimeout(() -> {
            future.cancel(true); // 超时取消任务
        });

        return emitter;
    }

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AI流式服务运行正常");
    }

    @PostMapping("/generate-nodes")
    public ResponseEntity<Map<String, Object>> generateNodes(@RequestBody Map<String, Object> request) {
        log.info("收到AI节点生成请求: {}", request);

        try {
            // 参数验证
            String userInput = (String) request.get("userInput");
            String designerType = (String) request.get("designerType");
            String contextData = (String) request.get("contextData");

            if (userInput == null || userInput.trim().isEmpty()) {
                log.warn("用户输入为空");
                return createErrorResponse("用户输入不能为空");
            }

            if (designerType == null || designerType.trim().isEmpty()) {
                log.warn("设计师类型为空");
                return createErrorResponse("设计师类型不能为空");
            }

            log.info("AI生成请求详情: 类型={}, 输入长度={}, 上下文数据长度={}",
                    designerType, userInput.length(),
                    contextData != null ? contextData.length() : 0);

            // 构建AI提示词
            String prompt = buildPrompt(userInput, designerType, null, contextData);
            log.debug("构建的AI提示词长度: {}", prompt.length());

            // 调用AI
            String aiResponse = qwenChatModel.chat(prompt);
            log.info("AI回复成功，回复长度: {}", aiResponse != null ? aiResponse.length() : 0);
            log.debug("AI原始回复: {}", aiResponse);

            // 解析AI回复
            List<Map<String, Object>> nodes = parseAiResponse(aiResponse, designerType);
            log.info("成功解析生成{}个节点", nodes.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("nodes", nodes);
            response.put("message", "成功生成" + nodes.size() + "个节点");
            response.put("nodeCount", nodes.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI节点生成失败", e);
            return createErrorResponse("生成失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("nodes", new ArrayList<>());
        errorResponse.put("nodeCount", 0);
        return ResponseEntity.ok(errorResponse);
    }

    private String buildPrompt(String userInput, String designerType, Integer count, String contextData) {
        StringBuilder prompt = new StringBuilder();

        // 根据设计师类型构建提示词
        switch (designerType) {
            case "narrative":
                prompt.append("你是剧本杀创作助手，专门负责场景设计。请基于团队的完整协作状态，结合现有的角色、线索、氛围等所有设计内容，智能生成与整体项目协调一致的场景节点。\n\n");
                break;
            case "character":
                prompt.append("你是剧本杀创作助手，专门负责角色设计。请基于团队的完整协作状态，结合现有的场景、线索、氛围等所有设计内容，智能生成与整体项目协调一致的角色节点。\n\n");
                break;
            case "clue":
                prompt.append("你是剧本杀创作助手，专门负责线索设计。请基于团队的完整协作状态，结合现有的场景、角色、氛围等所有设计内容，智能生成与整体项目协调一致的线索节点。\n\n");
                break;
            case "atmosphere":
                prompt.append("你是剧本杀创作助手，专门负责氛围设计。请基于团队的完整协作状态，结合现有的场景、角色、线索等所有设计内容，智能生成与整体项目协调一致的氛围节点。\n\n");
                break;
            default:
                prompt.append("你是剧本杀创作助手，请基于团队的完整协作状态和所有现有设计内容，智能生成与整体项目协调一致的节点。\n\n");
        }

        // 使用ContextDataProcessor生成结构化的上下文描述
        if (contextData != null && !contextData.isEmpty()) {
            try {
                String contextSummary = contextDataProcessor.generateContextSummary(contextData, designerType);
                prompt.append(contextSummary).append("\n");
            } catch (Exception e) {
                log.error("处理上下文数据失败", e);
                prompt.append("当前协作环境：数据处理异常，建议基于用户需求进行创作。\n\n");
            }
        } else {
            prompt.append("当前协作环境：新项目开始，暂无现有内容。\n\n");
        }

        prompt.append("【用户具体需求】\n").append(userInput).append("\n\n");

        prompt.append("【生成要求】\n");
        prompt.append("请基于以上完整的协作状态和用户需求，生成JSON数组格式的节点数据。\n");
        prompt.append("生成时必须考虑：\n");
        prompt.append("1. 与所有现有内容（场景、角色、线索、氛围等）保持一致性和连贯性\n");
        prompt.append("2. 参考团队讨论历史中的想法和建议\n");
        prompt.append("3. 符合剧本杀的创作规范和逻辑\n");
        prompt.append("4. 具有足够的细节和可操作性\n");
        prompt.append("5. 智能决定生成合适数量的节点（通常2-6个）\n");
        prompt.append("6. 确保新生成的内容能够与现有设计形成有机整体\n\n");

        // 根据设计师类型生成不同的字段结构
        prompt.append("节点数据格式要求，每个节点包含以下字段：\n");
        prompt.append("{\n");

        switch (designerType) {
            case "narrative":
                prompt.append("  \"title\": \"场景名称\",\n");
                prompt.append("  \"timeLabel\": \"时间标签(如DAY1 09:00)\",\n");
                prompt.append("  \"characters\": \"涉及角色\",\n");
                prompt.append("  \"clues\": \"相关线索\",\n");
                prompt.append("  \"sceneDescription\": \"场景详细描述\",\n");
                prompt.append("  \"nodeConnections\": \"与其他节点的关系\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
            case "character":
                prompt.append("  \"name\": \"角色姓名\",\n");
                prompt.append("  \"occupation\": \"职业\",\n");
                prompt.append("  \"age\": 年龄数字,\n");
                prompt.append("  \"background\": \"背景故事\",\n");
                prompt.append("  \"personality\": [\"性格特点1\", \"性格特点2\", \"性格特点3\"],\n");
                prompt.append("  \"skills\": [\"技能1\", \"技能2\", \"技能3\"],\n");
                prompt.append("  \"items\": \"携带物品\",\n");
                prompt.append("  \"notes\": \"备注\",\n");
                prompt.append("  \"relationships\": []\n");
                break;
            case "clue":
                prompt.append("  \"title\": \"线索名称\",\n");
                prompt.append("  \"type\": \"线索类型\",\n");
                prompt.append("  \"description\": \"线索描述\",\n");
                prompt.append("  \"location\": \"发现地点\",\n");
                prompt.append("  \"relatedCharacters\": \"相关角色\",\n");
                prompt.append("  \"importance\": \"重要程度\",\n");
                prompt.append("  \"hiddenInfo\": \"隐藏信息\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
            case "atmosphere":
                prompt.append("  \"title\": \"氛围名称\",\n");
                prompt.append("  \"mood\": \"情绪氛围(如：紧张、平静、神秘)\",\n");
                prompt.append("  \"lighting\": \"灯光设置\",\n");
                prompt.append("  \"music\": \"背景音乐\",\n");
                prompt.append("  \"weather\": \"天气状况\",\n");
                prompt.append("  \"timeOfDay\": \"时间段\",\n");
                prompt.append("  \"sceneElements\": \"场景元素\",\n");
                prompt.append("  \"notes\": \"备注\"\n");
                break;
        }
            switch (designerType) {
                case "narrative":
                    prompt.append("  \"title\": \"场景名称\",\n");
                    prompt.append("  \"timeLabel\": \"时间标签(如DAY1 09:00)\",\n");
                    prompt.append("  \"characters\": \"涉及角色\",\n");
                    prompt.append("  \"clues\": \"相关线索\",\n");
                    prompt.append("  \"sceneDescription\": \"场景详细描述\",\n");
                    prompt.append("  \"nodeConnections\": \"与其他节点的关系\",\n");
                    prompt.append("  \"notes\": \"备注\"\n");
                    break;
                case "character":
                    prompt.append("  \"name\": \"角色姓名\",\n");
                    prompt.append("  \"occupation\": \"职业\",\n");
                    prompt.append("  \"age\": 年龄数字,\n");
                    prompt.append("  \"background\": \"背景故事\",\n");
                    prompt.append("  \"personality\": [\"性格特点1\", \"性格特点2\", \"性格特点3\"],\n");
                    prompt.append("  \"skills\": [\"技能1\", \"技能2\", \"技能3\"],\n");
                    prompt.append("  \"items\": \"携带物品\",\n");
                    prompt.append("  \"notes\": \"备注\"\n");
                    break;
                case "clue":
                    prompt.append("  \"title\": \"线索名称\",\n");
                    prompt.append("  \"type\": \"线索类型\",\n");
                    prompt.append("  \"description\": \"线索描述\",\n");
                    prompt.append("  \"location\": \"发现地点\",\n");
                    prompt.append("  \"relatedCharacters\": \"相关角色\",\n");
                    prompt.append("  \"importance\": \"重要程度\",\n");
                    prompt.append("  \"hiddenInfo\": \"隐藏信息\",\n");
                    prompt.append("  \"notes\": \"备注\"\n");
                    break;
                case "atmosphere":
                    prompt.append("  \"title\": \"氛围名称\",\n");
                    prompt.append("  \"mood\": \"情绪氛围(如：紧张、平静、神秘)\",\n");
                    prompt.append("  \"lighting\": \"灯光设置\",\n");
                    prompt.append("  \"music\": \"背景音乐\",\n");
                    prompt.append("  \"weather\": \"天气状况\",\n");
                    prompt.append("  \"timeOfDay\": \"时间段\",\n");
                    prompt.append("  \"sceneElements\": \"场景元素\",\n");
                    prompt.append("  \"notes\": \"备注\"\n");
                    break;
            }

        prompt.append("}\n\n");
        prompt.append("【输出格式】\n");
        prompt.append("请直接返回JSON数组格式，不要添加任何解释文字。\n");
        prompt.append("确保JSON格式正确，所有字符串值都用双引号包围。\n");
        prompt.append("示例格式：[{...}, {...}, {...}]\n\n");
        prompt.append("开始生成：");

        return prompt.toString();
    }

    private List<Map<String, Object>> parseAiResponse(String aiResponse, String designerType) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            log.warn("AI回复为空，使用默认节点");
            return createDefaultNodes(designerType);
        }

        try {
            log.debug("开始解析AI回复，长度: {}", aiResponse.length());

            // 查找JSON数组
            int start = aiResponse.indexOf('[');
            int end = aiResponse.lastIndexOf(']');

            if (start != -1 && end != -1 && end > start) {
                String jsonPart = aiResponse.substring(start, end + 1);
                log.debug("提取的JSON部分: {}", jsonPart);

                List<Map<String, Object>> nodes = objectMapper.readValue(jsonPart, List.class);

                if (nodes == null || nodes.isEmpty()) {
                    log.warn("AI返回的节点列表为空，使用默认节点");
                    return createDefaultNodes(designerType);
                }

                log.info("成功解析AI回复，获得{}个节点", nodes.size());
                return nodes;
            } else {
                log.warn("AI回复中未找到有效的JSON数组格式，开始位置: {}, 结束位置: {}", start, end);
                log.debug("无效的AI回复内容: {}", aiResponse);
                return createDefaultNodes(designerType);
            }

        } catch (Exception e) {
            log.error("解析AI回复时发生异常，回复内容: {}", aiResponse, e);
            return createDefaultNodes(designerType);
        }
    }

    private List<Map<String, Object>> createDefaultNodes(String designerType) {
        List<Map<String, Object>> nodes = new ArrayList<>();

        // 默认生成2个节点，避免固定数量
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> node = new HashMap<>();

            switch (designerType) {
                case "character":
                    node.put("name", "AI生成角色" + i);
                    node.put("occupation", "待定职业");
                    node.put("age", 25 + i);
                    node.put("background", "AI自动生成的角色背景");
                    node.put("personality", Arrays.asList("待定性格1", "待定性格2", "待定性格3"));
                    node.put("skills", Arrays.asList("待定技能1", "待定技能2", "待定技能3"));
                    node.put("items", "待定物品");
                    node.put("notes", "AI生成失败时的默认角色节点");
                    node.put("relationships", new ArrayList<>());
                    break;
                case "clue":
                    node.put("title", "AI生成线索" + i);
                    node.put("type", "物理证据");
                    node.put("description", "AI自动生成的线索描述");
                    node.put("location", "待定地点");
                    node.put("relatedCharacters", Arrays.asList("待定角色"));
                    node.put("importance", "中");
                    node.put("hiddenInfo", "待定隐藏信息");
                    node.put("notes", "AI生成失败时的默认线索节点");
                    break;
                case "atmosphere":
                    node.put("title", "AI生成氛围" + i);
                    node.put("timeLabel", "DAY1 " + (8 + i) + ":00");
                    node.put("mood", "平静");
                    node.put("lighting", "自然光");
                    node.put("music", "无");
                    node.put("weather", "晴朗");
                    node.put("sceneElements", "待定场景元素");
                    node.put("notes", "AI生成失败时的默认氛围节点");
                    break;
                default: // narrative
                    node.put("title", "AI生成场景" + i);
                    node.put("timeLabel", "DAY1 " + (8 + i) + ":00");
                    node.put("characters", "待定角色");
                    node.put("clues", "待定线索");
                    node.put("sceneDescription", "AI自动生成的场景描述");
                    node.put("nodeConnections", "与其他场景相关");
                    node.put("notes", "AI生成失败时的默认节点");
                    break;
            }
            nodes.add(node);
        }
        return nodes;
    }
}
