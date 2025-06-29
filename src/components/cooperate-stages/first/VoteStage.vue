<template>
  <div class="vote-stage">
    <h2 class="stage-title">创作建议确认</h2>

    <div class="suggestion-area">
      <h3>AI生成的创作建议</h3>
      <div class="suggestion-content">
        {{ aiSuggestion || '正在生成建议...' }}
      </div>

      <div class="feedback-area">
        <textarea
            v-model="userFeedback"
            placeholder="对创作建议提出修改意见..."
            :disabled="isGenerating"
        ></textarea>
        <button
            class="regenerate-btn"
            @click="handleRegenerate"
            :disabled="isGenerating || !aiSuggestion"
        >
          {{ isGenerating ? '生成中...' : '重新生成' }}
        </button>
      </div>
    </div>

    <button
        class="accept-btn"
        @click="handleAccept"
        :disabled="!aiSuggestion"
    >
      确认采纳
    </button>
  </div>
</template>

<script setup>
import { ref,defineProps, defineEmits } from 'vue';

/* eslint-disable-next-line no-unused-vars */
const props = defineProps({
  aiSuggestion: String,
  isGenerating: Boolean
});

const emit = defineEmits(['regenerate', 'accept']);

const userFeedback = ref('');

const handleRegenerate = () => {
  emit('regenerate', userFeedback.value);
  userFeedback.value = '';
};

const handleAccept = () => {
  emit('accept');
};
</script>

<style scoped>
.vote-stage {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
}

.stage-title {
  margin-bottom: 20px;
  color: #333;
}

.suggestion-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}

.suggestion-content {
  flex: 1;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 15px;
  border: 1px solid #eee;
  white-space: pre-wrap;
}

.feedback-area {
  display: flex;
  gap: 10px;
}

.feedback-area textarea {
  flex: 1;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  resize: none;
}

.regenerate-btn {
  padding: 0 20px;
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.regenerate-btn:hover:not(:disabled) {
  background-color: #4F46E5;
}

.regenerate-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.accept-btn {
  padding: 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.accept-btn:hover:not(:disabled) {
  background-color: #3e8e41;
}

.accept-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style>