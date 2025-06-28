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
          placeholder="搜索房间"
          clearable
          class="search-box"
        />
        <div class="header-buttons">
          <el-button @click="fetchRooms" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="create">创建房间</el-button>
        </div>
      </div>

      <!-- 房间列表区域（带滚动） -->
      <div class="table-wrapper">
        <!-- 空状态 -->
        <div v-if="!loading && filteredRooms.length === 0" class="empty-state">
          <el-empty description="暂无房间">
            <el-button type="primary" @click="create">创建第一个房间</el-button>
          </el-empty>
        </div>
        
        <!-- 房间列表 -->
        <el-table
          v-else
          :data="pagedRooms"
          style="width: 100%"
          v-loading="loading"
          height="400"
        >
          <el-table-column label="房间名称" min-width="200">
            <template #default="scope">
              <div class="room-name">
                <span>{{ scope.row.name }}</span>
                <el-tag 
                  v-if="isOwnedRoom(scope.row.roomId)" 
                  size="small" 
                  type="success"
                  style="margin-left: 8px;"
                >
                  我的房间
                </el-tag>
                <el-icon v-if="scope.row.havePwd" class="lock-icon" color="#f56c6c">
                  <Lock />
                </el-icon>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="创建者" min-width="120" />
          <el-table-column prop="currentMembers" label="参与人数" width="100" />
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button 
                type="text" 
                @click="enterRoom(scope.row)"
                :loading="joiningRoomId === scope.row.roomId"
                :disabled="joiningRoomId !== null"
              >
                进入
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
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
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh, Lock, House } from '@element-plus/icons-vue';
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

  const tryJoinRoom = async (password = '') => {
    try {
      const result = await joinRoom(room.roomId, password);
      console.log('加入房间结果：', result);

      // 后端成功时返回 "success"
      if (result === 'success') {
        ElMessage.success('成功加入房间！');
        // 跳转到房间页面
        router.push(`/room/${room.roomId}`);
        return true;
      } else if (result === '房间密码错误') {
        // 密码错误，需要重新输入密码
        return 'needPassword';
      } else {
        if(result == "您已经在房间中"){
          router.push(`/room/${room.roomId}`);
          return true;
        } else if (result.includes('您当前在房间')) {
          // 用户在其他房间中，提供切换选项
          return 'needSwitch';
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

    // 处理需要切换房间的情况
    if (result === 'needSwitch') {
      try {
        await ElMessageBox.confirm(
          `您当前在房间"${currentRoom.value?.name}"中，是否要退出当前房间并加入房间"${room.name}"？`,
          '切换房间',
          {
            confirmButtonText: '退出并加入',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        // 用户确认切换，先退出当前房间
        if (currentRoom.value) {
          await leaveRoom(currentRoom.value.id);
          ElMessage.success('已退出当前房间');
        }
        
        // 重新尝试加入新房间
        const switchResult = await tryJoinRoom();
        if (switchResult === true) {
          // 更新当前房间信息
          await fetchCurrentRoom();
        }
        
      } catch (error) {
        if (error === 'cancel') {
          // 用户取消切换
          return;
        }
        throw error;
      }
    }
    
    // 如果需要密码，弹出密码输入框并重试
    else if (result === 'needPassword') {
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
  gap: 10px;
  align-items: center;
}

.search-box {
  width: 300px;
}

.table-wrapper {
  overflow-y: auto;
  max-height: 400px;
  margin-bottom: 20px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.room-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lock-icon {
  font-size: 14px;
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
  color: #495057;
  margin-bottom: 10px;
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

