<template>
  <div>
    <!-- 容器本体 -->
    <div class="container" :class="{ collapsed: isCollapsed }">
        <Chat v-show="!isCollapsed"/>

      <div
        @click="toggleCollapse"
        class="toggle-btn"
        :class="{ 'collapsed-btn': isCollapsed }"
      >
        <img class="img" :src="currentIcon" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import chatIcon from '@/assets/second/chat.png'
import collapseIcon from '@/assets/second/collapse.png'
import Chat from '../Chat.vue'

const isCollapsed = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const currentIcon = computed(() => (isCollapsed.value ? chatIcon : collapseIcon))
</script>

<style scoped>
.container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 300px;
  height: 500px;
  background-color: white;
  transition: all 0.5s ease-in-out;
  border-radius: 8px;
  z-index: 9999;
  overflow: hidden;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  border: 4px solid black;
}

.container.collapsed {
  width: 150px;
  height: 150px;
  border-radius: 50% 50% 0 0;
  bottom: -75px;
  right: -75px;
}

/* 按钮基础样式 */
.toggle-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 9999;
  cursor: pointer;
  opacity: 1;
  transition: all 0.5s ease;
}

/* 折叠状态下的按钮样式 */
.toggle-btn.collapsed-btn {
  top: 30px;
  left: 30px;
  opacity: 0.6;
}

/* 图片样式 */
.img {
  width: 30px;
  height: 30px;
  object-fit: cover;
  pointer-events: none;
}
</style>
