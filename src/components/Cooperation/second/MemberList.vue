<template>
  <div v-if="isVisible" class="collapsible-member-list" :style="{ top: positionTop + 'px' }">
    <div class="member-area" :class="{ collapsed: !isMemberOpen }">
      <div class="member-header">
        <h4 style="min-width: 72px">成员区</h4>
        <button @click="toggleMemberListVisibility" class="toggle-btn">
          {{ isMemberOpen ? '收起' : '展开' }}
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
            <div class="member-info">
              <span class="member-name" >{{ member.username }}</span>
            </div>
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
  fontColor: {
    type: String,
    default: 'white',
  },
  members: {
    type: Array,
    required: true,
  },
  isMemberOpen: {
    type: Boolean,
    default: true,
  },
  isVisible: {
    type: Boolean,
    default: true,
  },
  positionTop: {
    type: Number,
    default: 50,
  },
});

// 响应式变量
const isMemberOpen = ref(props.isMemberOpen);

// 切换成员列表显示
const toggleMemberListVisibility = () => {
  isMemberOpen.value = !isMemberOpen.value;
  console.log("成员列表：",socketState.members);
};
</script>

<style scoped>
.collapsible-member-list {
  position: fixed;
  top: 50px;
  right: 20px;
  z-index: 9999;
  background:transparent;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  width: 300px;
}

.member-area {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: black;
  border-radius: 12px;
  background: transparent;
  /* 半透明白色背景 */
  backdrop-filter: blur(8px);
  /* 毛玻璃模糊效果 */
  box-sizing: border-box;
  /* 其他样式 */
  border: 1px solid rgba(0, 0, 0, 0.4); /* 轻微的边框，增加层次感 */
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toggle-btn {
  background: transparent;
  border: none;
  color: black;
  cursor: pointer;
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
  color: black;
  width: 54px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 折叠/展开动画 */
.collapse-enter-active,
.collapse-leave-active {
  transition: max-height 0.5s ease, opacity 0.5s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  max-height: 500px;
  opacity: 1;
}
</style>
