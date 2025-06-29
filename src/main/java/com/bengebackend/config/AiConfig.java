 package com.bengebackend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.community.model.dashscope.QwenChatModel;

/**
 * AI配置类
 */
@Configuration
public class AiConfig {
    
    @Value("${DASHSCOPE_API_KEY:your_api_key_here}")
    private String apiKey;
    
    @Bean
    QwenChatModel qwenModel(){
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-turbo")
                .build();
    }
}