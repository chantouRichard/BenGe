package com.bengebackend.service.serviceImpl;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.dto.RoomDto;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.entity.room.getAllRoomEntity;
import com.bengebackend.mapper.RoomMapper;
import com.bengebackend.model.Room;
import com.bengebackend.service.RoomService;
import com.bengebackend.util.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomMapper roomMapper;

    @Override
    public List<RoomDto> getAllRooms(getAllRoomEntity getAllRoomsEntity) {
        // 计算分页偏移量
        int offset = (getAllRoomsEntity.getPage() - 1) * getAllRoomsEntity.getLimit();
        // 查询房间列表
        List<RoomDto> roomDtos = roomMapper.getAllRooms(offset, getAllRoomsEntity.getLimit());

        return roomDtos;
    }

    @Override
    public RoomCreateDto createRoom(createRoomEntity createRoomEntity) {
        int currentId= Context.getCurrentUserId();
        Room room = new Room();
        room.setName(createRoomEntity.getName());
        room.setDescription(createRoomEntity.getDescription());
        room.setHavePwd(createRoomEntity.isHavePwd());
        room.setPassword(createRoomEntity.getPassword());
        room.setOwnerId((long)currentId);
        roomMapper.createRoom(room);

        RoomCreateDto roomCreateDto = new RoomCreateDto();
        roomCreateDto.setRoomId(room.getId().intValue());
        roomCreateDto.setName(room.getName());
        roomCreateDto.setDescription(room.getDescription());
        roomCreateDto.setOwnerId(currentId);
        roomCreateDto.setCreatedAt(LocalDateTime.now());

        return roomCreateDto;
    }
}
