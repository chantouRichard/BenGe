<template>
  <div class="room-container">
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧工作区 -->
      <div class="left-panel">
        <h2 class="stage-title">{{ currentStage === 'direction' ? '第一阶段：方向确定' : '第二阶段：投票确认' }}</h2>

        <!-- 工作区 -->
        <div class="section" style="flex: 1; display: flex; flex-direction: column;min-height: 0">
          <h3 class="section-title">
            {{ currentStage === 'direction' ? '个人工作区' : '投票区' }}
          </h3>


          <div  class="direction-stage-container"
                style="flex:1;
                min-height: 0;
                overflow: hidden">
            <!--方向选择-->
            <DirectionSelect
                v-if="currentStage === 'direction'"
                :userInfo="currentUser"
                @submission="handleDirectionSubmission"
                @confirm-submit="handleConfirmSubmit"/>

            <VoteStage
                v-else-if="currentStage === 'vote'"
                :aiSuggestion="aiSuggestion"
                :isGenerating="isGenerating"
                @regenerate="handleRegenerate"
                @accept="handleAcceptSuggestion" />
          </div>
        </div>
      </div>

      <!-- 右侧进度区 -->
      <div class="right-panel">
        <!-- 成员区 -->
        <div class="member-area" :class="{ collapsed: !isMemberOpen }">
          <div class="member-header">
            <h2 style="min-width: 72px">在线成员 ({{ members.length }})</h2>
            <button class="toggle-btn" @click="toggleMemberArea">
              {{ isMemberOpen ? "▲" : "▼" }}
            </button>
          </div>
          <transition name="fade">
            <div v-show="isMemberOpen" class="member-list">
              <div
                  class="member-item"
                  v-for="member in members"
                  :key="member.id"
              >
                <div class="member-avatar-container">
                  <img :src="member.avatar" alt="avatar" class="member-avatar" />
                  <div class="online-indicator"></div>
                </div>
                <div class="member-info">
                  <span class="member-name">{{ member.name }}</span>
                  <div v-if="member.selections && member.selections.length > 0" class="member-directions">
                    <div class="direction-tag" v-for="(direction, idx) in member.selections" :key="idx">
                      {{ direction.content }}
                    </div>
                  </div>
                  <div v-else class="no-directions">
                    尚未选择方向
                  </div>
                </div>
              </div>
              <div v-if="members.length === 0" class="no-members">
                暂无其他成员在线
              </div>
            </div>
          </transition>
        </div>

        <!-- 聊天区域 -->
        <div
            class="chat-area"
            :style="{height: isMemberOpen ? '80%' : '95%' }"
        >
          <div>
            <h2>聊天区</h2>
          </div>
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
            <Chat :userId="currentUser.id" @membersUpdated="updateMembers" :avatar="currentUser.avatar" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Chat from './Chat.vue';
import DirectionSelect from "@/components/cooperate-stages/first/DirectionSelect.vue";
import VoteStage from "@/components/cooperate-stages/first/VoteStage.vue";

// 当前阶段状态
const currentStage = ref('direction');

// 当前用户信息
const currentUser = ref({
  id: "user_" + Math.floor(Math.random() * 1000),
  name: localStorage.getItem('username') || '用户' + Math.floor(Math.random() * 1000),
  avatar: require(`@/assets/avatar/${Math.floor(Math.random() * 5 + 1)}.jpg`)
});

// AI生成的建议
const aiSuggestion = ref('');
const isGenerating = ref(false);

// 成员区状态
const isMemberOpen = ref(true);
const members = ref([]);

// 存储各成员的选择
const memberSelections = ref({});

// 最终选择的方向
const selectedDirections = ref([]);

// 处理方向提交
const handleDirectionSubmission = (info) => {
  memberSelections.value[currentUser.value.id] = info.directions;
  updateMemberList();
};

// 处理确认提交，切换到投票阶段
const handleConfirmSubmit = async (data) => {
  selectedDirections.value = data.directions;

  // 模拟生成AI建议
  isGenerating.value = true;
  aiSuggestion.value = '';

  // 这里应该是调用API生成建议
  setTimeout(() => {
    aiSuggestion.value = `基于您选择的方向，建议创作一个关于：
${data.directions.map(d => `- ${d.content}`).join('\n')}

故事梗概：
主角在${data.directions[0].content}背景下，面临${data.directions[1].content}的挑战，最终通过${data.directions[2].content}解决问题。`;
    isGenerating.value = false;
  }, 1500);

  currentStage.value = 'vote';
  // WebSocket
  // socket.emit('start-vote-stage', {
  //   directions: data.directions,
  //   userId: currentUser.value.id
  // });
};

// 处理重新生成建议
const handleRegenerate = (feedback) => {
  isGenerating.value = true;
  aiSuggestion.value = '';

  // 模拟根据反馈重新生成
  setTimeout(() => {
    aiSuggestion.value = `根据您的反馈"${feedback}"，重新生成的建议：
${selectedDirections.value.map(d => `- ${d.content}`).join('\n')}

调整后的故事：
加入了${feedback || '更多细节'}，使情节更加丰富。`;
    isGenerating.value = false;
  }, 1500);
};

// 处理接受建议
const handleAcceptSuggestion = () => {
  console.log('接受建议:', aiSuggestion.value);
  // 这里可以进入下一阶段或保存结果
  // emit('next-stage', { suggestion: aiSuggestion.value });
};

// 更新成员列表
const updateMemberList = () => {
  const currentMemberIndex = members.value.findIndex(m => m.id === currentUser.value.id);
  if (currentMemberIndex !== -1) {
    members.value[currentMemberIndex].selections = memberSelections.value[currentUser.value.id] || [];
  } else {
    members.value.push({
      id: currentUser.value.id,
      name: currentUser.value.name,
      avatar: currentUser.value.avatar,
      selections: memberSelections.value[currentUser.value.id] || []
    });
  }
};

// 更新成员列表(聊天组件)
function updateMembers(membersList) {
  // 合并新成员列表与现有选择信息
  members.value = membersList.map((member) => {
  // 确保 member.id 是字符串，避免调用 slice 报错
  const memberId = String(member.id);  // 强制转换为字符串

  // 保留原有的选择信息
  const existingSelections = memberSelections.value[memberId] || [];

  return {
    id: memberId,
    name: member.name || `用户${memberId.slice(5)}`, // 这里使用转换后的 memberId
    avatar: member.avatar || currentUser.value.avatar,
    selections: existingSelections
  };
});


  // 确保当前用户在成员列表中
  if (!members.value.some(m => m.id === currentUser.value.id)) {
    members.value.push({
      id: currentUser.value.id,
      name: currentUser.value.name,
      avatar: currentUser.value.avatar,
      selections: memberSelections.value[currentUser.value.id] || []
    });
  }

  console.log("成员信息已更新", members.value);
}

// 切换成员区显示
const toggleMemberArea = () => {
  isMemberOpen.value = !isMemberOpen.value;
};

// 初始化
onMounted(() => {
  // 初始化当前用户
  members.value.push({
    id: currentUser.value.id,
    name: currentUser.value.name,
    avatar: currentUser.value.avatar,
    selections: []
  });

  // 模拟WebSocket接收其他用户的选择信息
  // 通过WebSocket监听
  /*
  socket.on('receive-direction-selection', (data) => {
    memberSelections.value[data.userId] = data.selections;

    const memberIndex = members.value.findIndex(m => m.id === data.userId);
    if (memberIndex !== -1) {
      members.value[memberIndex].selections = data.selections;
    } else {
      members.value.push({
        id: data.userId,
        name: data.username,
        avatar: data.avatar,
        selections: data.selections
      });
    }
  });
  */
});
</script>

<style scoped>
/* 页面容器 */
.room-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-image: url("../../assets/loginback.jpg");
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
  color: #333;
  overflow: hidden;
}

.main-content {
  display: flex;
  height: calc(100vh - 70px);
  padding: 20px;
  gap: 20px;
  overflow: hidden;
  box-sizing: border-box;
  flex: 1;
}

.left-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  flex: 3;
  padding: 15px;
}

.right-panel {
  flex: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  gap: 20px;
}

.direction-stage-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
}

.chat-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
  overflow: hidden;
}

.stage-title {
  color: #000000;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 20px;
}

.section {
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.section-title {
  color: #050000;
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
}

/* 成员区样式 */
.member-area {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.member-header {
  display: flex;
  height: 40px;
  align-items: center;
}

.toggle-btn {
  height: 30px;
  width: 30px;
  border: none;
  background-color: white;
  cursor: pointer;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: flex-start;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.member-item:hover {
  background-color: #f5f5f5;
}

.member-avatar-container {
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;
}

.member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.online-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background-color: #4caf50;
  border: 2px solid #fff;
  border-radius: 50%;
}

.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.member-directions {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}

.direction-tag {
  background-color: #e1f5fe;
  color: #0288d1;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
}

.no-directions {
  font-size: 12px;
  color: #999;
  font-style: italic;
}

.no-members {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 20px;
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
  max-height: 300px;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>