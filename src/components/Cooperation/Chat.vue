<template>
  <div class="chat-container">
    <div style="
        width: 100px;
        margin-left: auto;
        margin-right: auto;
        font-size: 12px;
        border-color: black;
        border-width: 16px;
        border-radius: 5px;
      ">
      现在开始聊天吧~
    </div>
    <!-- 聊天内容区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(message, index) in socketState.messages" :key="index" :class="[
        'message',
        message.isSystem
          ? 'message-system'
          : message.isMe
            ? 'message-me'
            : 'message-other',
      ]">
        <div v-if="!message.isSystem" class="message-avatar">
          <div class="avatar-circle">
            <img :src="message.avatar" alt="avatar" style="width: 40px; height: 40px; border-radius: 20px" />
          </div>
        </div>
        <div class="message-content">
          <div v-if="!message.isSystem" class="message-sender">
            {{ message.sender }}
          </div>
          <div class="message-bubble">{{ message.content }}</div>
          <div class="message-time">{{ message.time }}</div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <input type="text" v-model="newMessage" @keyup.enter="sendChatMessage" placeholder="输入消息..."
        class="message-input" />
      <div class="send-button" @click="sendChatMessage">
        <i class="fa-solid fa-paper-plane"></i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue';
import loginImage from "@/assets/login.png";
import { socketState } from '@/stores/socket';
import { nextTick } from 'vue';
import { collectContextData } from '@/utils/contextCollector';
// 接收 props
const props = defineProps({
  roomId: {
    type: [Number, String],
    default: 1,
  },
  roomName: {
    type: String,
    default: "群聊房间",
  },
  members: {
    type: Array,
    default: () => [],
  },
  userId: {
    type: String,
    default: "anonymous",
  },
  userName: {
    type: String,
    default: "匿名用户",
  },
  avatar: {
    type: String,
    default: loginImage,
  },
  initialMessages: {
    type: Array,
    default: () => [],
  },
});

// 响应式变量
const isMemberListVisible = ref(false);
const newMessage = ref("");
const messages = ref(props.initialMessages);
const currentUserId = ref(null);
const currentUsername = ref(null);

// 切换成员列表显示
const toggleMemberList = () => {
  isMemberListVisible.value = !isMemberListVisible.value;
};

// 获取当前时间
const getCurrentTime = () => {
  const now = new Date();
  return `${now.getHours()}:${now.getMinutes().toString().padStart(2, "0")}`;
};

// 发送消息
const sendChatMessage = () => {
  if (newMessage.value.trim() === "") return;

  let messageContent=newMessage.value;

  if(newMessage.value.startsWith('@ai')){
    const ContextData=collectContextData();
    messageContent=`${newMessage.value}\n\n[CONTEXT_DATA]${JSON.stringify(ContextData)}[/CONTEXT_DATA]`;
  }

  const messageData = {
    type: "chat",
    content: messageContent,
    time: getCurrentTime(),
    avatar: props.avatar,
  };

  if (socketState.socket && socketState.socket.readyState === WebSocket.OPEN) {
    socketState.socket.send(JSON.stringify(messageData));
  } else {
    console.error("WebSocket连接未就绪");
  }

  newMessage.value = "";
};

// 滚动到最底部
const scrollToBottom = () => {
  nextTick(() => {
    // 确保 messagesContainer 已加载
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

// 监听 props 更新
watch(() => props.initialMessages, (newVal) => {
  messages.value = newVal;
});


// Refs
const messagesContainer = ref(null);
</script>


<style>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  /* max-height: 648px; */
  width: 100%;
  background-color: #ffffff2d;
  backdrop-filter: blur(3px);
  border-radius: 16px;
  overflow: hidden;
  /* box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); */
}

/* 头部样式 */
.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: black;
  position: relative;
  z-index: 10;
}

/* .member-toggle {
  background: none;
  border: none;
  color: black;
  font-size: 20px;
  cursor: pointer;
  margin-right: 12px;
  padding: 4px 8px;
}

.room-name {
  margin: 0;
  font-size: 18px;
  font-weight: normal;
} */

/* 成员列表样式 */

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  background-color: transparent;
}

.message {
  display: flex;
  margin-bottom: 8px;
}

.message-me {
  flex-direction: row-reverse;
  color: white;
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 4px;
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
}

.message-content {
  max-width: 70%;
}

.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  line-height: 1.4;
  word-break: break-word;
}

.message-me .message-bubble {
  background-color: #5d98f5;
  border-top-right-radius: 4px;
}

.message-other .message-bubble {
  background-color: #e9edf4;
  border-top-left-radius: 4px;
}

.message-time {
  font-size: 11px;
  color: hsl(0, 11%, 84%);
  margin-top: 4px;
}

.message-me .message-time {
  text-align: right;
}

/* 系统消息样式 */
.message-system {
  justify-content: center;
  margin: 8px 0;
}

.message-system .message-content {
  max-width: 80%;
  text-align: center;
}

.message-system .message-bubble {
  background-color: #f0f0f0;
  color: #666;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 12px;
}

/* 输入区域样式 */
.chat-input-area {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-top: 0.5px solid #eee;
}

.message-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #ccc;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
}

/* 外层按钮容器 - 圆形蓝底 */
.send-button {
  width: 36px;
  height: 36px;
  margin-left: 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

/* 图标本身 */
.send-button i {
  font-size: 16px;
  color: white;
  pointer-events: none; /* 避免事件冲突 */
}

.send-button:hover {
  background-color: #2563eb;
  transform: scale(1.05);
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);
}
</style>
