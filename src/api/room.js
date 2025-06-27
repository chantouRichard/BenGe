// src/api/room.js
import request from './request';

//获取房间列表
export function getRoomList() {
  return request.get('/api/room');
}
// 创建房间
export function createRoom(data) {
  return request.post('/api/room', data);
}

//申请加入房间
export function joinRoom(data) {
    return request.post('/api/room/application', data);
}