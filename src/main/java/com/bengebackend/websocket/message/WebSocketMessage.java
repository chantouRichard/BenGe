package com.bengebackend.websocket.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

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

    /**
     * 节点数据列表(用于剧情设计师的节点更新)
     */
    private List<NodeData> nodes;

    /**
     * 边数据列表(用于剧情设计师的边更新)
     */
    private List<EdgeData> edges;

    /**
     * 情设计师节点数据类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeData {
        private String id;
        private String type;
        private Position position;
        private NodeContent data;
    }

    /**
     * 情设计师结点位置信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private Integer x;
        private Integer y;
    }

    /**
     * 情设计师节点内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeContent {
        private String title;
        private String timeLabel;
        private String characters;
        private String clues;
        private String sceneDescription;
        private String nodeConnections;
        private String notes;
    }

    /**
     * 情设计师边数据类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeData {
        private String id;
        private String source;
        private String target;
        private String sourcePosition;
        private String targetPosition;
        private String type;
        private EdgeContent data;
    }

    /**
     * 情设计师边内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeContent {
        private String type;
        private String label;
    }
}
