package com.bengebackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ChatRequestEntity {

    @JsonProperty("message")
    private String message;  // 当前用户输入

    @JsonProperty("history")
    private List<String> history;  // 可选：历史对话内容

    // 默认构造函数
    public ChatRequestEntity() {}

    // 带参构造函数
    public ChatRequestEntity(String message, List<String> history) {
        this.message = message;
        this.history = history;
    }

    // Getter 和 Setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }
}
