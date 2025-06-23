package com.bengebackend.entity;

import jakarta.validation.constraints.NotNull;

public class ScriptReplyRequestEntity {

    /**
     * 脚本ID
     */
    @NotNull(message = "ScriptId cannot be null")
    private Integer scriptId;

    /**
     * 回复消息
     */
    @NotNull(message = "Message cannot be null")
    private String message;

    // 默认构造函数
    public ScriptReplyRequestEntity() {}

    // 带参构造函数
    public ScriptReplyRequestEntity(Integer scriptId, String message) {
        this.scriptId = scriptId;
        this.message = message;
    }

    // Getter 和 Setter
    public Integer getScriptId() {
        return scriptId;
    }

    public void setScriptId(Integer scriptId) {
        this.scriptId = scriptId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
