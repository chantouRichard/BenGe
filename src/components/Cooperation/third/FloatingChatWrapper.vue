<template>
    <div
      class="floating-chat"
      :class="{ expanded: isExpanded }"
      :style="{ top: position.top + 'px', left: position.left + 'px' }"
      @mousedown.stop="startDrag"
    >
      <!-- 顶部栏 -->
      <div class="chat-header-bar">
        <div class="user-info">
          <img :src="avatar" class="user-avatar" />
          <span class="user-name">{{ userName }}</span>
        </div>
        <i class="fa-solid fa-chevron-down toggle-button" @click.stop="toggleExpand" />
      </div>
  
      <!-- 嵌套 Chat 组件 -->
      <div class="chat-inner-wrapper">
        <Chat
          :roomId="roomId"
          :userId="userId"
          :userName="userName"
          :avatar="avatar"
          :initialMessages="initialMessages"
          @membersUpdated="$emit('membersUpdated', $event)"
        />
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive } from "vue";
  import Chat from "../Chat.vue";
  import loginImage from "@/assets/login.png";
  
  defineProps({
    roomId: Number,
    userId: String,
    userName: String,
    avatar: {
      type: String,
      default: loginImage
    },
    initialMessages: {
      type: Array,
      default: () => []
    }
  });
  defineEmits(['membersUpdated']);
  
  const isExpanded = ref(true);
  
  const position = reactive({ top: 100, left: 100 });
  let dragging = false, offsetX = 0, offsetY = 0;
  
  const startDrag = (e) => {
    dragging = true;
    offsetX = e.clientX - position.left;
    offsetY = e.clientY - position.top;
    document.addEventListener("mousemove", onDrag);
    document.addEventListener("mouseup", stopDrag);
  };
  
  const onDrag = (e) => {
    if (!dragging) return;
    position.left = e.clientX - offsetX;
    position.top = e.clientY - offsetY;
  };
  
  const stopDrag = () => {
    dragging = false;
    document.removeEventListener("mousemove", onDrag);
    document.removeEventListener("mouseup", stopDrag);
  };
  
  const toggleExpand = () => {
    isExpanded.value = !isExpanded.value;
  };
  </script>
  
  <style scoped>
  .floating-chat {
    position: fixed;
    width: 320px;
    height: 400px;
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    z-index: 999;
  }
  .floating-chat.expanded {
    height: 520px;
  }
  .chat-header-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background-color: #cbe7f5;
    cursor: move;
  }
  .user-info {
    display: flex;
    align-items: center;
  }
  .user-avatar {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    margin-right: 6px;
  }
  .user-name {
    font-size: 14px;
    font-weight: bold;
  }
  .toggle-button {
    cursor: pointer;
    font-size: 14px;
    color: #333;
  }
  .chat-inner-wrapper {
    flex: 1;
    height: 100%;
    display: flex;
  }
  </style>
  