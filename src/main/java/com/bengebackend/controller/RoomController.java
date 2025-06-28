package com.bengebackend.controller;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.dto.RoomDto;
import com.bengebackend.entity.room.applyRoomEntity;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.entity.room.getAllRoomEntity;
import com.bengebackend.model.Room;
import com.bengebackend.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("api/room")
@Slf4j
public class RoomController {

    @Autowired
    private RoomService roomService;


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
}
