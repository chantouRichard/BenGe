package com.bengebackend.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI聊天助手实现类
 */
@Slf4j
@Service
public class ChatAiAssistantImpl implements ChatAiAssistant {
    
    @Autowired
    private QwenChatModel qwenChatModel;

    @Value("${AI.key}")
    private static String API_KEY;

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

} 