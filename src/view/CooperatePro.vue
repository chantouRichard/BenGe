<template>
  <div class="room-list-container">
    <!-- 创建房间页面 -->
    <div v-if="showCreatePage">
      <h2 style="margin-bottom: 20px">创建新房间</h2>
      <h4 style="margin-bottom: 20px">基本信息</h4>
      <el-form label-width="80px">
        <el-form-item style="margin-bottom: 30px" label="房间名称">
          <el-input v-model="newRoom.name" />
        </el-form-item>
        <el-form-item style="margin-bottom: 30px" label="房间描述">
          <el-input v-model="newRoom.description" />
        </el-form-item>
        <div>
          <el-checkbox v-model="newRoom.havePwd" style="margin-bottom: 10px">
            设置房间密码
          </el-checkbox>

          <el-form-item label="房间密码">
            <el-input
              style="margin-bottom: 10px; width: 200px"
              v-model="newRoom.password"
              :disabled="!newRoom.havePwd"
              placeholder="请输入密码"
            />
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" @click="submitRoom">创建</el-button>
          <el-button @click="showCreatePage = false">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-else>
      <!-- 当前房间状态 -->
      <div v-if="currentRoom" class="current-room-info">
        <el-alert
          :title="`当前房间: ${currentRoom.name}`"
          type="info"
          show-icon
          :closable="false"
        >
          <template #default>
            <div class="current-room-content">
              <span>当前房间: {{ currentRoom.name }}</span>
              <div class="room-actions">
                <el-button 
                  type="text" 
                  size="small" 
                  @click="handleLeaveCurrentRoom"
                >
                  退出房间
                </el-button>
              </div>
            </div>
          </template>
        </el-alert>
      </div>
      
      <!-- 我的房间快速切换 -->
      <div v-if="ownedRooms.length > 0" class="owned-rooms-section">
        <div class="section-title">
          <el-icon><House /></el-icon>
          我的房间 ({{ ownedRooms.length }})
        </div>
        <div class="owned-rooms-list">
          <el-tag
            v-for="room in ownedRooms"
            :key="room.roomId"
            :type="currentRoom?.id === room.roomId ? 'success' : 'info'"
            class="room-tag"
            @click="quickSwitchRoom(room)"
            style="cursor: pointer; margin-right: 8px; margin-bottom: 8px;"
          >
            {{ room.name }}
            <span v-if="currentRoom?.id === room.roomId"> (当前)</span>
          </el-tag>
        </div>
      </div>
      
      <!-- 顶部搜索和创建 -->
      <div class="header-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索房间名称..."
          clearable
          class="search-box"
          :prefix-icon="Search"
        />
        <div class="header-buttons">
          <el-button @click="fetchRooms" :loading="loading" :icon="Refresh">
            刷新
          </el-button>
          <el-button type="primary" @click="create">
            <el-icon><Plus /></el-icon>
            创建房间
          </el-button>
        </div>
      </div>

      <!-- 房间卡片网格 -->
      <div class="room-cards-container" v-loading="loading">
        <!-- 空状态 -->
        <div v-if="!loading && filteredRooms.length === 0" class="empty-state">
          <el-empty description="暂无房间数据">
            <el-button type="primary" @click="create">
              <el-icon><Plus /></el-icon>
              创建第一个房间
            </el-button>
          </el-empty>
        </div>
        
        <!-- 房间卡片网格 -->
        <div v-else class="room-grid">
          <div 
            v-for="room in pagedRooms" 
            :key="room.roomId"
            class="room-card"
            :class="{ 
              'owned-room': isOwnedRoom(room.roomId), 
              'current-room': currentRoom?.id === room.roomId 
            }"
          >
            <!-- 卡片头部 -->
            <div class="card-header">
              <div class="room-title">
                <h3>{{ room.name }}</h3>
                <div class="room-badges">
                  <el-tag v-if="room.havePwd" type="warning" size="small">
                    <el-icon><Lock /></el-icon>
                    密码
                  </el-tag>
                  <el-tag v-if="isOwnedRoom(room.roomId)" type="success" size="small">
                    <el-icon><House /></el-icon>
                    我的
                  </el-tag>
                  <el-tag v-if="currentRoom?.id === room.roomId" type="info" size="small">
                    当前房间
                  </el-tag>
                </div>
              </div>
            </div>

            <!-- 卡片内容 -->
            <div class="card-content">
              <p class="room-description">{{ room.description || '暂无描述' }}</p>
              <div class="room-info">
                <div class="info-item">
                  <el-icon><User /></el-icon>
                  <span>{{ room.currentMembers }} 人</span>
                </div>
                <div class="info-item">
                  <span class="owner-name">创建者: {{ room.ownerName || '未知' }}</span>
                </div>
              </div>
            </div>

            <!-- 卡片操作 -->
            <div class="card-actions">
              <el-button 
                type="primary" 
                :loading="joiningRoomId === room.roomId"
                :disabled="joiningRoomId !== null"
                @click="enterRoom(room)"
                class="join-btn"
              >
                {{ isOwnedRoom(room.roomId) ? '进入房间' : '加入房间' }}
              </el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next, ->, sizes, total"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh, Lock, House, User, Plus, Search } from '@element-plus/icons-vue';
import {createRoom, getRoomList, joinRoom, getCurrentRoom, leaveRoom, getOwnedRooms, enterOwnedRoom} from "../api/room";

const router = useRouter();

const showCreatePage = ref(false);
const newRoom = ref({
  name: "",
  description: "",
  havePwd: false,
  password: "",
});

const searchKeyword = ref("");
const debouncedSearchKeyword = ref("");
const roomList = ref([]);
const loading = ref(false);
const joiningRoomId = ref(null); // 正在加入的房间ID
const currentRoom = ref(null); // 用户当前所在房间
const ownedRooms = ref([]); // 用户拥有的房间

const pageSize = ref(5);
const currentPage = ref(1);
const total = ref(0);

const filteredRooms = computed(() => {
  return roomList.value.filter((room) =>
    room.name.includes(debouncedSearchKeyword.value)
  );
});

watch(
  filteredRooms,
  (val) => {
    total.value = val.length;
  },
  { immediate: true }
);

const pagedRooms = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRooms.value.slice(start, start + pageSize.value);
});

async function fetchCurrentRoom() {
  try {
    const res = await getCurrentRoom();
    currentRoom.value = res;
  } catch (err) {
    // 用户可能不在任何房间中，这是正常情况
    currentRoom.value = null;
  }
}

async function fetchOwnedRooms() {
  try {
    const res = await getOwnedRooms();
    ownedRooms.value = Array.isArray(res) ? res : [];
  } catch (err) {
    ownedRooms.value = [];
  }
}

async function fetchRooms() {
  loading.value = true;
  try {
    // 并行获取房间列表、当前房间信息和拥有的房间
    const [roomListRes] = await Promise.all([
      getRoomList(1, 100),
      fetchCurrentRoom(),
      fetchOwnedRooms()
    ]);
    
    console.log('API响应数据：', roomListRes);

    // 后端直接返回房间数组
    if (Array.isArray(roomListRes)) {
      roomList.value = roomListRes;
    } else {
      console.warn('未知的响应格式：', roomListRes);
      roomList.value = [];
      ElMessage.error('获取房间列表失败：响应格式不正确');
    }
  } catch (err) {
    console.error('获取房间列表失败：', err);
    ElMessage.error('获取房间列表失败：' + (err.response?.data || err.message));
    roomList.value = [];
  } finally {
    loading.value = false;
  }
}

function handlePageChange(page) {
  currentPage.value = page;
}

function handleSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
}

function isOwnedRoom(roomId) {
  return ownedRooms.value.some(room => room.roomId === roomId);
}

function create() {
  showCreatePage.value = true;

  console.log("创建房间");

  
}

async function enterRoom(room) {
  console.log(`进入房间：${room.name}，房间ID：${room.roomId}`);
  
  // 防止重复点击
  if (joiningRoomId.value !== null) {
    return;
  }
  
  joiningRoomId.value = room.roomId;

  // 首先检查用户是否已在其他房间中
  if (currentRoom.value && currentRoom.value.id !== room.roomId) {
    try {
      await ElMessageBox.confirm(
        `您当前在房间"${currentRoom.value.name}"中，是否要退出当前房间并加入房间"${room.name}"？`,
        '切换房间',
        {
          confirmButtonText: '退出并加入',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      // 用户确认切换，先退出当前房间
      await leaveRoom(currentRoom.value.id);
      ElMessage.success('已退出当前房间');
      // 更新当前房间状态
      currentRoom.value = null;
      await fetchCurrentRoom();
      
    } catch (error) {
      if (error === 'cancel') {
        // 用户取消切换
        joiningRoomId.value = null;
        return;
      }
      throw error;
    }
  }

  const tryJoinRoom = async (password = '') => {
    try {
      const result = await joinRoom(room.roomId, password);
      console.log('加入房间结果：', result);

      // 后端成功时返回 "success"
      if (result === 'success') {
        ElMessage.success('成功加入房间！');
        // 跳转到房间页面
        router.push(`/room/${room.roomId}`);
        // 更新当前房间信息
        await fetchCurrentRoom();
        return true;
      } else if (result === '房间密码错误') {
        // 密码错误，需要重新输入密码
        return 'needPassword';
      } else {
        if(result == "您已经在房间中"){
          router.push(`/room/${room.roomId}`);
          return true;
        } else {
          ElMessage.error(result || '加入房间失败');
          return false;
        }
      }
    } catch (error) {
      console.error('加入房间请求失败：', error);
      throw error;
    }
  };

  try {
    // 先尝试不带密码加入
    const result = await tryJoinRoom();
    
    // 如果需要密码，弹出密码输入框并重试
    if (result === 'needPassword') {
      try {
        const { value } = await ElMessageBox.prompt(
          `房间"${room.name}"需要密码`, 
          '加入房间', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputType: 'password',
            inputPlaceholder: '请输入房间密码',
            inputValidator: (val) => {
              if (!val || val.trim() === '') {
                return '密码不能为空';
              }
              return true;
            }
          }
        );

        // 用输入的密码重新尝试加入
        const retryResult = await tryJoinRoom(value);
        if (retryResult === 'needPassword') {
          ElMessage.error('密码错误，请重试');
        }
      } catch (promptError) {
        if (promptError === 'cancel') {
          // 用户取消输入密码
          return;
        }
        throw promptError;
      }
    }
  } catch (error) {
    console.error('加入房间失败：', error);
    ElMessage.error('加入房间失败：' + (error.response?.data || error.message));
  } finally {
    joiningRoomId.value = null; // 重置加入状态
  }
}

// 搜索防抖
let searchTimer = null;
watch(searchKeyword, (newVal) => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  searchTimer = setTimeout(() => {
    debouncedSearchKeyword.value = newVal;
  }, 300);
});

async function quickSwitchRoom(room) {
  if (currentRoom.value?.id === room.roomId) {
    // 已经在当前房间，直接进入
    router.push(`/room/${room.roomId}`);
    return;
  }
  
  // 切换到自己拥有的房间
  joiningRoomId.value = room.roomId;
  try {
    await enterOwnedRoom(room.roomId);
    ElMessage.success(`已切换到房间: ${room.name}`);
    router.push(`/room/${room.roomId}`);
  } catch (error) {
    console.error('切换房间失败：', error);
    ElMessage.error('切换房间失败：' + (error.response?.data || error.message));
  } finally {
    joiningRoomId.value = null;
  }
}

async function handleLeaveCurrentRoom() {
  if (!currentRoom.value) return;
  
  try {
    await ElMessageBox.confirm(
      `确定要退出房间"${currentRoom.value.name}"吗？`,
      '退出房间',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const result = await leaveRoom(currentRoom.value.id);
    if (result === 'success') {
      ElMessage.success('已退出房间');
      currentRoom.value = null;
      // 刷新房间列表
      await fetchRooms();
    } else {
      ElMessage.error(result || '退出房间失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出房间失败：', error);
      ElMessage.error('退出房间失败：' + (error.response?.data || error.message));
    }
  }
}

onMounted(() => {
  fetchRooms();
  debouncedSearchKeyword.value = searchKeyword.value;
});

async function submitRoom() {
  const { name, description, havePwd, password } = newRoom.value;

  // 校验房间名称
  if (!name || name.trim() === "") {
    ElMessage.error("房间名称不能为空！");
    console.log("房间描述",description);
    return;
  }

  // 校验密码（如果勾选了设置密码）
  if (havePwd && (!password || password.trim() === "")) {
    ElMessage.error("请填写房间密码！");
    return;
  }

  // 通过验证后提交
  console.log("提交的房间信息：", newRoom.value);
  const payload = {
    name: name.trim(),
    description: description?.trim() || '',
    havePwd: havePwd, // 直接传递 boolean 值
    password: havePwd ? password.trim() : ''
  };

  try {
    const res = await createRoom(payload);
    console.log('创建房间响应：', res);

    // 后端直接返回 RoomCreateDto 对象
    if (res && res.roomId) {
      ElMessage.success('房间创建成功！');
      showCreatePage.value = false;
      // 重置表单
      newRoom.value = {
        name: "",
        description: "",
        havePwd: false,
        password: "",
      };
      // 重新获取房间列表
      await fetchRooms();
      // 跳转到房间页面
      router.push(`/room/${res.roomId}`);
    } else {
      ElMessage.error('创建失败：响应格式不正确');
    }
  } catch (err) {
    console.error('创建房间失败：', err);
    ElMessage.error('请求失败：' + (err.response?.data || err.message));
  }
}

</script>

<style scoped>
.room-list-container {
  max-width: 900px;
  margin: 100px auto;
  padding: 20px;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  background-color: #fff;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.header-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-box {
  width: 320px;
  margin-right: 12px;
}

.room-cards-container {
  margin-bottom: 20px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.room-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.room-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.room-card.owned-room {
  border-color: #67c23a;
  background: linear-gradient(135deg, #fff 0%, #f0f9ff 100%);
}

.room-card.current-room {
  border-color: #409eff;
  background: linear-gradient(135deg, #fff 0%, #ecf5ff 100%);
}

.card-header {
  padding: 16px 16px 0;
}

.room-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.room-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-badges {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.card-content {
  padding: 0 16px 16px;
}

.room-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 12px 0;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.room-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.info-item .el-icon {
  font-size: 14px;
}

.owner-name {
  color: #909399;
}

.card-actions {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafbfc;
}

.join-btn {
  width: 100%;
  height: 36px;
  font-weight: 500;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.current-room-info {
  margin-bottom: 15px;
}

.current-room-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.room-actions {
  display: flex;
  gap: 8px;
}

.owned-rooms-section {
  margin-bottom: 15px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .room-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .header-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .search-box {
    width: 100%;
    margin-right: 0;
  }
  
  .header-buttons {
    justify-content: center;
  }
  
  .room-cards-container {
    padding: 0 10px;
  }
}

@media (max-width: 480px) {
  .room-list-container {
    margin: 50px auto;
    padding: 15px;
  }
  
  .room-title h3 {
    font-size: 16px;
    max-width: 150px;
  }
  
  .room-badges {
    align-items: flex-start;
  }
}

.owned-rooms-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-tag {
  transition: all 0.3s ease;
}

.room-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
</style>

