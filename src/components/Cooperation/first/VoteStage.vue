<template>
  <transition
    name="fade"
    @before-enter="beforeEnter"
    @before-leave="beforeLeave"
  >
    <div v-if="AIGenerate" class="ai-loading-container">
      <video
        class="loading-video"
        autoplay
        muted
        loop
        playsinline
        src="@/assets/first/book.mp4"
      ></video>
      <div class="loading-text">稍等，AI正在整合...</div>
    </div>
  </transition>
  <div class="vote-stage">
    <h1 class="title">Script Direction Voting</h1>
    <div class="vote-info">
      <p>请从以下方向中选择1-2个进行投票 ({{ votesLeft }}票剩余)</p>
    </div>

    <div class="vote-options">
      <div
        v-for="(option, index) in sortedOptions"
        :key="index"
        class="vote-option"
        :class="{
          selected: selectedOptions.includes(option.direction),
          leading: index < 3 && hasVotes,
        }"
        @click="
          toggleVote(option.direction, topDirections.indexOf(option.direction))
        "
      >
        <div class="option-content">
          <span class="direction">{{ option.direction.title }}</span>
          <span style="font-size: 12px; font-weight: bold">{{
            option.direction.description
          }}</span>
          <span class="vote-count">{{ option.votes }}票</span>
        </div>
        <div class="voters">
          <span v-for="voter in option.voters" :key="voter" class="voter">
            {{ voter }}
          </span>
        </div>
        <div v-if="index < 3 && hasVotes" class="top-badge">
          TOP {{ index + 1 }}
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <button class="confirm-btn" :disabled="!canConfirm" @click="confirmVote">
        {{ hasVoted ? "等待其他成员..." : "确认投票" }}
      </button>
    </div>

    <div v-if="showAISuggestion" class="ai-suggestion-area">
      <h3>AI创作建议</h3>
      <div class="suggestion-content">
        {{ aiSuggestion }}
      </div>
      <div class="modification-area">
        <textarea
          v-model="modificationRequest"
          placeholder="输入您的修改意见..."
          :disabled="isRegenerating"
        ></textarea>
        <button
          @click="requestRegeneration"
          :disabled="isRegenerating || !modificationRequest.trim()"
        >
          {{ isRegenerating ? "生成中..." : "重新生成" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { socketState } from "@/stores/socket";
import { ref, computed, watch, defineProps, defineEmits } from "vue";
const AIGenerate = ref(true);

const props = defineProps({
  roomId: {
    type: String,
    required: true,
  },
  members: {
    type: Array,
    default: () => [],
  },
  allDirections: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["submitVote", "regenerateSuggestion", "next-stage"]);

const voteOptions = computed(() =>
  topDirections.value.map((direction, index) => {
    const totalVotes = socketState.members.reduce((sum, member) => {
      if (
        Array.isArray(member.vote) &&
        typeof member.vote[index] === "number"
      ) {
        return sum + member.vote[index];
      }
      return sum;
    }, 0);

    // 可选扩展 voters 字段：谁投了该方向
    const voters = socketState.members
      .filter((member) => Array.isArray(member.vote) && member.vote[index] > 0)
      .map((member) => member.username || "匿名");

    return {
      direction,
      votes: totalVotes,
      voters,
    };
  })
);

const selectedOptions = ref([]);
const hasVoted = ref(false);
const totalMembers = ref(props.members.length + 1);
const aiSuggestion = ref("");
const modificationRequest = ref("");
const isRegenerating = ref(false);
const showAISuggestion = ref(false);
const allVotesReceived = ref(false);

const topDirections = ref([]);

import { AIIntegrateDirection } from "@/api/room";
// 初始化投票选项
const initializeOptions = async () => {
  const allKeys = socketState.members.flatMap((member) => member.key || []);
  const data = {
    keyWords:allKeys,
    roomId:socketState.roomId
  }
  console.log("所有Keys：", allKeys);

  try {
    await AIIntegrateDirection(data);
  } catch (error) {
    console.log("请求出错：", error);
  }

  // 如果不是房主，监听 options 的变化
  watch(
    () => socketState.options,
    (newOptions) => {
      if (newOptions) {
        console.log("打印socketState：",socketState.options);
        console.log("打印new：",newOptions);
        topDirections.value = newOptions;
        AIGenerate.value = false;
      }
    },
    { immediate: true } // 确保初始化时就检查一次
  );
};

initializeOptions();

// 计算属性
const votesLeft = computed(() => 2 - selectedOptions.value.length);
const canConfirm = computed(
  () => selectedOptions.value.length > 0 && !hasVoted.value
);
const sortedOptions = computed(() => {
  return [...voteOptions.value].sort((a, b) => b.votes - a.votes);
});
const hasVotes = computed(() => voteOptions.value.some((opt) => opt.votes > 0));
const vote = ref([0, 0, 0, 0, 0, 0]);

// 切换投票选择
const toggleVote = (direction, voteIndex) => {
  if (voteIndex < 0 || voteIndex >= vote.value.length) return;
  if (hasVoted.value) return;

  const index = selectedOptions.value.indexOf(direction);
  if (index === -1) {
    if (selectedOptions.value.length < 2) {
      selectedOptions.value.push(direction);
      vote.value[voteIndex] = 1;
    }
  } else {
    selectedOptions.value.splice(index, 1);
    vote.value[voteIndex] = 0;
  }

  console.log("投票数组:", vote.value);
  socketState.socket.send(JSON.stringify({ type: "vote", vote: vote.value }));
};

// 确认投票
const confirmVote = () => {
  if (selectedOptions.value.length === 0) return;

  hasVoted.value = true;
  console.log("有没有投票好：", hasVoted.value);
  socketState.socket.send(
    JSON.stringify({ type: "vote", hasVoted: hasVoted.value })
  );
};

// 处理收到的投票更新
const handleVoteUpdate = (data) => {
  // 更新投票数据
  voteOptions.value = data.options;
  allVotesReceived.value = data.allVoted;

  // 如果所有人都投票了，显示AI建议
  if (allVotesReceived.value) {
    showAISuggestion.value = true;
    // 这里应该从后端获取AI建议
    aiSuggestion.value = "这是基于投票结果生成的AI创作建议...";
  }
};

import { userLoadingStore } from "@/stores/userLoadingStore";

const loadingStore = userLoadingStore();
// 判断成员是否全部选择了
const allMembersVoted = computed(
  () =>
    socketState.members.length > 0 &&
    socketState.members.every((member) => member.hasVoted === true)
);
watch(allMembersVoted, (newVal) => {
  if (newVal) {
    // showAISuggestion.value = true;
    // 这里应该从后端获取AI建议
    // aiSuggestion.value = "这是基于投票结果生成的AI创作建议...";
    socketState.direction = sortedOptions.value[0].direction;
    console.log("最终方向：", sortedOptions.value);

    loadingStore.show2();

    setTimeout(() => {
      emit("next-stage", 1);
      setTimeout(() => {
        loadingStore.hide2();
      });
    }, 10000);
  }
});

// 请求重新生成AI建议
const requestRegeneration = () => {
  if (isRegenerating.value) return;

  isRegenerating.value = true;
  // 发送重新生成请求到后端
  emit("regenerateSuggestion", {
    roomId: props.roomId,
    modification: modificationRequest.value,
  });

  // 此处websocket监听更新
};

// 监听成员变化
watch(
  () => props.members,
  (newMembers) => {
    totalMembers.value = newMembers.length + 1;
  }
);

// 监听投票选项变化
watch(
  () => props.allDirections,
  () => {
    initializeOptions();
  }
);

watch(
  voteOptions,
  (newVal) => {
    console.log("voteOptions 更新了:", newVal);
    console.log("socketState.members:", socketState.members);
  },
  { deep: true, immediate: true }
);
</script>

<style scoped>
.vote-stage {

  top: 100px;
  padding: 20px;
  font-family: "Arial", sans-serif;
  display: flex;
  flex-direction: column;
  height: 80%;
  width: 90%;

  align-items: center;
  justify-content: center;

  position: absolute;
}

.title {
  text-align: left;
  color: #333;
  margin-bottom: 20px;
  width: 90%;

  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    Oxygen, Ubuntu, Cantarell, "Open Sans", "Helvetica Neue", sans-serif;
}

.vote-info {
  text-align: center;
  margin-bottom: 20px;
  color: #666;

  width: 90%;
  margin-left: auto;
  margin-right: auto;

  display: flex;
}

.vote-info p {
  width: 100%;
  margin: 5px 0;
}

.vote-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
  flex: 1;

  width: 100%;
  padding: 10px;
  height: 100%;
  min-height: 360px;

  overflow-y: auto;

}

.vote-option {
  padding: 15px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  border: 2px solid transparent;
  overflow: hidden;
  width: 200px;
  height: 160px;

  overflow-y: auto;
}

.vote-option:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.vote-option.selected {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.05);
}

.vote-option.leading {
  border-left: 4px solid #67c23a;
}

.option-content {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  margin-bottom: 10px;

  position: relative;

  overflow-y: auto;
}

.direction {
  font-weight: bold;
  font-size: 16px;

  text-align: left;
}

.vote-count {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #f0f0f0;
  padding: 3px 8px;
  border-radius: 10px;
  font-size: 14px;
}

.voters {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  font-size: 12px;
  color: #666;
}

.voter {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 10px;
}

.top-badge {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #67c23a;
  color: white;
  padding: 2px 8px;
  font-size: 12px;
  border-bottom-left-radius: 5px;
}

.action-buttons {
  text-align: center;
  margin-top: 20px;
}

.confirm-btn {
  padding: 12px 30px;
  background-color: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #85ce61;
}

.confirm-btn:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.ai-suggestion-area {
  margin-top: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #eee;
}

.ai-suggestion-area h3 {
  margin-top: 0;
  color: #333;
  border-bottom: 1px solid #ddd;
  padding-bottom: 10px;
}

.suggestion-content {
  margin: 15px 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.modification-area {
  margin-top: 20px;
}

.modification-area textarea {
  width: 100%;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
  resize: vertical;
}

.modification-area button {
  padding: 8px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.modification-area button:hover:not(:disabled) {
  background-color: #66b1ff;
}

.modification-area button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.loading-video {
  width: 200px; /* 你可以改成 100% 或 cover 效果 */
  height: 200px;
  object-fit: contain;
}
.ai-loading-container {
  position: absolute;

  background-color: #fcf9fc;
  width: 97%;
  height: 94%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  border-radius: 24px;

  z-index: 9000;
}
.loading-text {
  font-size: 36px;
  font-weight: bold;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.6s ease;
}

.fade-enter, .fade-leave-to /* .fade-leave-active in <2.1.8 */ {
  opacity: 0;
}
</style>
