<template>
  <div class="scrolling-tips">
    <transition name="slide-fade">
      <p :key="currentTip">{{ currentTip }}</p>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

// 定义滚动提示文本
const tips = [
  '这是第一条提示信息。',
  '第二条提示来啦！',
  '这里是第三条提示！',
  '不要忘了第四条提示。',
  '最后一条提示信息！'
];

// 当前显示的提示信息
const currentTip = ref(tips[0]);

// 当前提示信息的索引
let currentIndex = 0;

// 每3秒切换一条提示
const changeTip = () => {
  currentIndex = (currentIndex + 1) % tips.length;  // 循环切换
  currentTip.value = tips[currentIndex];
};

onMounted(() => {
  // 设置定时器，每3秒切换一次提示
  setInterval(changeTip, 3000);
});
</script>

<style scoped>
.scrolling-tips {
  position: fixed;
  bottom: 20px; /* 固定在屏幕左下角 */
  left: 20px;
  font-size: 24px; /* 字体大小 24px */
  color: black; /* 文字颜色黑色 */
  background-color: rgba(255, 255, 255, 0.7); /* 背景透明度适中 */
  padding: 10px 20px;
  border-radius: 8px;
  z-index: 9999;
  overflow: hidden;
  height: 50px; /* 给定固定高度，防止文本的动态改变高度影响布局 */
}

/* 滑动动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: transform 0.5s ease, opacity 0.5s ease;
}

.slide-fade-enter, 
.slide-fade-leave-to {
  transform: translateY(100%); /* 初始位置在屏幕下方 */
  opacity: 0;
}

.slide-fade-enter-to {
  transform: translateY(0); /* 进入时从下方滑入 */
  opacity: 1;
}

.slide-fade-leave-from {
  transform: translateY(0); /* 离开时从当前位置滑出 */
  opacity: 1;
}

.slide-fade-leave-to {
  transform: translateY(-100%); /* 离开时向上滑动 */
  opacity: 0;
}
</style>
