<template>
    <div class="main-page">
      <!-- 顶部栏 -->
      <div class="top-bar">
        <div class="logo">本格视界</div>
        <div class="title">BenGe.vision</div>
        <div class="user-avatar">{{ username?.charAt(0) || '访' }}</div>
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
          <div class="mode-card" @click="navigateTo('cooperate')">
            <img src="@/assets/cooperate.png" alt="团队协作" class="mode-icon" />
            <h3>团队协作</h3>
            <p>实时协同创作剧本</p>
          </div>
        </div>
  
        <!-- 最近创建 -->
        <div class="recent-section">
          <h2>最近创作</h2>
          <div class="recent-card">
            <img src="@/assets/user-menu-script.png" alt="Script Icon" class="recent-icon" />
            <div class="recent-info">
              <div class="recent-title">新剧本</div>
              <div class="recent-date">2025-06-25</div>
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
  const router = useRouter();
  const username = '叶'; // 模拟用户首字母
  
  function navigateTo(mode) {
    router.push(`/${mode}`);
  }
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
  }
  
  /* 顶部栏 */
  .top-bar {
    position: absolute;
    top: 20px;
    left: 40px;
    right: 40px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    font-size: 16px;
    color: #333;
  }
  
  .logo {
    font-size: 20px;
  }
  
  .title {
    font-size: 16px;
    opacity: 0.6;
  }
  
  .user-avatar {
    background-color: #3a7afe;
    color: #fff;
    padding: 6px 10px;
    border-radius: 20px;
    font-size: 14px;
  }
  
  /* 液态玻璃居中容器 */
  .glass-container {
    margin-top: 120px;
    padding: 40px 60px;
    background: rgba(255, 255, 255, 0.25);
    border-radius: 20px;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    text-align: center;
  }
  
  .hub-title {
    font-size: 32px;
    color: #1a1a1a;
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
  
  /* 最近创建 */
  .recent-section {
    text-align: left;
  }
  
  .recent-section h2 {
    font-size: 18px;
    margin-bottom: 10px;
    color: #1a1a1a;
  }
  
  .recent-card {
    background: #ffffff;
    border-radius: 12px;
    padding: 12px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  }
  
  .recent-icon {
    width: 24px;
    height: 24px;
  }
  
  .recent-title {
    font-weight: 600;
    font-size: 14px;
  }
  
  .recent-date {
    font-size: 12px;
    color: #999;
  }
  
  /* 页面底部背景图 */
  .bottom-bg {
    position: absolute;
    bottom: 0;
    width: 100%;
    height: 200px;
    background: url('../assets/loginback.jpg') no-repeat center bottom;
    background-size: cover;
    opacity: 0.4;
    z-index: -1;
  }
  </style>
  