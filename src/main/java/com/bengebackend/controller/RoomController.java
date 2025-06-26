package com.bengebackend.controller;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.dto.RoomDto;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.entity.room.getAllRoomEntity;
import com.bengebackend.service.RoomService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
