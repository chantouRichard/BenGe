<template>
  <!-- 任务卡片 -->
  <div
    class="task-card"
    :style="{
      transform: isCardVisible ? 'translateX(0)' : 'translateX(-100%)',
    }"
  >
    <div class="task-card-header">
      <h1>你的角色：{{}}</h1>
      <h2>{{ title }}</h2>
    </div>
    <p class="task-text">{{ taskText }}</p>
    <button @click="toggleCard" class="toggle-btn">
      {{ !isCardVisible ? ">" : "<" }}
    </button>
  </div>

  <!-- 控制卡片显示的固定按钮 -->
</template>

<script setup>
import { ref } from "vue";

// 接收父组件传入的属性
const props = defineProps({
  role: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  taskText: {
    type: String,
    required: true,
  },
});

// 控制任务卡片是否显示
const isCardVisible = ref(true);
const cardPosition = ref(20); // 初始位置在屏幕外

// 控制卡片滑动进入和退出
const toggleCard = () => {
  isCardVisible.value = !isCardVisible.value;
  cardPosition.value = isCardVisible.value ? 20 : -220; // 进入和退出的动画
};
</script>

<style scoped>
/* 任务卡片样式 */
.task-card {
  position: fixed;
  top: 20px;
  left: 20px;
  transform: translateY(-50%);
  width: 300px;
  height: 300px;
  backdrop-filter: blur(10px); /* 毛玻璃效果 */
  border-radius: 10px;
  padding: 20px;
  color: white;
  transition: left 0.3s ease; /* 滑动动画 */
  z-index: 9999;
  background: transparent;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);

  -webkit-backdrop-filter: blur(12px); /* Safari 支持 */

  border-right: 1px solid rgba(255, 255, 255, 0.2); /* 细边界线 */

  transition: transform 0.3s ease;

  border-width: 4px;
  border-color: black;
}

/* 任务卡片头部 */
.task-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  color: black;
}

h2 {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.toggle-btn {
  background: transparent;
  border: none;
  color: white;
  background-color: black;
  font-size: 20px;
  cursor: pointer;

  position: absolute;
  left: 300px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 60px;

  border-top-right-radius: 16px;
  border-bottom-right-radius: 16px;

  z-index: 9999;

  box-shadow: 0 0 6px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

/* 任务文本 */
.task-text {
  font-size: 14px;
  margin-top: 10px;
  color: #000000;
}

/* 控制卡片显示的固定按钮 */
.control-btn {
  position: fixed;
  top: 300px;
  left: 20px;
  background-color: white;
  color: black;
  border-color: black;
  border-width: 4px;
  padding: 12px 20px;
  border-radius: 50px;
  cursor: pointer;
  font-size: 16px;
  transition: transform 0.3s ease;
  z-index: 9999;
}

.control-btn:hover {
  transform: scale(1.1);
}
</style>
