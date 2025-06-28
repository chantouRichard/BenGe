package com.bengebackend.websocket.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * WebSocket消息基础类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 时间戳
     */
    private String time;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 认证Token（仅用于auth类型消息）
     */
    private String token;
}
