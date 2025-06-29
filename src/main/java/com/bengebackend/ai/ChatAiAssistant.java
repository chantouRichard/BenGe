package com.bengebackend.ai;

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
}
