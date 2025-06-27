// src/api/room.js
import request from './request';

//获取房间列表
export function getRoomList(page = 1, limit = 10) {
  return request.post('/room/list', {
    page,
    limit
  });
}

// 创建房间
export function createRoom(data) {
  const roomData = {
    ...data,
    havePwd: Boolean(data.havePwd)
  };
  return request.post('/room', roomData);
}

//申请加入房间
export function joinRoom(roomId, password = '', applyReason = '') {
    return request.post('/room/application', {
        roomId,
        password,
        applyReason
    });
}