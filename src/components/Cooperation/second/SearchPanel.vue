<template>
  <div class="search-panel">
    <img class="search-icon-left" src="@/assets/second/search1.png" alt="search icon" />
    <input type="text" placeholder="Search..." v-model="query" />
    <button class="search-button" @click="onSearch">
      <img class="search-icon-right" src="@/assets/second/search1.png" alt="search icon" />
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const query = ref('')

function onSearch() {
  // 触发搜索逻辑
  console.log('Search for:', query.value)
}
</script>

<style scoped>
.search-panel {
  position: fixed;
  top: 50px; /* margin-top 50px */
  left: 50%;
  transform: translateX(-50%);
  width: 400px;
  height: 50px;
  background: #E7F0F9;
  border-radius: 24px;
  box-shadow: 0 2px 6px rgb(0 0 0 / 0.1);

  display: flex;
  align-items: center;
  padding: 0 10px;
  gap: 10px;
  box-sizing: border-box;
  z-index: 1001;
}

.search-icon-left {
  width: 24px;
  height: 24px;
}

input {
  flex: 1;
  height: 32px;
  border: none;
  outline: none;
  font-size: 16px;

  background-color:#E7F0F9;
}

.search-button {
  width: 40px;
  height: 40px;
  background-color: #3b82f6; /* 蓝色背景 */
  border: none;
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-icon-right {
  width: 20px;
  height: 20px;
  filter: invert(100%); /* 白色图标，如果你的图标是黑色可以用这个反转 */
}
</style>
