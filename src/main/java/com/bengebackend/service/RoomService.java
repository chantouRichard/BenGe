package com.bengebackend.service;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.dto.RoomDto;
import com.bengebackend.entity.room.createRoomEntity;
import com.bengebackend.entity.room.getAllRoomEntity;

import java.util.List;

public interface RoomService {
    RoomCreateDto   createRoom(createRoomEntity createRoomEntity);

    List<RoomDto> getAllRooms(getAllRoomEntity getAllRoomsEntity);


}
