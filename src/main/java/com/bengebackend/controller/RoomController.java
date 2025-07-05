package com.bengebackend.controller;

import com.bengebackend.ai.ChatAiAssistant;
import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.dto.RoomDto;
import com.bengebackend.entity.room.AICooperateDirection;
import com.bengebackend.entity.room.applyRoomEntity;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.entity.room.getAllRoomEntity;
import com.bengebackend.model.Room;
import com.bengebackend.service.RoomService;
import com.bengebackend.websocket.message.WebSocketMessage;
import com.bengebackend.websocket.session.RoomManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/room")
@Slf4j
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private final RedissonClient redissonClient;

    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private ChatAiAssistant chatAiAssistant;

    public RoomController(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 创建房间
     *
     * @param createRoomEntity 包含房间信息的实体类
     * @return 创建结果的响应实体
     */
    @PostMapping
    public ResponseEntity<RoomCreateDto> createRoom(@RequestBody createRoomEntity createRoomEntity) {
        log.debug("createRoomEntity: {}", createRoomEntity);
        RoomCreateDto roomDto = roomService.createRoom(createRoomEntity);
        return ResponseEntity.ok(roomDto);
    }

    /**
     * 获取所有房间
     *
     * @param getAllRoomsEntity 包含分页信息的实体类
     * @return 房间列表的响应实体
     */
    @PostMapping("/list")
    public ResponseEntity<List<RoomDto>> getAllRooms(@RequestBody getAllRoomEntity getAllRoomsEntity) {
        log.debug("getAllRoomsEntity: {}", getAllRoomsEntity);
        List<RoomDto> roomDtos = roomService.getAllRooms(getAllRoomsEntity);
        return ResponseEntity.ok(roomDtos);
    }

    @PostMapping("/application")
    public ResponseEntity<String> applyRoom(@RequestBody applyRoomEntity applyRoomEntity) {
        log.debug("applyRoomEntity: {}", applyRoomEntity);
        String result = roomService.applyRoom(applyRoomEntity);
        return ResponseEntity.ok(result);
    }

    /**
     * 退出房间
     *
     * @param roomId 房间ID
     * @return 退出结果的响应实体
     */
    @PostMapping("/leave/{roomId}")
    public ResponseEntity<String> leaveRoom(@PathVariable int roomId) {
        log.debug("退出房间，房间ID: {}", roomId);
        String result = roomService.leaveRoom(roomId);
        return ResponseEntity.ok(result);
    }

    /**
     * 同步所有房间的成员数量（管理员功能）
     *
     * @return 同步结果的响应实体
     */
    @PostMapping("/sync-member-counts")
    public ResponseEntity<String> syncMemberCounts() {
        log.debug("开始同步所有房间的成员数量");
        roomService.syncAllRoomMemberCounts();
        return ResponseEntity.ok("同步完成");
    }

    /**
     * 获取用户当前所在房间
     *
     * @return 当前房间信息
     */
    @PostMapping("/current")
    public ResponseEntity<Room> getCurrentRoom() {
        Room currentRoom = roomService.getUserCurrentRoom();
        return ResponseEntity.ok(currentRoom);
    }

    /**
     * 获取用户拥有的房间列表
     *
     * @return 用户拥有的房间列表
     */
    @PostMapping("/owned")
    public ResponseEntity<List<RoomDto>> getOwnedRooms() {
        List<RoomDto> ownedRooms = roomService.getUserOwnedRooms();
        return ResponseEntity.ok(ownedRooms);
    }

    @PostMapping("/enter-owned/{roomId}")
    public ResponseEntity<String> enterOwnedRoom(@PathVariable int roomId) {
        try {
            String result = roomService.enterOwnedRoom(roomId);
            if ("success".equals(result)) {
                return ResponseEntity.ok("进入房间成功");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("进入房间失败");
        }
    }

    @PostMapping("/is-owner/{roomId}")
    public ResponseEntity<Boolean> isOwner(@PathVariable Integer roomId) {
        // 调用 Service 获取当前用户是否是房主
        boolean isOwner = roomService.isOwner(roomId);
        return ResponseEntity.ok(isOwner);
    }

    @PostMapping("/generate-ai-content")
    public ResponseEntity<?> generateDirection(@RequestBody AICooperateDirection aiCooperateDirection){
        String roomId = aiCooperateDirection.getRoomId();
        RLock lock = redissonClient.getLock("room:" + roomId);
        log.info("testtest");

        boolean locked = false;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!locked) {
                return ResponseEntity.status(429).body("正在处理中，请稍候");
            }

            if (Boolean.TRUE.equals(redisTemplate.hasKey("ai_done:" + roomId))) {
                return ResponseEntity.ok("AI已经生成过内容了");
            }

            // 调用AI接口，返回 List<Map<String, String>>
            List<Map<String, String>> result = chatAiAssistant.getCoopDirection(aiCooperateDirection.getKeyWords());

            // 广播：
            WebSocketMessage msg = new WebSocketMessage();
            msg.setType("vote");
            msg.setRoomId(roomId);
            msg.setContent(objectMapper.writeValueAsString(result));
            roomManager.broadcastToRoom(roomId, objectMapper.writeValueAsString(msg));

            // 设置标志，10分钟过期
            redisTemplate.opsForValue().set("ai_done:" + roomId, true, 10, TimeUnit.MINUTES);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("生成剧本方向失败", e);
            return ResponseEntity.status(500).body("生成失败");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
