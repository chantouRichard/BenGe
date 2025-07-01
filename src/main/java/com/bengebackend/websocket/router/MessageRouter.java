package com.bengebackend.websocket.router;

import com.bengebackend.websocket.handler.MessageHandler;
import com.bengebackend.websocket.message.WebSocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息路由器
 * 负责将不同类型的消息路由到对应的处理器
 */
@Slf4j
@Component
public class MessageRouter {
    
    private final Map<String, MessageHandler> handlerMap;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public MessageRouter(List<MessageHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        MessageHandler::getSupportedMessageType,
                        Function.identity()
                ));
        
        log.info("注册消息处理器: {}", handlerMap.keySet());
    }
    
    /**
     * 路由消息到对应的处理器
     */
    public void routeMessage(WebSocketSession session, String payload) {
        try {
            log.debug("收到消息: sessionId={}, payload={}", session.getId(), payload);
            
            // 解析消息
            JsonNode jsonNode = objectMapper.readTree(payload);
            String messageType = jsonNode.get("type").asText();
            
            // 转换为消息对象
            WebSocketMessage message = parseMessage(jsonNode);
            
            // 查找对应的处理器
            MessageHandler handler = handlerMap.get(messageType);
            if (handler != null) {
                handler.handleMessage(session, message);
            } else {
                log.warn("未找到消息类型 {} 的处理器: sessionId={}", messageType, session.getId());
            }
            
        } catch (Exception e) {
            log.error("路由消息失败: sessionId={}", session.getId(), e);
        }
    }
    
    /**
     * 解析JSON消息为WebSocketMessage对象
     */
    private WebSocketMessage parseMessage(JsonNode jsonNode) {
        WebSocketMessage message = new WebSocketMessage();

        message.setType(getTextValue(jsonNode, "type"));
        message.setRoomId(getTextValue(jsonNode, "roomId"));
        message.setContent(getTextValue(jsonNode, "content"));
        message.setTime(getTextValue(jsonNode, "time"));
        message.setAvatar(getTextValue(jsonNode, "avatar"));
        message.setToken(getTextValue(jsonNode, "token"));

        // 处理 userId（可能为数字）
        JsonNode userIdNode = jsonNode.get("userId");
        if (userIdNode != null && !userIdNode.isNull()) {
            message.setUserId(userIdNode.asInt());
        }

        message.setUsername(getTextValue(jsonNode, "username"));

        if (jsonNode.has("roleName")) {
            message.setRoleName(getTextValue(jsonNode, "roleName"));
        }

        // 使用统一的 parseList 函数处理各种列表字段
        if (jsonNode.has("nodes")) {
            message.setNodes(parseList(jsonNode.get("nodes"), WebSocketMessage.NodeData.class));
        }

        if (jsonNode.has("edges")) {
            message.setEdges(parseList(jsonNode.get("edges"), WebSocketMessage.EdgeData.class));
        }

        if (jsonNode.has("characterNodes")) {
            message.setCharacterNodes(parseList(jsonNode.get("characterNodes"), WebSocketMessage.CharacterNode.class));
        }

        if (jsonNode.has("characterEdges")) {
            message.setCharacterEdges(parseList(jsonNode.get("characterEdges"), WebSocketMessage.CharacterEdge.class));
        }

        if (jsonNode.has("clueNodes")) {
            message.setClueNodes(parseList(jsonNode.get("clueNodes"), WebSocketMessage.ClueNode.class));
        }

        if (jsonNode.has("clueEdges")) {
            message.setEdges(parseList(jsonNode.get("clueEdges"), WebSocketMessage.EdgeData.class));
        }

        if (jsonNode.has("inferenceNodes")) {
            message.setInferenceNodes(parseList(jsonNode.get("inferenceNodes"), WebSocketMessage.InferenceNode.class));
        }

        if (jsonNode.has("personNodes")) {
            message.setPersonNodes(parseList(jsonNode.get("personNodes"), WebSocketMessage.PersonNode.class));
        }

        return message;
    }


    /**
     * 安全获取文本值
     */
    private String getTextValue(JsonNode jsonNode, String fieldName) {
        JsonNode fieldNode = jsonNode.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    private <T> List<T> parseList(JsonNode node, Class<T> clazz) {
        try {
            return objectMapper.readValue(
                    node.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            log.error("解析 {} 列表失败", clazz.getSimpleName(), e);
            return null;
        }
    }

}


