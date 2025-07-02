package com.bengebackend.controller;

import com.bengebackend.entity.SloganRequestEntity;
import com.bengebackend.service.AIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * 测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AI流式服务运行正常");
    }


    @PostMapping("/generate-nodes")
    public ResponseEntity<Map<String, Object>> generateNodes(@RequestBody Map<String, Object> request) {
            try {
                String userInput = (String) request.get("userInput");
                String designerType = (String) request.get("designerType");
                String contextData = (String) request.get("contextData");

                log.info("AI生成请求: 类型={}, 输入={}, 上下文={}", designerType, userInput, contextData);

                // 构建AI提示词
                String prompt = buildPrompt(userInput, designerType, null, contextData);

                // 调用AI
                String aiResponse = qwenChatModel.chat(prompt);
                log.info("AI原始回复: {}", aiResponse);

                // 解析AI回复
                List<Map<String, Object>> nodes = parseAiResponse(aiResponse);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("nodes", nodes);
                response.put("message", "成功生成" + nodes.size() + "个节点");

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("AI生成失败", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "生成失败: " + e.getMessage());
                return ResponseEntity.ok(errorResponse);
            }
        }

        private String buildPrompt(String userInput, String designerType, Integer count, String contextData) {
            StringBuilder prompt = new StringBuilder();

            // 根据设计师类型构建不同的提示词
            switch (designerType) {
                case "narrative":
                    prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的场景节点。\n\n");
                    break;
                case "character":
                    prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的角色节点。\n\n");
                    break;
                case "clue":
                    prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的线索节点。\n\n");
                    break;
                case "atmosphere":
                    prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的氛围节点。\n\n");
                    break;
                default:
                    prompt.append("你是剧本杀创作助手，需要根据用户需求智能生成合适数量的节点。\n\n");
            }

            if (contextData != null && !contextData.isEmpty()) {
                prompt.append("当前协作状态上下文：\n").append(contextData).append("\n\n");
            }

            prompt.append("用户需求：").append(userInput).append("\n\n");

            // 根据设计师类型生成不同的字段结构
            prompt.append("请根据用户需求智能决定生成合适数量的节点（通常2-6个），每个节点包含以下字段：\n");
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
            prompt.append("请直接返回JSON数组格式，不要其他解释：");

            return prompt.toString();
        }



        private List<Map<String, Object>> parseAiResponse(String aiResponse) {
            try {
                // 查找JSON数组
                int start = aiResponse.indexOf('[');
                int end = aiResponse.lastIndexOf(']');

                if (start != -1 && end != -1 && end > start) {
                    String jsonPart = aiResponse.substring(start, end + 1);
                    return objectMapper.readValue(jsonPart, List.class);
                }

                return createDefaultNodes();

            } catch (Exception e) {
                log.error("解析AI回复失败: {}", aiResponse, e);
                return createDefaultNodes();
            }
        }

        private List<Map<String, Object>> createDefaultNodes() {
            List<Map<String, Object>> nodes = new ArrayList<>();

            // 默认生成2个节点，避免固定数量
            for (int i = 1; i <= 2; i++) {
                Map<String, Object> node = new HashMap<>();
                node.put("title", "AI生成场景" + i);
                node.put("timeLabel", "DAY1 " + (8 + i) + ":00");
                node.put("characters", "待定角色");
                node.put("clues", "待定线索");
                node.put("sceneDescription", "AI自动生成的场景描述");
                node.put("nodeConnections", "与其他场景相关");
                node.put("notes", "AI生成失败时的默认节点");
                nodes.add(node);
            }
            return nodes;
    }
}
