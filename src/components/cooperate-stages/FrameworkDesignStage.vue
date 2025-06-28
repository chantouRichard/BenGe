<template>
  <div class="container">
    <div class="header">
      <div class="logo">BenGe Vision</div>
      <div class="right-menu">
        <div class="menu-item">我的剧本</div>
        <div class="menu-item">帮助</div>
        <img src="../../assets/login.png" alt="avatar" class="avatar" />
      </div>
    </div>
    <!-- <h2 style="margin: 20px 0px 0px 50px; color: white">第二阶段：共同编辑</h2> -->
    <div v-if="stage == 0" class="choose-area">
      <div
        style="
          display: flex;
          margin: 0px 20px 20px;
          justify-content: space-between;
          width: 100%;
        "
      >
        <h2 style="color: white">请选择你的角色</h2>
        <button class="button" style="margin-left: auto" @click="stage++">
          选择完毕
        </button>
      </div>
      <div
        style="
          display: flex;
          width: 100%;
          margin: auto;
          min-width: 700px;
          justify-content: space-evenly;
          gap: 20px;
        "
      >
        <div v-for="(role, index) in roles" :key="index" class="role-wrapper">
          <div class="card-container">
            <img
              :src="require(`@/assets/second/role${index + 1}.jpeg`)"
              alt="角色图片"
              class="role-image"
              @click="
                role = index;
                stage = 1;
              "
            />
          </div>
          <div class="role-text">{{ roles[index].name }}</div>
          <div class="role-description">{{ roles[index].description }}</div>
        </div>
      </div>
    </div>
    <div v-else-if="stage == 1" class="main-area">
      <div class="edit-area">
        <div style="display: flex; margin: 0px 20px 20px">
          <h2>
            第二阶段:分工设计阶段<br />你的角色是：{{ roles[userRole].name }}
          </h2>
          <button class="button" @click="changeStage(1)">下一阶段</button>
        </div>
        <teleport to="body">
            <transition name="fullscreen-slide">
          <div class="canvas fullscreen" v-if="isFullScreen">
            <!-- 全屏版本的 canvas -->
            <button
              class="canvasButton"
              @click="isFullScreen = false"
              :style="{
                backgroundImage: `url(${require('@/assets/second/reduce.png')})`,
              }"
            ></button>
            <div style="padding: 20px;">
            <NarrativeWorkspace />
            </div>
          </div>
          </transition>
        </teleport>

        <div class="canvas">
          <button
            class="canvasButton"
            @click="isFullScreen = true"
            :style="{
              backgroundImage: `url(${require('@/assets/second/enlarge.png')})`,
            }"
          ></button>
          <div style="padding: 20px;height: 100%;">
            <NarrativeWorkspace />
            </div>
        </div>
      </div>

      <div class="right-area">
        <!-- 成员区 -->
        <div class="member-area" :class="{ collapsed: !isMemberOpen }">
          <div class="member-header">
            <h2 style="min-width: 72px">成员区</h2>
            
          </div>
          <transition name="collapse">
            <div v-if="isMemberOpen" class="member-list">
              <div
                class="member-item"
                v-for="member in members"
                :key="member.id"
              >
                <img :src="member.avatar" alt="avatar" class="member-avatar" />
                <div style="display: flex; align-items: flex-start;height: 100%;margin-left: 10px;">
    <span class="member-name">{{ member.name }}</span>
</div>

              </div>
            </div>
          </transition>
        </div>

        <!-- 聊天区 -->
        <div
          class="chat-area"
          :style="{ height: isMemberOpen ? '80%' : '95%' }"
        >
          <div
            style="
              width: 100%;
              height: 100%;
              margin-left: auto;
              margin-right: auto;
              position: relative;
              overflow: hidden;
            "
          >
            <Chat 
              :roomId="roomId"
              :userId="userId" 
              @membersUpdated="updateMembers" 
              :avatar="userAvatar" 
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import NarrativeWorkspace from "../roles/NarrativeWorkspace.vue";
import { onMounted, onBeforeUnmount } from "vue";
import Chat from "./Chat.vue";
// import loginImage from "../../assets/login.png";
import { ref, computed } from "vue";
import { useRoute } from "vue-router";

import { defineEmits } from "vue";

// 控制全屏
const isFullScreen = ref(false);

const emit = defineEmits(["updateStage"]);

const route = useRoute();

// 从路由参数获取房间ID，如果没有则使用默认值1
const roomId = computed(() => {
  return parseInt(route.params.roomId) || 1;
});

const changeStage = (newStage) => {
  emit("updateStage", newStage);
};

const isMemberOpen = ref(true);



const userId = "user_" + Math.floor(Math.random() * 1000);
// const userName = "用户_" + userId.slice(-3);
const randomAvatar = Math.floor(Math.random() * 5 + 1); // 1 到 5
const userAvatar = require(`@/assets/avatar/${randomAvatar}.jpg`);
const userRole = ref(0);

const roles = ref([
  {
    name: "剧情设计师",
    description:
      "擅长构建故事主线与反转，通过精妙布局勾勒出跌宕起伏的剧情，掌控节奏与情感张力，引导玩家沉浸在虚构与现实交织的世界中。",
  },
  {
    name: "角色设计师",
    description:
      "负责塑造人物性格与关系网络，为每一个角色赋予鲜明动机与成长轨迹，让玩家在扮演中感受真实的情感与冲突。",
  },
  {
    name: "线索设计师",
    description:
      "精于埋设线索与误导，通过巧妙布局隐藏真相，引导推理节奏，确保玩家在抽丝剥茧中感受层层惊喜与挑战。",
  },
  {
    name: "氛围设计师",
    description:
      "以视觉、音效与文本语言营造沉浸式体验，塑造紧张或诡秘的氛围，让每一处场景都充满戏剧张力，增强整体代入感。",
  },
]);

const stage = ref(0);

// 固定使用roomId为1进行测试
// const roomId = 1;
const members = ref([]);

let provider;

onMounted(async () => {
  // 设置本地用户信息
  //   provider.awareness.setLocalStateField("user", {
  //     id: userId,
  //     name: userName,
  //     avatar: userAvatar,
  //   });
});

onBeforeUnmount(() => {
  provider?.destroy();
});

function updateMembers(membersList) {
  // 直接更新整个成员列表
  members.value = membersList.map((member) => ({
    id: member.id,
    name: member.username,
    avatar: member.avatar || userAvatar,
  }));
  console.log("成员列表已更新:", members.value);
}
</script>

<style scoped>
.role-wrapper {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  transition: transform 0.3s ease;
  position: relative; /* ✅ 卡片允许自身浮动 */
}

/* 悬浮时卡片整体上移 */
.role-wrapper:hover {
  transform: translateY(-100px); /* ✅ 只这个卡片上移 */
  z-index: 1; /* ✅ 避免 hover 卡片被其他卡片遮住 */
}

/* 整个卡片容器：木纹背景 + 图片 */
.card-container {
  width: 100%;
  height: 240px;
  padding: 10px;
  background-image: url("@/assets/second/wood.jpg");
  background-size: cover;
  background-repeat: no-repeat;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.3s ease;
}

/* 图片本身只是填充，无需动画 */
.role-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 5px;
}

/* 文字样式 + 动画 */
.role-text {
  font-size: 24px;
  color: white;
  transition: font-size 0.3s ease;
}

/* 悬浮：整个卡片旋转 + 缩放；文字变大 */
.role-wrapper:hover .card-container {
  transform: scale(1.1) rotate(4deg);
}

.role-wrapper:hover .role-text {
  font-size: 28px;
}
/* 介绍文字默认透明 + 占位 */
.role-description {
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #555;
  font-size: 14px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  overflow: hidden;
}

/* 悬浮时显示文字 */
.role-wrapper:hover .role-description {
  opacity: 1;
  color: white;
  font-size: 18px;
}

.canvas {
  position: relative;
  width: 100%;
  height: calc(100% - 100px);

  background-image: url(~@/assets/second/canvas.png);

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  overflow: hidden;

  transition: all 0.5s ease;
}

.canvas.fullscreen {
  position: fixed; /* 使其浮动在最顶层 */
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999; /* 保证在最顶层 */
  background-color: white; /* 如需白底，或保持透明 */

  /* ✅ 关键调整项 */
  background-size: 120% auto; /* 放大宽度，高度自适应 */
  background-position: center center; /* 始终聚焦中心 */
  background-repeat: no-repeat;

  transition: all 0.5s ease;
}
/* 动画进入 */
.fullscreen-slide-enter-active {
  animation: slideDown 0.5s ease-out forwards;
}

/* 动画离开 */
.fullscreen-slide-leave-active {
  animation: slideUp 0.5s ease-in forwards;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(0);
    opacity: 1;
  }
  to {
    transform: translateY(100%);
    opacity: 0;
  }
}

.canvasButton {
  width: 32px;
  height: 32px;

  border: none;
  position: absolute; /* 绝对定位 */
  top: 20px; /* 距离父容器顶部 10px */
  right: 20px; /* 距离父容器右侧 10px */
  object-fit: cover;

  background: transparent;

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  z-index: 900;
  transition: all 0.3s ease;
}

.canvasButton:hover {
  width: 40px;
  height: 40px;
}

/* 折叠/展开动画 */
.collapse-enter-active,
.collapse-leave-active {
  transition: max-height 0.3s ease, opacity 0.3s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  max-height: 500px; /* 根据实际内容高度设大一点 */
  opacity: 1;
}

.member-header {
  display: flex;
  height: 20px;

  align-items: center;
}
.toggle-btn {
  height: 30px;
  width: 30px;

  border: none;
  background: transparent;
  color: white;
}
.member-area {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  background: black;
  color: white;

  border-radius: 8px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;

  /* ❌ 移除固定高度 */
  height: 32%;

}

.member-list {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  width: 100%;
  max-width: 100%;
  gap: 24px;

  overflow-y: auto;

}

.member-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 4px;
}

.member-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #ccc;
}

.member-name {
  font-size: 12px;
  color: white;

  width: 54px;
  overflow: hidden; /* 隐藏溢出的内容 */
  text-overflow: ellipsis; /* 超出部分显示省略号 */
  white-space: nowrap; /* 不换行 */
}

.container {
  width: 100%;
  height: 100vh; /* 使用视口高度，确保充满整个屏幕 */

  background-image: url("@/assets/second/secondback2.jpeg");
  background-size: cover;
  background-repeat: no-repeat;
}
.header {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 60px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.logo {
  font-size: 22px;
  font-weight: bold;
}

.right-menu {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 30px;
}

.menu-item {
  cursor: pointer;
  font-size: 16px;
  color: #333;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: gray;
}

.choose-area {
  display: flex;
  flex-direction: column;
  padding: 32px;
  gap: 20px; /* 子区域间距 */
  width: calc(100% - 300px);
  min-width: 827px;
  margin: auto;
  margin-top: 50px;
  background-color: rgba(0, 0, 0, 0.3); /* 半透明白色背景 */
  backdrop-filter: blur(8px); /* 毛玻璃模糊效果 */
  height: calc(100% - 150px);
  min-height: 500px;
  box-sizing: border-box; /* 确保 padding 不超宽 */
  border-radius: 20px;

  /* 其他样式 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 轻微的边框，增加层次感 */
}
.main-area {
  display: flex;
  padding: 32px;
  gap: 20px; /* 子区域间距 */
  width: calc(100% - 300px);
  min-width: 827px;
  margin: auto;
  margin-top: 50px;
  background-color: rgba(0, 0, 0, 0.3); /* 半透明白色背景 */
  backdrop-filter: blur(8px); /* 毛玻璃模糊效果 */
  height: calc(100% - 150px);
  min-height: 500px;
  box-sizing: border-box; /* 确保 padding 不超宽 */
  border-radius: 20px;

  /* 其他样式 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 轻微的边框，增加层次感 */
}

.edit-area {
  width: 66%;
  min-width: 442px;
  background: transparent;
  color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.right-area {
  gap: 20px;
  display: flex;
  flex-direction: column;
  width: 34%;
  min-width: 312px;

  height: 100%;
}
.chat-area {
  height: 68%;
  max-height: 475px;
  display: flex;
  flex-direction: column;

  background: transparent;
  color: hsl(from color h s l);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.button {
  height: 40px;
  background-color: #2712bb;
  color: white;

  border-radius: 10px;
}
</style>
