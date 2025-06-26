package com.bengebackend.mapper;

import com.bengebackend.dto.RoomDto;
import com.bengebackend.model.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {


    int createRoom(Room room);

    List<RoomDto> getAllRooms(@Param("offset") int offset, @Param("limit") int limit);
}
