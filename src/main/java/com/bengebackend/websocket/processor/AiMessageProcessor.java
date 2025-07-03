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
            // 清理消息内容，移除@ai前缀
            String cleanContent = content.replaceFirst("^@ai\\s*", "").trim();
            String contextData="";
            if(cleanContent.contains("[CONTEXT_DATA]")&&cleanContent.contains("[/CONTEXT_DATA]")){
                int startIdx=cleanContent.indexOf("[CONTEXT_DATA]");
                int endIdx=cleanContent.indexOf("[/CONTEXT_DATA]");
                contextData=cleanContent.substring(startIdx,endIdx);
                cleanContent=cleanContent.substring(0,startIdx).trim();
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
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是团队的AI创作助手，正在和大家一起协作设计剧本杀。你能看到项目的完整状态，包括所有设计内容和讨论历史。请用自然、友好的语气参与讨论，就像团队中的一员。\n\n");

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
                    .put("userId", -1) // AI用户ID设为-1
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
}
