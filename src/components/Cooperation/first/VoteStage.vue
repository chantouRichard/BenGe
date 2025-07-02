<template>
  <div class="vote-stage">
    <h2 class="title">剧本方向投票</h2>
    <div class="vote-info">
      <p>请从以下方向中选择1-2个进行投票 ({{ votesLeft }}票剩余)</p>
      <p>当前参与投票成员: {{ members.length + 1 }}/{{ totalMembers }}</p>
    </div>

    <div class="vote-options">
      <div
          v-for="(option, index) in sortedOptions"
          :key="index"
          class="vote-option"
          :class="{
          'selected': selectedOptions.includes(option.direction),
          'leading': index < 3 && hasVotes
        }"
          @click="toggleVote(option.direction)"
      >
        <div class="option-content">
          <span class="direction">{{ option.direction }}</span>
          <span class="vote-count">{{ option.votes }}票</span>
        </div>
        <div class="voters">
          <span v-for="voter in option.voters" :key="voter" class="voter">
            {{ voter }}
          </span>
        </div>
        <div v-if="index < 3 && hasVotes" class="top-badge">TOP {{ index + 1 }}</div>
      </div>
    </div>

    <div class="action-buttons">
      <button
          class="confirm-btn"
          :disabled="!canConfirm"
          @click="confirmVote"
      >
        {{ hasVoted ? '等待其他成员...' : '确认投票' }}
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
          {{ isRegenerating ? '生成中...' : '重新生成' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, defineProps, defineEmits } from 'vue'

const props = defineProps({
  roomId: {
    type: String,
    required: true
  },
  members: {
    type: Array,
    default: () => []
  },
  allDirections: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['submitVote', 'regenerateSuggestion'])

const voteOptions = ref([])
const selectedOptions = ref([])
const hasVoted = ref(false)
const totalMembers = ref(props.members.length + 1)
const aiSuggestion = ref('')
const modificationRequest = ref('')
const isRegenerating = ref(false)
const showAISuggestion = ref(false)
const allVotesReceived = ref(false)

// 初始化投票选项
const initializeOptions = () => {
  // 统计所有方向出现的次数
  const directionCounts = {}
  props.allDirections.forEach(directions => {
    directions.forEach(dir => {
      directionCounts[dir] = (directionCounts[dir] || 0) + 1
    })
  })

  // 转换为投票选项格式
  voteOptions.value = Object.keys(directionCounts).map(direction => ({
    direction,
    votes: 0,
    voters: []
  }))
}

initializeOptions()

// 计算属性
const votesLeft = computed(() => 2 - selectedOptions.value.length)
const canConfirm = computed(() => selectedOptions.value.length > 0 && !hasVoted.value)
const sortedOptions = computed(() => {
  return [...voteOptions.value].sort((a, b) => b.votes - a.votes)
})
const hasVotes = computed(() => voteOptions.value.some(opt => opt.votes > 0))

// 切换投票选择
const toggleVote = (direction) => {
  if (hasVoted.value) return

  const index = selectedOptions.value.indexOf(direction)
  if (index === -1) {
    if (selectedOptions.value.length < 2) {
      selectedOptions.value.push(direction)
    }
  } else {
    selectedOptions.value.splice(index, 1)
  }
}

// 确认投票
const confirmVote = () => {
  if (selectedOptions.value.length === 0) return

  // 发送投票到后端
  emit('submitVote', {
    roomId: props.roomId,
    directions: selectedOptions.value
  })

  hasVoted.value = true
}

// 处理收到的投票更新
const handleVoteUpdate = (data) => {
  // 更新投票数据
  voteOptions.value = data.options
  allVotesReceived.value = data.allVoted

  // 如果所有人都投票了，显示AI建议
  if (allVotesReceived.value) {
    showAISuggestion.value = true
    // 这里应该从后端获取AI建议
    aiSuggestion.value = "这是基于投票结果生成的AI创作建议..."
  }
}

// 请求重新生成AI建议
const requestRegeneration = () => {
  if (isRegenerating.value) return

  isRegenerating.value = true
  // 发送重新生成请求到后端
  emit('regenerateSuggestion', {
    roomId: props.roomId,
    modification: modificationRequest.value
  })

  // 此处websocket监听更新
}

// 监听成员变化
watch(() => props.members, (newMembers) => {
  totalMembers.value = newMembers.length + 1
})

// 监听投票选项变化
watch(() => props.allDirections, () => {
  initializeOptions()
})
</script>

<style scoped>
.vote-stage {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Arial', sans-serif;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.title {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.vote-info {
  text-align: center;
  margin-bottom: 20px;
  color: #666;
}

.vote-info p {
  margin: 5px 0;
}

.vote-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
  flex: 1;
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.direction {
  font-weight: bold;
  font-size: 16px;
}

.vote-count {
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
</style>