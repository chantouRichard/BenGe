package com.bengebackend.mapper;

import com.bengebackend.model.Room;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomMapper {


    int createRoom(Room room);
}
