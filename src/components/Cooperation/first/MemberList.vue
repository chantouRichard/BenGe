<template>
  <div class="member-list-container">
    <div class="member-area">
      <div class="member-header">
        <h4>成员列表</h4>
        <button @click="toggleMemberListVisibility" class="toggle-btn">
          <i :class="isMemberOpen ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
        </button>
      </div>

      <transition name="collapse">
        <div v-if="isMemberOpen" class="member-list">
          <div
              class="member-item"
              v-for="(member, index) in socketState.members"
              :key="index"
              @click="$emit('clickMember', { member, index })"
          >
            <img :src="member.avatar" alt="avatar" class="member-avatar" />
            <span class="member-name">{{ member.username }}</span>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { socketState } from '@/stores/socket';

const props = defineProps({
  members: {
    type: Array,
    required: true,
  },
});

// 响应式变量
const isMemberOpen = ref(true);

// 切换成员列表显示
const toggleMemberListVisibility = () => {
  isMemberOpen.value = !isMemberOpen.value;
  console.log("成员列表：",socketState.members);
};
</script>

<style scoped>
.member-list-container {
  width: 100%;
  margin-bottom: 20px;
}

.member-area {
  background-color: white;
  border-radius: 15px;
  padding: 15px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.05);
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.member-header h4 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.toggle-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  padding: 5px;
}

.member-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 15px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 5px;
}

.member-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #f0f0f0;
}

.member-name {
  font-size: 12px;
  color: #666;
  text-align: center;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 滚动条样式 */
.member-list::-webkit-scrollbar {
  width: 4px;
}

.member-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.member-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.member-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 折叠/展开动画 */
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  max-height: 200px;
  opacity: 1;
}
</style>