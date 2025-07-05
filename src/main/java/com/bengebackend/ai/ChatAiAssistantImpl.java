package com.bengebackend.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI聊天助手实现类
 */
@Slf4j
@Service
public class ChatAiAssistantImpl implements ChatAiAssistant {
    
    @Autowired
    private QwenChatModel qwenChatModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${AI.key}")
    private String API_KEY;

    public ChatAiAssistantImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private static final String SYSTEM_MESSAGE = """
            你是一位热爱剧本创作的小伙伴，正在和其他创作者一起在群聊里讨论项目。你很懂剧本写作，经验丰富，总能给出实用的建议。
            
            聊天风格：
            
            用日常口语，就像平时微信群聊一样自然
            
            语气轻松友好，偶尔用点表情符号😊
            
            回复简洁有用，不要太正式或冗长
            
            把自己当成群里的一员，而不是AI助手
            
            专业能力：
            
            擅长剧本结构、人物塑造、对话写作
            
            能快速给出创意建议和修改意见
            
            了解各种类型剧本的创作技巧
            
            会分享实用的写作方法和工具
            
            记住：你就是个爱写剧本的朋友，在群里和大家一起头脑风暴呢！
            """;
    
    @Override
    public String chat(String message) {
        try {
            log.info("发送消息给AI: {}", message);
            
            // 构建完整的提示词
            String fullPrompt = SYSTEM_MESSAGE + "\n\n用户: " + message + "\n\nAI助手:";
            
            // 调用AI模型
            String response = qwenChatModel.chat(fullPrompt);
            
            log.info("AI回复: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("AI服务调用失败", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        }
    }

    @Override
    public List<Map<String, String>> getCoopDirection(List<String> keywords) {
        String prompt = String.format("""
        请根据以下关键词整合出6个剧本方向，每个方向需要包含标题（title）和描述（description）。关键词如下：

        %s

        请用如下格式返回：
        [
          { "title": "xxx", "description": "..." },
          ...
        ]
        """, objectMapper.valueToTree(keywords).toPrettyString());

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "qwen-plus");
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "你是一个创意助手，擅长剧本方向设计。"),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<?, ?> result = response.getBody();
            if (result == null) {
                log.error("AI 返回为空");
                return List.of();
            }

            List<?> choices = (List<?>) result.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("AI 返回 choices 为空");
                return List.of();
            }

            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            String reply = (String) message.get("content");

            log.info("AI 原始回复内容：\n{}", reply);

            return objectMapper.readValue(reply, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.error("调用 AI 接口失败", e);
            return List.of();
        }
    }
} 