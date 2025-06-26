package com.bengebackend.service;

import com.bengebackend.dto.RoomCreateDto;
import com.bengebackend.entity.room.createRoomEntity;

public interface RoomService {
    RoomCreateDto   createRoom(createRoomEntity createRoomEntity);


}
