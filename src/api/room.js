// src/api/room.js
import request from './request';

//获取房间列表
export function getRoomList(page = 1, limit = 10) {
  return request.get('/room/list', {
    params: {
      page,
      limit
    }
  });
}
// 创建房间
export function createRoom(data) {
  return request.post('/room', data);
}

//申请加入房间
export function joinRoom(data) {
    return request.post('/room/application', data);
}