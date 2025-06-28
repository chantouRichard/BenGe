<template>
  <div class="chat-container">
    <!-- 顶部标题栏 -->
    <!-- <div class="chat-header">
      <button class="member-toggle" @click="toggleMemberList">
        <i class="toggle-icon">{{ isMemberListVisible ? "×" : "≡" }}</i>
      </button>
      <h2 class="room-name">{{ roomName }}</h2>
    </div> -->

    <!-- 成员列表侧边栏 -->
    <!-- <div
      class="member-list-overlay"
      :class="{ visible: isMemberListVisible }"
      @click="toggleMemberList"
    ></div>
    <div
      class="member-list"
      :class="{ 'member-list-visible': isMemberListVisible }"
    >
      <h3>群成员 ({{ members.length }})</h3>
      <ul>
        <li v-for="member in members" :key="member.id" class="member-item">
          <div class="avatar-placeholder"></div>
          <img :src="member.avatar" alt="头像" class="avatar" />
          <span>{{ member.name }}</span>
        </li>
      </ul>
    </div> -->
    <div
      style="
        width: 100px;
        margin-left: auto;
        margin-right: auto;
        font-size: 12px;
        border-color: black;
        border-width: 16px;
        border-radius: 5px;
      "
    >
      现在开始聊天吧~
    </div>
    <!-- 聊天内容区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div
        v-for="(message, index) in messages"
        :key="index"
        :class="[
          'message',
          message.isSystem
            ? 'message-system'
            : message.isMe
            ? 'message-me'
            : 'message-other',
        ]"
      >
        <div v-if="!message.isSystem" class="message-avatar">
          <div class="avatar-circle">
            <img
              :src="message.avatar"
              alt="avatar"
              style="width: 40px; height: 40px; border-radius: 20px"
            />
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
      <input
        type="text"
        v-model="newMessage"
        @keyup.enter="sendMessage"
        placeholder="输入消息..."
        class="message-input"
      />
      <img
        src="../../assets/send.png"
        @click="sendMessage"
        class="send-button"
      />
    </div>
  </div>
</template>

<script>
import loginImage from "@/assets/login.png";

export default {
  name: "ChatComponent",
  props: {
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
  },
  data() {
    return {
      isMemberListVisible: false,
      newMessage: "",
      messages: this.initialMessages,
      socket: null,
      currentUserId: null,
      currentUsername: null,
    };
  },
  methods: {
    toggleMemberList() {
      this.isMemberListVisible = !this.isMemberListVisible;
    },

    sendMessage() {
      if (this.newMessage.trim() === "") return;

      const messageData = {
        type: "chat",
        content: this.newMessage,
        time: this.getCurrentTime(),
        avatar: this.avatar,
      };

      // 通过 WebSocket 发送
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify(messageData));
      } else {
        console.error("WebSocket连接未就绪");
      }

      this.newMessage = "";
    },
    scrollToBottom() {
      this.$nextTick(() => {
        this.$refs.messagesContainer.scrollTop =
          this.$refs.messagesContainer.scrollHeight;
      });
    },
    getCurrentTime() {
      const now = new Date();
      return `${now.getHours()}:${now
        .getMinutes()
        .toString()
        .padStart(2, "0")}`;
    },
    setupWebSocket() {
      // 获取token
      const token = localStorage.getItem("token");
      if (!token) {
        console.error("未找到token，无法建立WebSocket连接");
        return;
      }

      this.socket = new WebSocket("ws://localhost:7122/ws");

      this.socket.onopen = () => {
        console.log("WebSocket连接已建立");
        // 连接建立后发送认证信息，使用传入的roomId
        this.socket.send(
          JSON.stringify({
            type: "auth",
            token: token,
            roomId: this.roomId,
            avatar: this.avatar,
          })
        );
      };

      this.socket.onmessage = (event) => {
        const msg = JSON.parse(event.data);

        if (msg.type === "userInfo") {
          this.currentUserId = msg.userId;
          this.currentUsername = msg.username;
          console.log(
            "接收到用户信息:",
            this.currentUserId,
            this.currentUsername
          );
        } else if (msg.type === "chat") {
          this.messages.push({
            ...msg,
            isMe: msg.userId === this.currentUserId,
            sender: msg.username,
            content: msg.content,
            time: msg.time,
            avatar: msg.avatar || this.avatar,
          });
          this.scrollToBottom();
        } else if (msg.type === "system") {
          this.messages.push({
            type: "system",
            content: msg.message,
            time: this.getCurrentTime(),
            isMe: false,
            isSystem: true,
          });
          this.scrollToBottom();
        } else if (msg.type === "members") {
          // 处理成员列表更新
          const incoming = msg.members || [];
          const existing = this.members || [];

          // 使用 Map 根据 id 去重（新来的会覆盖旧的）
          const mergedMap = new Map();

          // 先加已有成员
          existing.forEach((member) => {
            mergedMap.set(member.id, member);
          });

          // 再加新成员（会自动覆盖相同 id 的）
          incoming.forEach((member) => {
            mergedMap.set(member.id, member);
          });

          // 得到去重后的数组
          const mergedMembers = Array.from(mergedMap.values());

          this.$emit("membersUpdated", mergedMembers);
          console.log("合并后的成员列表:", mergedMembers);
        } else if (msg.type === "error") {
          console.error("WebSocket错误:", msg.message);
          alert("错误: " + msg.message);
        }
      };

      this.socket.onclose = () => {
        console.log("WebSocket连接已关闭");
      };

      this.socket.onerror = (err) => {
        console.error("WebSocket连接错误:", err);
      };
    },
  },
  watch: {
    initialMessages(newVal) {
      this.messages = newVal;
    },
  },
  mounted() {
    this.setupWebSocket();
  },
  beforeUnmount() {
    if (this.socket) {
      this.socket.close();
    }
  },
};
</script>

<style>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  /* max-height: 648px; */
  width: 100%;
  background-color: white;
  border-radius: 16px;
  overflow: hidden;
  /* box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); */
}

/* 头部样式 */
.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: whte;
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
  background-color: white;
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
  background-color: #ddd;
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
  color: #999;
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
  padding: 12px;
  background-color: #f0f0f0;
  border-top: 1px solid #ddd;
}

.message-input {
  height: 40px;
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
}

.send-button {
  width: 40px;
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;

  margin-left: 12px;
  padding: 4px 5px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  outline: none;
}

.send-button:hover {
  background-color: #00c3ff;

  transition: all 0.3s ease;
}
</style>
