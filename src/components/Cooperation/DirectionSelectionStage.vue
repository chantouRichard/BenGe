<template>
  <div class="direction-selection-stage">
    <div class="header">
      <div class="left-menu">
        <div class="stage-title">
          <i class="fa-solid fa-bars-staggered title-icon"></i>
          <span class="title">方向选择</span>
        </div>
      </div>
      <div class="right-menu">
        <div class="back-image"></div>
        <div class="menu-front">
          <i class="fa-solid fa-scroll logo"></i>
          <div class="menu-group">
            <div class="menu-item">BenGe.Vision</div>
            <i class="fa-solid fa-circle-info menu-icon"></i>
            <img src="../../assets/login.png" alt="avatar" class="avatar" />
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧区域 -->
      <div class="left-panel">
        <DirectionSelect
            v-if="!showVoteStage"
            @confirm="handleDirectionConfirm"
            class="direction-select-component"
        />
        <VoteStage
            v-else
            :room-id="roomId"
            :members="members"
            :all-directions="allDirections"
            @submitVote="handleVoteSubmit"
            @regenerateSuggestion="handleRegenerateRequest"
            class="vote-stage-component"
        />
      </div>

      <!-- 右侧区域 -->
      <div class="right-panel">
        <!-- 成员区 -->
        <MemberList members="members" />

        <!-- 聊天区 -->
        <Chat
            :room-id="String(roomId)"
            :user-id="currentUser.id"
            :user-name="currentUser.name"
            :avatar="currentUser.avatar"
            @membersUpdated="updateMembers"
            class="chat-component"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import DirectionSelect from '@/components/Cooperation/first/DirectionSelect.vue';
import VoteStage from "@/components/Cooperation/first/VoteStage.vue";
import Chat from './Chat.vue';
import loginImage from '../../assets/login.png';
import { socketState } from '@/stores/socket';
import MemberList from "@/components/Cooperation/first/MemberList.vue";

const route = useRoute();
const isMemberOpen = ref(true);
const showVoteStage = ref(false);
const allDirections = ref([]); // 存储所有成员选择的方向

// 获取房间ID
const roomId = computed(() => {
  return parseInt(route.params.roomId) || 1;
});

// 从localStorage获取用户信息
const currentUser = computed(() => {
  const username = localStorage.getItem('username') || '匿名用户';
  return {
    id: socketState.userId || `user_${username}_${Date.now()}`,
    name: username,
    avatar: loginImage,
  };
});

const toggleMemberArea = () => {
  isMemberOpen.value = !isMemberOpen.value;
};

const updateMembers = (membersList) => {
  if (!Array.isArray(membersList)) return;

  const currentUserId = String(currentUser.value.id);

  // 更新socketState中的成员数据
  socketState.members = membersList
      .filter(member =>
          member &&
          member.id != null &&
          String(member.id) !== currentUserId &&
          member.roomId === roomId.value
      )
      .map(m => ({
        id: m.id,
        username: m.name || m.username || '匿名用户',
        avatar: m.avatar || loginImage,
        selectedDirections: m.selectedDirections || []
      }));
};

const handleDirectionConfirm = (selectedDirections) => {
  // json格式发送到后端并收集所有成员的选择
  socketState.socket.send(JSON.stringify({
    type: 'submit_directions',
    roomId: roomId.value,
    directions: selectedDirections
  }))
  allDirections.value.push(selectedDirections);

  showVoteStage.value = true;
};

// 投票
const handleVoteSubmit = (voteData) => {
  socketState.socket.send(JSON.stringify({
    type: 'submit_vote',
    roomId: roomId.value,
    directions: voteData.directions
  }))
}

const handleRegenerateRequest = (requestData) => {
  socketState.socket.send(JSON.stringify({
    type: 'regenerate_suggestion',
    roomId: roomId.value,
    modification: requestData.modification
  }))
}

// 监听所有方向数据
watch(() => socketState.directionDate, (newData) => {
  if (newData && newData.roomId === roomId.value) {
    allDirections.value = newData.allDirections;
    showVoteStage.value = true;
  }
}, { deep: true });

defineExpose({
  roomId,
  currentUser,
  isMemberOpen,
  toggleMemberArea,
  updateMembers,
  handleDirectionConfirm,
  showVoteStage,
  allDirections,
});
</script>

<style scoped>
.direction-selection-stage {
  width: 98%;
  height: 95vh;
  margin: 10px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(90deg, #EDEEF2 0%, #ECEDEF 38%, #ECEDF1 70%, #EDEEF3 100%);
  background-size: cover;
  background-repeat: no-repeat;
}

.header {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  height: 65px;
  padding: 0 0;
  background-color: transparent;
  border-color: transparent;
  border-radius: 20px;
}

.left-menu {
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom-color: transparent;
}

.stage-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.title-icon {
  font-size: large;
}

.title {
  font-weight: 800;
  font-family: Arial, Helvetica, sans-serif;
  letter-spacing: 2px;
}

.right-menu {
  flex: 1;
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 30px;
  position: relative;
  height: 100%;
  border-bottom-left-radius: 30px;
  border-top-right-radius: 20px;
  border-top-left-radius: 4px;
}

.back-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 113%;
  background-image: url('../../assets/header-back.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  z-index: 1;
  border-bottom-left-radius: 30px;
  border-top-right-radius: 20px;
  border-top-left-radius: 4px;
}

.menu-front {
  width: 100%;
  height: 113%;
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  justify-content: space-between;
  border-bottom-left-radius: 30px;
  border-top-right-radius: 20px;
  border-top-left-radius: 4px;
  background-color: transparent;
  backdrop-filter: blur(10px);
  align-items: center;
  z-index: 2;
  padding-left: 20px;
}

.logo {
  font-size: 22px;
  font-weight: bold;
  color: white;
}

.menu-group {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  align-items: center;
  gap: 20px;
  margin-right: 10px;
}

.menu-item {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.menu-icon {
  cursor: pointer;
  font-size: 30px;
  background-image: linear-gradient(45deg, #E6E6ED 0%, #C6C3DF 30%, #B5C4E1 70%, #B5BDDF 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.avatar {
  padding: 2px;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  background-color: #FCFEFE;
  z-index: 2;
}

.main-content {
  display: flex;
  flex: 1;
  padding: 20px;
  gap: 20px;
  height: calc(100% - 85px);
}

.left-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.05);
}

.left-panel .stage-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.direction-select-component {
  flex: 1;
  height: 0; /* 让DirectionSelect组件填充剩余空间 */
}

.right-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-component {
  flex: 1;
  background-color: white;
  border-radius: 15px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.05);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, max-height 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  max-height: 0;
}

.fade-enter-to,
.fade-leave-from {
  opacity: 1;
  max-height: 200px;
}
</style>