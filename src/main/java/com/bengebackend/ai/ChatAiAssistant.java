package com.bengebackend.ai;

import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI聊天助手接口
 */
public interface ChatAiAssistant {
    /**
     * 发送消息给AI并获取回复
     * @param message 用户消息
     * @return AI回复
     */
    String chat(String message);

    List<Map<String, String>> getCoopDirection(List<String> keywords);
}
