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
    <div v-if="stage == 0" class="main-area">
      <RoleSelector
        :roles="roles"
        @selected="handleRoleSelect"
        @confirm="stage++"
      />
    </div>

    <div v-else-if="stage == 1">
      <EditArea
        :roles="roles"
        :userRole="userRole"
        @nextStage="changeStage(2)"
      />

        <SearchPanel />
        <MemberList />
        <TaskCard :role="userRole" :content="socketState.roles[socketState.userRole].task" />
        <ScollingTips/>
        <ChatPanel/>
      
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from "vue";
import { ref } from "vue";
import MemberList from "./second/MemberList.vue";
import SearchPanel from "./second/SearchPanel.vue";
import TaskCard from "./second/TaskCard.vue";
import ScollingTips from "./second/ScollingTips.vue";
import ChatPanel from "./second/ChatPanel.vue";

import { defineEmits } from "vue";
import { socketState } from "@/stores/socket";

// 角色选择区
import RoleSelector from "./second/RoleSelector.vue";

function handleRoleSelect(index) {
  userRole.value = index;
  stage.value = 1;
}

// 侧边栏成员信息
import SidePanel from "./second/SidePanel.vue";

function handleMemberClick(data) {
  console.log("点击了成员：", data.member);
  selectedMember.value = data.member;
  showOther.value = true;
  isPanelCollapsed.value = false;
  // TODO: 打开他的工作区
}

// 编辑区
import EditArea from "./second/EditArea.vue";

const emit = defineEmits(["updateStage"]);

const changeStage = (newStage) => {
  emit("updateStage", newStage);
};

// 折叠面板状态
const selectedMember = ref(null);
const showOtherPanel = ref(false);
const showOther = ref(false);
const isPanelCollapsed = ref(false);

// const isMemberOpen = ref(false);

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

const members = ref([]);

let provider;

onMounted(async () => {});

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
/* 折叠面板 */

/* 外部容器（包含按钮 + 面板） */
/* 侧边栏容器（整体一起滑动） */
.side-panel-container {
  position: fixed;
  top: 0;
  right: 0;
  width: 350px;
  height: 100%;

  display: flex;
  justify-content: center;
  align-items: center;

  flex-direction: row;

  /* 亚克力风格核心 */
  background: rgba(255, 255, 255, 0.15); /* 半透明白色 */
  backdrop-filter: blur(12px); /* 毛玻璃模糊效果 */
  -webkit-backdrop-filter: blur(12px); /* Safari 支持 */

  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.2); /* 柔和阴影 */
  border-left: 1px solid rgba(255, 255, 255, 0.2); /* 细边界线 */

  transition: transform 0.3s ease;
  z-index: 9999;
  border-top-left-radius: 12px;
  border-bottom-left-radius: 12px;
}

/* SidePanel 区域 */
.side-panel-content {
  width: 100%;
  height: 80%;
  overflow: hidden;

  display: flex;
  flex-direction: column;
  justify-content: center;

  align-items: center;
}

/* 垂直居中的按钮 */
.side-panel-toggle {
  position: absolute;
  left: -24px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 60px;

  background-color: white;
  color: black;
  border: none;
  border-top-left-radius: 16px;
  border-bottom-left-radius: 16px;

  cursor: pointer;
  z-index: 9999;

  box-shadow: 0 0 6px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.container {
  width: 100%;
  height: 100vh;
  /* 使用视口高度，确保充满整个屏幕 */

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
.main-area {
  display: flex;
  padding: 32px;
  gap: 20px;
  /* 子区域间距 */
  width: calc(100% - 300px);
  min-width: 827px;
  margin: auto;
  margin-top: 50px;
  background-color: rgba(0, 0, 0, 0.3);
  /* 半透明白色背景 */
  backdrop-filter: blur(8px);
  /* 毛玻璃模糊效果 */
  height: calc(100% - 150px);
  min-height: 500px;
  box-sizing: border-box;
  /* 确保 padding 不超宽 */
  border-radius: 20px;

  /* 其他样式 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 轻微的边框，增加层次感 */
}

.right-area {
  gap: 20px;
  display: flex;
  flex-direction: column;
  width: 34%;
  min-width: 312px;

  height: 100%;
}
</style>
