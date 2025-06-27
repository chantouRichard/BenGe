<template>
  <div class="main-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="logo">本格视界</div>
      <div class="title">BenGe.vision</div>
      <div class="user">
        <div class="quit-login" @click="logout()">退出登录</div>
        <div class="user-avatar">{{ username?.charAt(0) || '访' }}</div>
      </div>
    </div>

    <!-- 液态玻璃容器 -->
    <div class="glass-container">
      <h1 class="hub-title">创作空间</h1>
      <div class="mode-options">
        <div class="mode-card" @click="navigateTo('single')">
          <img src="@/assets/create-script.png" alt="单人创作" class="mode-icon" />
          <h3>单人创作</h3>
          <p>独立构建完整剧本</p>
        </div>
        <div class="mode-card" @click="navigateTo('canvas')">
          <img src="@/assets/cooperate.png" alt="团队协作" class="mode-icon" />
          <h3>团队协作</h3>
          <p>实时协同创作剧本</p>
        </div>
      </div>

      <!-- 最近创建 -->
      <h2 class="recent-title"><span>最近创作</span></h2>
      <div class="recent-section" v-if="recentScripts.length">
        <div class="recent-scroll" ref="scrollRef">
          <div
            v-for="script in recentScripts"
            :key="script.id"
            class="recent-card"
          >
            <img src="@/assets/user-menu-script.png" alt="Script Icon" class="recent-icon" />
            <div class="recent-info">
              <div class="recent-title-text">{{ script.title || '未命名剧本' }}</div>
              <div class="recent-date">{{ formatTimestamp(script.lastUpdated) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 页面底部背景 -->
    <div class="bottom-bg"></div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { userLoadingStore } from '@/stores/userLoadingStore';
import { usescriptStore } from '@/stores/scriptStore';
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';

const router = useRouter();
const loadingStore = userLoadingStore();
const scriptStore = usescriptStore();

const username = ref("");
const scrollRef = ref(null);

// 退出登录函数
const logout = () => {
  localStorage.removeItem('username');
  localStorage.removeItem('token');
  router.push('/');
};

// 计算最近的剧本
const recentScripts = computed(() => {
  if (!scriptStore.scripts || scriptStore.scripts.length === 0) return [];
  return [...scriptStore.scripts]
    .filter(script => !script.is_deleted)
    .sort((a, b) => new Date(b.lastUpdated) - new Date(a.lastUpdated))
    .slice(0, 3);
});

// 格式化时间戳
function formatTimestamp(timestamp) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

// 导航到指定模式的页面
function navigateTo(mode) {
  router.push(`/${mode}`);
}

// 鼠标滚轮横向滚动处理函数
function handleWheelScroll(event) {
  const el = scrollRef.value;
  if (!el) return;

  event.preventDefault(); // 阻止默认垂直滚动行为
  el.scrollLeft += event.deltaY * 0.5; // 调整滚动速度，乘以0.5以减缓滚动
}

onMounted(() => {
  username.value = localStorage.getItem('username');

  if (loadingStore.loading) {
    setTimeout(() => {
      loadingStore.hide();
    }, 1000);
  }

  scriptStore.loadScripts();

  // 绑定滚轮事件
  const el = scrollRef.value;
  if (el) {
    el.addEventListener('wheel', handleWheelScroll, { passive: false });
  }
});

onBeforeUnmount(() => {
  // 清理滚轮事件
  const el = scrollRef.value;
  if (el) {
    el.removeEventListener('wheel', handleWheelScroll);
  }
});
</script>

<style scoped>
.main-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(to bottom right, #e6f0ff, #f8fbff);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

/* 顶部栏 */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  color: #333;
  z-index: 3;
  background: linear-gradient(to bottom right, #e6f0ff, #f8fbff);
  padding: 10px 10px;
}

.logo {
  font-size: 20px;
}

.title {
  font-size: 16px;
  opacity: 0.6;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quit-login {
  font-size: 12px;
  opacity: 0.6;
  cursor: pointer;
}

.user-avatar {
  background-color: #3a7afe;
  color: #fff;
  padding: 6px 10px;
  border-radius: 50px;
  font-size: 14px;
}

/* 液态玻璃居中容器 */
.glass-container {
  padding: 40px 60px;
  background: linear-gradient(
    to bottom right,
    rgba(230, 240, 255, 0.3),
    rgba(248, 251, 255, 0.3)
  );
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
  text-align: center;
  overflow: visible;
  max-width: 100%;
  z-index: 3;
}

.hub-title {
  font-size: 32px;
  color: #F2F7FF;
  margin-bottom: 30px;
}

/* 模式卡片 */
.mode-options {
  display: flex;
  gap: 40px;
  justify-content: center;
  margin-bottom: 40px;
}

.mode-card {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px 30px;
  border-radius: 16px;
  cursor: pointer;
  width: 200px;
  transition: transform 0.2s;
}

.mode-card:hover {
  transform: translateY(-6px);
}

.mode-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 10px;
}

.mode-card h3 {
  font-size: 16px;
  margin-bottom: 6px;
  color: #222;
}

.mode-card p {
  font-size: 12px;
  color: #666;
}

/* 最近创建区域 */
.recent-section {
  position: relative; /* 使用相对定位 */
  width: 100%;
  overflow: visible; /* 防止溢出影响父容器 */
  padding: 10px 20px;
  box-sizing: border-box;
  z-index: 3;
}

.recent-scroll {
  display: flex;
  flex-wrap: nowrap;
  gap: 20px;
  white-space: nowrap;
  overflow-x: visible; /* 启用水平滚动 */
  scroll-behavior: smooth; /* 平滑滚动 */
  padding-bottom: 10px; /* 增加底部间距以避免滚动条遮挡 */
  -webkit-overflow-scrolling: touch; /* 优化移动端触摸滚动 */
}

.recent-card {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 16px;
  backdrop-filter: blur(16px);
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
  min-width: 150px;
  z-index: 2;
  position: relative;
}

.recent-card:hover {
  transform: translateY(-6px);
  z-index: 10;
}

.recent-icon {
  width: 30px;
  height: 30px;
  margin-right: 10px;
}

.recent-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recent-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.recent-date {
  font-size: 12px;
  color: #666;
}

.recent-title {
  display: flex;
  align-items: center;
  text-align: center;
  color: #4a6fa5;
  font-size: 20px;
  font-weight: 700;
  margin: 40px 0 16px;
}

.recent-title::before,
.recent-title::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, #d3e1f8, #eaf0ff);
  margin: 0 16px;
}

.bottom-bg {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('../assets/loginback.jpg');
  background-size: cover;
  z-index: 2;
}

/* 隐藏滚动条但保留滚动功能 */
.recent-scroll::-webkit-scrollbar {
  display: none; /* 隐藏 Webkit 浏览器的滚动条 */
}

.recent-scroll {
  -ms-overflow-style: none; /* 隐藏 IE/Edge 滚动条 */
  scrollbar-width: none; /* 隐藏 Firefox 滚动条 */
}
</style>