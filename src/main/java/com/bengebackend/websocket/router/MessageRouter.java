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
        
        // 处理userId（可能为数字）
        JsonNode userIdNode = jsonNode.get("userId");
        if (userIdNode != null && !userIdNode.isNull()) {
            message.setUserId(userIdNode.asInt());
        }
        
        message.setUsername(getTextValue(jsonNode, "username"));

        // 新增对nodes和edges的处理
        if (jsonNode.has("nodes")) {
            message.setNodes(parseNodes(jsonNode.get("nodes")));
        }

        if (jsonNode.has("edges")) {
            message.setEdges(parseEdges(jsonNode.get("edges")));
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

    /**
     * 解析节点数据
     */
    private List<WebSocketMessage.NodeData> parseNodes(JsonNode nodesNode) {
        try {
            return objectMapper.readValue(
                    nodesNode.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            WebSocketMessage.NodeData.class
                    )
            );
        } catch (Exception e) {
            log.error("解析节点数据失败", e);
            return null;
        }
    }

    /**
     * 解析边数据
     */
    private List<WebSocketMessage.EdgeData> parseEdges(JsonNode edgesNode) {
        try {
            return objectMapper.readValue(
                    edgesNode.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            WebSocketMessage.EdgeData.class
                    )
            );
        } catch (Exception e) {
            log.error("解析边数据失败", e);
            return null;
        }
    }
}
