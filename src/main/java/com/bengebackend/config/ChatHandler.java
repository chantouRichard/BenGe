package com.bengebackend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bengebackend.mapper.UserMapper;
import com.bengebackend.mapper.RoomMapper;
import com.bengebackend.model.User;
import com.bengebackend.model.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Slf4j
public class ChatHandler implements WebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> sessionUsers = new ConcurrentHashMap<>();

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("新的连接已建立: sessionId = {}, 远程地址 = {}", session.getId(), session.getRemoteAddress());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            log.debug("收到来自 sessionId = {} 的原始消息: {}", session.getId(), payload);

            JsonNode msg = mapper.readTree(payload);
            String type = msg.get("type").asText();
            Integer userId = sessionUsers.get(session.getId());

            if ("auth".equals(type)) {
                // 处理认证消息
                String token = msg.get("token").asText();
                String roomIdStr = msg.get("roomId").asText();
                
                if (token == null || token.isEmpty()) {
                    log.warn("认证消息缺少token: sessionId = {}", session.getId());
                    sendErrorMessage(session, "Missing token");
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
                    return;
                }
                
                if (roomIdStr == null || roomIdStr.isEmpty()) {
                    log.warn("认证消息缺少roomId: sessionId = {}", session.getId());
                    sendErrorMessage(session, "Missing roomId");
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing roomId"));
                    return;
                }
                
                try {
                    // 验证token并获取用户信息
                    Integer authUserId = tokenService.getUserIdFromToken(token);
                    Optional<User> userOpt = userMapper.findById(authUserId);
                    
                    if (userOpt.isEmpty()) {
                        log.warn("token对应的用户不存在: userId = {}, sessionId = {}", authUserId, session.getId());
                        sendErrorMessage(session, "User not found");
                        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("User not found"));
                        return;
                    }
                    
                    // 验证用户是否有权限加入房间
                    int roomId = Integer.parseInt(roomIdStr);
                    Room room = roomMapper.getRoomById(roomId);
                    if (room == null) {
                        log.warn("用户 {} 尝试加入不存在的房间: roomId = {}", authUserId, roomId);
                        sendErrorMessage(session, "房间不存在");
                        return;
                    }
                    
                    boolean canJoin = roomMapper.isUserInRoom(roomId, authUserId);
                    if (!canJoin) {
                        // 如果用户不在房间中，尝试添加（这里可以根据业务逻辑调整）
                        int result = roomMapper.addRoomMember(roomId, authUserId, 0);
                        if (result <= 0) {
                            log.warn("用户 {} 无法加入房间: roomId = {}", authUserId, roomId);
                            sendErrorMessage(session, "无法加入房间");
                            return;
                        }
                        // 更新房间成员数量
                        roomMapper.updateRoomMemberCount(roomId, 1);
                    }
                    
                    // 保存用户和房间映射
                    sessionUsers.put(session.getId(), authUserId);
                    sessionRooms.put(session.getId(), roomIdStr);
                    
                    // 加入房间WebSocket组
                    rooms.computeIfAbsent(roomIdStr, k -> new CopyOnWriteArraySet<>()).add(session);
                    
                    log.info("用户认证成功并加入房间: userId = {}, username = {}, roomId = {}, sessionId = {}", 
                            authUserId, userOpt.get().getUsername(), roomId, session.getId());
                    
                    // 发送用户信息给前端
                    sendUserInfo(session, userOpt.get());
                    
                    // 广播用户加入房间的消息
                    broadcastSystemMessage(roomIdStr, userOpt.get().getUsername() + " 加入了房间");
                    
                    // 广播当前房间成员列表
                    broadcastRoomMembers(roomIdStr);
                    
                } catch (Exception e) {
                    log.error("认证失败: sessionId = {}, error = {}", session.getId(), e.getMessage());
                    sendErrorMessage(session, "认证失败");
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication failed"));
                }
                
            } else if ("chat".equals(type)) {
                // 检查用户是否已认证
                if (userId == null) {
                    log.warn("未认证的用户尝试发送聊天消息: sessionId = {}", session.getId());
                    sendErrorMessage(session, "请先进行认证");
                    return;
                }
                
                // 获取用户当前所在的房间
                String roomId = sessionRooms.get(session.getId());
                
                if (roomId != null && rooms.containsKey(roomId)) {
                    
                    // 获取用户信息并添加到消息中
                    Optional<User> userOpt = userMapper.findById(userId);
                    if (userOpt.isPresent()) {
                        // 创建包含用户信息的消息
                        JsonNode enhancedMsg = mapper.createObjectNode()
                                .put("type", "chat")
                                .put("roomId", roomId)
                                .put("userId", userId)
                                .put("username", userOpt.get().getUsername())
                                .put("content", msg.get("content").asText())
                                .put("time", msg.get("time").asText())
                                .put("avatar", msg.has("avatar") ? msg.get("avatar").asText() : "");
                        
                        String enhancedPayload = mapper.writeValueAsString(enhancedMsg);
                        
                        CopyOnWriteArraySet<WebSocketSession> clients = rooms.get(roomId);
                        log.info("用户 {} 在房间 {} 发送消息，共有 {} 个客户端", 
                                userOpt.get().getUsername(), roomId, clients.size());

                        clients.forEach(client -> {
                            try {
                                if (client.isOpen()) {
                                    client.sendMessage(new TextMessage(enhancedPayload));
                                }
                            } catch (IOException e) {
                                log.error("向 sessionId = {} 发送消息时出错", client.getId(), e);
                            }
                        });
                    }
                } else {
                    log.warn("收到用户 {} 的聊天消息，但房间 {} 不存在或无效", userId, roomId);
                }
            }
        } catch (Exception e) {
            log.error("处理来自 sessionId = {} 的消息时发生未知错误", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("连接 sessionId = {} 发生传输错误", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String roomId = sessionRooms.remove(session.getId());
        Integer userId = sessionUsers.remove(session.getId());
        
        if (roomId != null && rooms.containsKey(roomId)) {
            if (rooms.get(roomId).remove(session)) {
                log.info("用户 {} 已从房间 {} 断开, 状态: {}", userId, roomId, closeStatus);
                
                // 获取用户信息并广播离开消息
                if (userId != null) {
                    Optional<User> userOpt = userMapper.findById(userId);
                    if (userOpt.isPresent()) {
                        broadcastSystemMessage(roomId, userOpt.get().getUsername() + " 离开了房间");
                    }

                    try {
                        roomMapper.updateRoomMemberCount(Integer.parseInt(roomId), -1);
                    } catch (NumberFormatException e) {
                        log.error("无法更新房间成员数量，房间ID格式错误: {}", roomId);
                    }
                    
                    // 广播更新后的成员列表
                    broadcastRoomMembers(roomId);
                }
            }
        } else {
            log.info("未加入房间的用户 {} 已断开, 状态: {}", userId, closeStatus);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void sendErrorMessage(WebSocketSession session, String error) {
        try {
            JsonNode errorMsg = mapper.createObjectNode()
                    .put("type", "error")
                    .put("message", error);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(errorMsg)));
        } catch (IOException e) {
            log.error("发送错误消息失败: sessionId = {}", session.getId(), e);
        }
    }
    
    private void sendUserInfo(WebSocketSession session, User user) {
        try {
            JsonNode userInfoMsg = mapper.createObjectNode()
                    .put("type", "userInfo")
                    .put("userId", user.getId())
                    .put("username", user.getUsername());
            session.sendMessage(new TextMessage(mapper.writeValueAsString(userInfoMsg)));
        } catch (IOException e) {
            log.error("发送用户信息失败: sessionId = {}", session.getId(), e);
        }
    }
    
    private void broadcastSystemMessage(String roomId, String message) {
        try {
            JsonNode systemMsg = mapper.createObjectNode()
                    .put("type", "system")
                    .put("roomId", roomId)
                    .put("message", message)
                    .put("time", String.valueOf(System.currentTimeMillis()));
            
            String payload = mapper.writeValueAsString(systemMsg);
            CopyOnWriteArraySet<WebSocketSession> clients = rooms.get(roomId);
            
            if (clients != null) {
                clients.forEach(client -> {
                    try {
                        if (client.isOpen()) {
                            client.sendMessage(new TextMessage(payload));
                        }
                    } catch (IOException e) {
                        log.error("广播系统消息失败: sessionId = {}", client.getId(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("创建系统消息失败: roomId = {}", roomId, e);
        }
    }
    
    private void broadcastRoomMembers(String roomIdStr) {
        try {
            CopyOnWriteArraySet<WebSocketSession> clients = rooms.get(roomIdStr);
            if (clients == null) return;
            
            // 获取房间内所有用户信息
            List<Map<String, Object>> members = new ArrayList<>();
            for (WebSocketSession client : clients) {
                Integer userId = sessionUsers.get(client.getId());
                if (userId != null) {
                    Optional<User> userOpt = userMapper.findById(userId);
                    if (userOpt.isPresent()) {
                        Map<String, Object> member = new HashMap<>();
                        member.put("id", userId);
                        member.put("username", userOpt.get().getUsername());
                        member.put("avatar", ""); // 可以后续添加头像URL
                        members.add(member);
                    }
                }
            }
            
            // 创建成员列表消息
            JsonNode membersMsg = mapper.createObjectNode()
                    .put("type", "members")
                    .put("roomId", roomIdStr)
                    .set("members", mapper.valueToTree(members));
            
            String payload = mapper.writeValueAsString(membersMsg);
            
            // 广播给房间内所有用户
            clients.forEach(client -> {
                try {
                    if (client.isOpen()) {
                        client.sendMessage(new TextMessage(payload));
                    }
                } catch (IOException e) {
                    log.error("广播成员列表失败: sessionId = {}", client.getId(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("获取房间成员失败: roomId = {}", roomIdStr, e);
        }
    }
}
