<template>
  <div class="room-list-container">
    <!-- 顶部搜索和创建 -->
    <div class="header-bar">
      <el-input v-model="searchKeyword" placeholder="搜索房间" clearable class="search-box" />
      <el-button type="primary" @click="createRoom">创建房间</el-button>
    </div>

    <!-- 房间列表区域（带滚动） -->
    <div class="table-wrapper">
      <el-table :data="pagedRooms" style="width: 100%" v-loading="loading" height="400">
        <el-table-column prop="name" label="房间名称" />
        <el-table-column prop="creator" label="创建者" />
        <el-table-column prop="participants" label="参与人数" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="text" @click="enterRoom(scope.row)">进入</el-button>
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
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const searchKeyword = ref('')
const roomList = ref([])
const loading = ref(false)

const pageSize = ref(5)
const currentPage = ref(1)
const total = ref(0)

const filteredRooms = computed(() => {
  return roomList.value.filter(room =>
    room.name.includes(searchKeyword.value)
  )
})

watch(filteredRooms, (val) => {
  total.value = val.length
}, { immediate: true })

const pagedRooms = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRooms.value.slice(start, start + pageSize.value)
})

function fetchRooms() {
  loading.value = true
  setTimeout(() => {
    roomList.value = Array.from({ length: 50 }, (_, i) => ({
      name: `剧本房间 ${i + 1}`,
      creator: `用户${i + 1}`,
      participants: Math.floor(Math.random() * 8 + 1)
    }))
    loading.value = false
  }, 500)
}

function handlePageChange(page) {
  currentPage.value = page
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
}

function createRoom() {
  alert('跳转到创建房间页面')
}

function enterRoom(room) {
  alert(`进入房间：${room.name}`)
}

onMounted(() => {
  fetchRooms()
})
</script>

<style scoped>
.room-list-container {
  max-width: 900px;
  margin: 20px auto;
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
