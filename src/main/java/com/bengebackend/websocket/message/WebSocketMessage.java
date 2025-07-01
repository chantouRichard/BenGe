package com.bengebackend.websocket.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * WebSocket 消息统一封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /** 消息类型 */
    private String type;

    /** 房间 ID */
    private String roomId;

    /** 用户 ID */
    private Integer userId;

    /** 用户名 */
    private String username;

    /** 消息文本内容 */
    private String content;

    /** 消息时间戳 */
    private String time;

    /** 用户头像 URL */
    private String avatar;

    /** 用户认证 Token（用于 auth 类型消息） */
    private String token;

    /** 角色名称（如“剧情设计师”“人物设计师”） */
    private String roleName;

    /** 剧情设计师节点数据列表 */
    private List<NodeData> nodes;

    /** 剧情设计师边数据列表 */
    private List<EdgeData> edges;

    /** 人物设计师节点数据列表 */
    private List<CharacterNode> characterNodes;

    /** 人物设计师边数据列表 */
    private List<CharacterEdge> characterEdges;

    /**
     * 剧情设计师节点结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeData {
        /** 节点 ID */
        private String id;

        /** 节点类型 */
        private String type;

        /** 节点位置 */
        private Position position;

        /** 节点内容 */
        private NodeContent data;
    }

    /**
     * 节点位置信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        /** x 坐标 */
        private Integer x;

        /** y 坐标 */
        private Integer y;
    }

    /**
     * 剧情设计师节点内容结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeContent {
        /** 节点标题 */
        private String title;

        /** 时间标签 */
        private String timeLabel;

        /** 涉及人物 */
        private String characters;

        /** 相关线索 */
        private String clues;

        /** 场景描述 */
        private String sceneDescription;

        /** 连接信息 */
        private String nodeConnections;

        /** 备注信息 */
        private String notes;
    }

    /**
     * 剧情设计师边结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeData {
        /** 边 ID */
        private String id;

        /** 源节点 ID */
        private String source;

        /** 目标节点 ID */
        private String target;

        /** 源节点连接点位置 */
        private String sourcePosition;

        /** 目标节点连接点位置 */
        private String targetPosition;

        /** 边类型 */
        private String type;

        /** 边内容 */
        private EdgeContent data;
    }

    /**
     * 剧情设计师边内容结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeContent {
        /** 关系类型 */
        private String type;

        /** 标签内容 */
        private String label;
    }

    /**
     * 人物设计师节点结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterNode {
        /** 节点 ID */
        private String id;

        /** 节点类型 */
        private String type;

        /** 节点位置 */
        private Position position;

        /** 人物节点内容 */
        private CharacterContent data;
    }

    /**
     * 人物节点详细内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterContent {
        /** 姓名 */
        private String name;

        /** 头像 URL */
        private String avatar;

        /** 年龄 */
        private Integer age;

        /** 职业 */
        private String occupation;

        /** 性格特征列表 */
        private List<String> personality;

        /** 背景故事 */
        private String background;

        /** 技能列表 */
        private List<String> skills;

        /** 携带物品 */
        private String items;

        /** 备注信息 */
        private String notes;

        /** 关系 ID 列表 */
        private List<String> relationships;
    }

    /**
     * 人物设计师边结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterEdge {
        /** 边 ID */
        private String id;

        /** 源人物节点 ID */
        private String source;

        /** 目标人物节点 ID */
        private String target;

        /** 源连接点位置 */
        private String sourceHandle;

        /** 目标连接点位置 */
        private String targetHandle;

        /** 边类型（如 relationship） */
        private String type;

        /** 边数据 */
        private CharacterEdgeContent data;
    }

    /**
     * 人物设计师边内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterEdgeContent {
        /** 关系类型（如亲属、敌对） */
        private String type;

        /** 关系描述 */
        private String description;

        /** 关系强度（如高、中、低） */
        private String strength;

        /** 当前状态（如稳定、紧张） */
        private String status;

        /** 显示用标签 */
        private String label;
    }
}
