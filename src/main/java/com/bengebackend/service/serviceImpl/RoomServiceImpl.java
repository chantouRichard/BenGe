package com.bengebackend.service.serviceImpl;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.mapper.RoomMapper;
import com.bengebackend.model.Room;
import com.bengebackend.service.RoomService;
import com.bengebackend.util.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomMapper roomMapper;

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
