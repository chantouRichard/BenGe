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
      <!-- 顶部搜索和创建 -->
      <div class="header-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索房间"
          clearable
          class="search-box"
        />
        <el-button type="primary" @click="create">创建房间</el-button>
      </div>

      <!-- 房间列表区域（带滚动） -->
      <div class="table-wrapper">
        <el-table
          :data="pagedRooms"
          style="width: 100%"
          v-loading="loading"
          height="400"
        >
          <el-table-column prop="name" label="房间名称" />
          <el-table-column prop="description" label="创建者" />
          <el-table-column prop="participants" label="参与人数" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button type="text" @click="enterRoom(scope.row)"
                >进入</el-button
              >
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
import { ElMessage } from "element-plus";
import {createRoom} from "../api/room";

const router = useRouter();

const showCreatePage = ref(false);
const newRoom = ref({
  name: "",
  description: "",
  havePwd: true,
  password: "",
});

const searchKeyword = ref("");
const roomList = ref([]);
const loading = ref(false);

const pageSize = ref(5);
const currentPage = ref(1);
const total = ref(0);

const filteredRooms = computed(() => {
  return roomList.value.filter((room) =>
    room.name.includes(searchKeyword.value)
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

function fetchRooms() {
  loading.value = true;
  setTimeout(() => {
    roomList.value = Array.from({ length: 50 }, (_, i) => ({
      name: `剧本房间 ${i + 1}`,
      description: `用户${i + 1}`,
      participants: Math.floor(Math.random() * 8 + 1),
    }));
    loading.value = false;
  }, 500);
}

function handlePageChange(page) {
  currentPage.value = page;
}

function handleSizeChange(size) {
  pageSize.value = size;
  currentPage.value = 1;
}

function create() {
  showCreatePage.value = true;

  console.log("创建房间");

  
}

function enterRoom(room) {
  console.log(`进入房间：${room.name}`);


}

onMounted(() => {
  fetchRooms();
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
    havePwd: havePwd ? 1 : 0,
    password: havePwd ? password : ''
  };

  try {
    const res = await createRoom(payload);
    if (res.code === 200) {
      ElMessage.success('房间创建成功！');
      router.push(`/room/${res.data.roomId}`);
    } else {
      ElMessage.error(res.msg || '创建失败');
    }
  } catch (err) {
    ElMessage.error('请求失败：' + (err.response?.data?.msg || err.message));
  }

  showCreatePage.value = false;
  ElMessage.success("房间创建成功！");

  // 跳转到房间页面
  router.push("/room");
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

.search-box {
  width: 300px;
}

.table-wrapper {
  overflow-y: auto;
  max-height: 400px;
  margin-bottom: 20px;
}
</style>
