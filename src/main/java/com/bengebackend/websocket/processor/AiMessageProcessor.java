package com.bengebackend.websocket.processor;

import com.bengebackend.ai.ChatAiAssistant;
import com.bengebackend.model.User;
import com.bengebackend.util.ContextDataProcessor;
import com.bengebackend.websocket.session.RoomManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessageProcessor {
    
    private final ChatAiAssistant chatAiAssistant;
    private final RoomManager roomManager;
    private final ContextDataProcessor contextDataProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理AI消息
     */
    public void processAiMessage(WebSocketSession session, String roomId, Integer userId, User user, String content) {
        try {
            String cleanContent;
            String contextData = "";

            // 检测消息格式并正确解析
            if (content.startsWith("@ai:")) {
                // JSON格式：@ai:{"userMessage":"...","contextData":{...}}
                String jsonPart = content.substring(4); // 移除"@ai:"，保留JSON部分
                try {
                    JsonNode messageJson = objectMapper.readTree(jsonPart);
                    cleanContent = messageJson.get("userMessage").asText();
                    JsonNode contextDataNode = messageJson.get("contextData");
                    if (contextDataNode != null) {
                        contextData = objectMapper.writeValueAsString(contextDataNode);
                    }
                } catch (Exception e) {
                    log.error("解析AI消息JSON失败", e);
                    // 解析失败时回退到简单处理
                    cleanContent = content.replaceFirst("^@ai:?\\s*", "").trim();
                }
            } else {
                // 简单格式：@ai 用户消息
                cleanContent = content.replaceFirst("^@ai\\s*", "").trim();
            }

            if (cleanContent.isEmpty()) {
                cleanContent = "你好";
            }
            
            log.info("用户 {} 在房间 {} 向AI发送消息: {}", user.getUsername(), roomId, cleanContent);
            
            // 先广播用户的原始消息
            broadcastUserMessage(roomId, userId, user, "@ai"+cleanContent);

            String enhancePrompt= buildEnhancedPrompt(cleanContent, contextData, user.getUsername(), roomId);

            
            // 调用AI服务获取回复
            String aiResponse = chatAiAssistant.chat(enhancePrompt);

            aiResponse=aiResponse.substring(aiResponse.indexOf("AI助手:")+5);
            log.info(aiResponse);

            
            // 广播AI回复
            broadcastAiMessage(roomId, aiResponse);
            
            log.info("AI在房间 {} 回复: {}", roomId, aiResponse);
            
        } catch (Exception e) {
            log.error("处理AI消息失败: roomId={}, userId={}", roomId, userId, e);
            broadcastAiErrorMessage(roomId);
        }
    }



    /**
     * 构建增强的AI提示词
     */
    private String buildEnhancedPrompt(String userMessage, String contextData, String username, String roomId) {
        // 检测是否为冲突检测请求
        if (isConflictDetectionRequest(userMessage)) {
            return buildConflictDetectionPrompt(contextData, username, userMessage);
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("你是团队的AI创作助手，正在和大家一起协作设计剧本杀。你能看到项目的完整状态，包括所有设计内容和讨论历史。请用自然、友好的语气参与讨论，就像团队中的一员\n\n");

        // 使用ContextDataProcessor处理上下文数据，获取完整的协作状态
        if (!contextData.isEmpty()) {
            try {
                // 生成完整的上下文摘要，不区分设计师类型
                String contextSummary = contextDataProcessor.generateContextSummary(contextData, "chat");
                prompt.append(contextSummary).append("\n");
            } catch (Exception e) {
                log.error("处理聊天上下文数据失败", e);
                prompt.append("当前协作环境：数据处理异常，基于用户问题进行回复。\n\n");
            }
        } else {
            prompt.append("当前协作环境：新项目开始，团队正在进行初步讨论。\n\n");
        }

        prompt.append("【用户消息】\n");
        prompt.append(username).append("：").append(userMessage).append("\n\n");

        prompt.append("请基于完整的协作状态、所有现有设计内容和讨论历史，用自然的聊天语气回复。");
        prompt.append("回复要求：\n");
        prompt.append("1. 语气自然友好，像团队成员一样\n");
        prompt.append("2. 基于所有现有内容（场景、角色、线索、氛围等）给出建设性建议\n");
        prompt.append("3. 参考之前的讨论历史，保持对话的连贯性\n");
        prompt.append("4. 不要列举分析步骤，直接给出讨论回答\n");
        prompt.append("5. 如果涉及具体设计，要考虑与整体项目的一致性和协调性\n");
        prompt.append("6. 能够跨设计师类型提供综合性的建议和意见\n\n");
        prompt.append("开始回复：");

        return prompt.toString();
    }

    /**
     * 广播用户消息
     */
    private void broadcastUserMessage(String roomId, Integer userId, User user, String content) {
        try {
            JsonNode userMsg = objectMapper.createObjectNode()
                    .put("type", "chat")
                    .put("roomId", roomId)
                    .put("userId", userId)
                    .put("username", user.getUsername())
                    .put("content", content)
                    .put("time", getCurrentFormattedTime())
                    .put("avatar", "");
            
            roomManager.broadcastToRoom(roomId, objectMapper.writeValueAsString(userMsg));
        } catch (Exception e) {
            log.error("广播用户消息失败", e);
        }
    }
    
    /**
     * 广播AI回复
     */
    private void broadcastAiMessage(String roomId, String aiResponse) {
        try {
            JsonNode aiMsg = objectMapper.createObjectNode()
                    .put("type", "chat")
                    .put("roomId", roomId)
                    .put("userId", -1)
                    .put("username", "AI助手")
                    .put("content", aiResponse)
                    .put("time", getCurrentFormattedTime())
                    .put("avatar", "");
            
            roomManager.broadcastToRoom(roomId, objectMapper.writeValueAsString(aiMsg));
        } catch (Exception e) {
            log.error("广播AI消息失败", e);
        }
    }
    
    /**
     * 广播AI错误消息
     */
    private void broadcastAiErrorMessage(String roomId) {
        try {
            JsonNode errorMsg = objectMapper.createObjectNode()
                    .put("type", "chat")
                    .put("roomId", roomId)
                    .put("userId", -1)
                    .put("username", "AI助手")
                    .put("content", "抱歉，AI服务暂时不可用，请稍后再试。")
                    .put("time", getCurrentFormattedTime())
                    .put("avatar", "");
            
            roomManager.broadcastToRoom(roomId, objectMapper.writeValueAsString(errorMsg));
        } catch (Exception e) {
            log.error("发送AI错误消息失败", e);
        }
    }
    
    /**
     * 获取当前格式化时间
     */
    private String getCurrentFormattedTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 检测是否为冲突检测请求
     */
    private boolean isConflictDetectionRequest(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        return lowerMessage.contains("检测冲突") ||
               lowerMessage.contains("冲突检查") ||
               lowerMessage.contains("冲突分析") ||
               lowerMessage.contains("conflict") ||
               lowerMessage.contains("检查冲突") ||
               lowerMessage.contains("分析冲突") ||
               lowerMessage.contains("有没有冲突") ||
               lowerMessage.contains("存在冲突") ||
               lowerMessage.contains("冲突问题");
    }

    /**
     * 构建冲突检测专用提示词
     */
    private String buildConflictDetectionPrompt(String contextData, String username, String userMessage) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是专业的剧本杀设计冲突检测专家。请基于当前项目的完整状态，进行全面的冲突分析。\n\n");

        // 添加上下文信息
        if (!contextData.isEmpty()) {
            try {
                String contextSummary = contextDataProcessor.generateContextSummary(contextData, "conflict");
                prompt.append(contextSummary).append("\n");
            } catch (Exception e) {
                log.error("处理冲突检测上下文数据失败", e);
                prompt.append("当前协作环境：数据处理异常，将基于可用信息进行冲突检测。\n\n");
            }
        } else {
            prompt.append("当前协作环境：新项目，暂无现有内容可供冲突检测。\n\n");
            return "目前项目还没有足够的设计内容，暂时无法进行冲突检测。建议在添加更多场景、角色、线索等内容后再进行冲突分析。";
        }

        prompt.append("【冲突检测任务】\n");
        prompt.append(username).append("请求：").append(userMessage).append("\n\n");

        prompt.append("请进行全面的冲突检测分析，重点关注以下方面：\n\n");

        prompt.append("**1. 时间线冲突检测**\n");
        prompt.append("- 检查场景时间标签是否存在重叠或逻辑错误\n");
        prompt.append("- 分析角色在同一时间段是否出现在多个场景中\n");
        prompt.append("- 验证事件发生的时间顺序是否合理\n");
        prompt.append("- 检查线索发现时间与场景时间的一致性\n\n");

        prompt.append("**2. 逻辑冲突检测**\n");
        prompt.append("- 角色关系是否存在矛盾（如A认识B，但B不认识A）\n");
        prompt.append("- 线索逻辑链是否完整且无矛盾\n");
        prompt.append("- 角色动机与行为是否一致\n");
        prompt.append("- 推理节点的逻辑推导是否合理\n\n");

        prompt.append("**3. 内容冲突检测**\n");
        prompt.append("- 角色背景描述是否存在自相矛盾\n");
        prompt.append("- 场景描述与角色、线索的关联是否合理\n");
        prompt.append("- 角色技能、物品与其背景是否匹配\n");
        prompt.append("- 氛围设计与场景、情节是否协调\n\n");

        prompt.append("**4. 跨设计师类型冲突检测**\n");
        prompt.append("- 场景设计与角色设计的匹配度\n");
        prompt.append("- 线索设计与场景、角色的关联性\n");
        prompt.append("- 氛围设计与整体故事风格的一致性\n");
        prompt.append("- 不同设计师创建内容之间的协调性\n\n");

        prompt.append("**输出要求：**\n");
        prompt.append("请按以下格式输出检测结果：\n\n");
        prompt.append("🔍 **冲突检测报告**\n\n");
        prompt.append("**检测概况：**\n");
        prompt.append("- 总体评估：[无冲突/轻微冲突/中等冲突/严重冲突]\n");
        prompt.append("- 检测范围：[具体说明检测了哪些内容]\n\n");

        prompt.append("**发现的冲突：**\n");
        prompt.append("[如果有冲突，按类型详细列出，包括具体位置和问题描述,如果没有冲突就不要说]\n\n");

        prompt.append("**修改建议：**\n");
        prompt.append("[针对每个冲突提供具体的解决方案]\n\n");

        prompt.append("**优化建议：**\n");
        prompt.append("[提供进一步完善设计的建议]\n\n");

        prompt.append("请开始进行冲突检测分析：");

        return prompt.toString();
    }
}
