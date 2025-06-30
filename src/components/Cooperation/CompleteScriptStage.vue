<template>
  <div class="container">
    <div class="header">
      <div class="logo">BenGe Vision</div>
      <div class="right-menu">
        <div class="menu-item">我的剧本</div>
        <div class="menu-item">帮助</div>
        <img src="../../assets/login.png" alt="avatar" class="avatar" />
      </div>
    </div>
    <h2 style="margin: 20px 50px 10px; color: white">第三阶段：共同编辑</h2>
    <div class="main-area">
      <div class="edit-area">
        <div style="display: flex; margin: 0px 20px 20px; align-items: center">
          <h2>共同编辑区</h2>
          <div style="margin-left: 20px; display: flex; align-items: center; gap: 10px">
            <el-tag
              :type="isConnected ? 'success' : 'danger'"
              size="small"
              style="font-size: 12px"
            >
              {{ connectionStatus }}
            </el-tag>
            <span style="font-size: 12px; color: #666">
              房间ID: {{ roomId }}
            </span>
          </div>
          <div style="margin-left: auto; display: flex; gap: 10px">
            <el-button class="button" @click="saveToServer" :disabled="!isConnected">
              <img
                src="../../assets/save.png"
                style="width: 16px; height: 16px; margin-right: 2px"
              />保存剧本</el-button
            >
            <el-button class="button" @click="output">
              <img
                src="../../assets/output.png"
                style="width: 16px; height: 16px; margin-right: 2px"
              />导出剧本</el-button
            >
          </div>
        </div>
        <div
          id="editor"
          style="height: calc(100% - 100px); background: #fff"
        ></div>
      </div>
      <div class="right-area">
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
                <span class="member-name">{{ member.name }}</span>
              </div>
              <div v-if="members.length === 0" class="no-members">
                暂无其他成员在线
              </div>
            </div>
          </transition>
        </div>

        <!-- 聊天区 -->
        <div
          class="chat-area"
          :style="{ height: isMemberOpen ? '80%' : '95%' }"
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
            <Chat
              :roomId="roomId"
              :userId="currentUser.id"
              :userName="currentUser.name"
              @membersUpdated="updateMembers"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import Quill from "quill";
import QuillCursors from "quill-cursors";
import "quill/dist/quill.snow.css";
import * as Y from "yjs";
import { QuillBinding } from "y-quill";
import { WebsocketProvider } from "y-websocket";
import { marked } from "marked";
import TurndownService from "turndown";
import axios from "axios";
import Chat from "./Chat.vue";
import loginImage from "../../assets/login.png";

const route = useRoute();
const isMemberOpen = ref(true);
const isConnected = ref(false);
const connectionStatus = ref('连接中...');

const toggleMemberArea = () => {
  isMemberOpen.value = !isMemberOpen.value;
}

// 注册光标模块
Quill.register("modules/cursors", QuillCursors);

// 从路由参数获取房间ID，如果没有则使用默认值1
const roomId = computed(() => {
  return parseInt(route.params.roomId) || 1;
});

// 从localStorage获取用户信息
const currentUser = computed(() => {
  const username = localStorage.getItem('username') || '匿名用户';
  const userId = `user_${username}_${Date.now()}`;
  return {
    id: userId,
    name: username,
    color: getRandomColor(),
    avatar: loginImage
  };
});

const members = ref([]);

let quill, ydoc, provider, cursorsModule;

onMounted(async () => {
  try {
    connectionStatus.value = '初始化编辑器...';

    // 初始化 Yjs 文档
    ydoc = new Y.Doc();
    const ytext = ydoc.getText("quill");

    // 初始化 Quill 编辑器
    quill = new Quill("#editor", {
      theme: "snow",
      modules: {
        cursors: {
          transformOnTextChange: true,
        },
        toolbar: [
          [{ 'header': [1, 2, 3, false] }],
          ['bold', 'italic', 'underline', 'strike'],
          [{ 'list': 'ordered'}, { 'list': 'bullet' }],
          [{ 'indent': '-1'}, { 'indent': '+1' }],
          ['link', 'blockquote', 'code-block'],
          ['clean']
        ],
      },
    });

    // 获取 cursors 模块实例
    cursorsModule = quill.getModule("cursors");

    connectionStatus.value = '连接协作服务器...';

    // 初始化 WebSocket Provider - 使用动态房间ID
    provider = new WebsocketProvider("ws://localhost:1234", `room-${roomId.value}`, ydoc);

    // 设置本地用户信息
    provider.awareness.setLocalStateField("user", {
      id: currentUser.value.id,
      name: currentUser.value.name,
      color: currentUser.value.color,
      avatar: currentUser.value.avatar,
    });

    // 绑定 Yjs 与 Quill
    new QuillBinding(ytext, quill, provider.awareness);

    // 监听连接状态
    provider.on('status', (event) => {
      console.log('Y.js连接状态:', event.status);
      if (event.status === 'connected') {
        isConnected.value = true;
        connectionStatus.value = '已连接';
        ElMessage.success('协作服务器连接成功');
      } else if (event.status === 'disconnected') {
        isConnected.value = false;
        connectionStatus.value = '连接断开';
        ElMessage.warning('协作服务器连接断开');
      }
    });

    // 监听远程用户状态更新（用于光标显示）
    provider.awareness.on("update", () => {
      const states = Array.from(provider.awareness.getStates().entries());
      cursorsModule.clearCursors();

      for (const [clientID, state] of states) {
        if (clientID === provider.awareness.clientID) continue;
        const user = state.user;
        const selection = state.selection;
        if (user && selection) {
          cursorsModule.createCursor(clientID.toString(), user.name, user.color);
          cursorsModule.moveCursor(clientID.toString(), selection);
        }
      }
    });

    // 本地用户光标变化广播
    quill.on("selection-change", (range) => {
      provider.awareness.setLocalStateField("selection", range ?? null);
    });

    // 首次加载内容
    provider.once("synced", async () => {
      connectionStatus.value = '同步完成';
      if (ytext.length === 0) {
        try {
          // 尝试从后端加载剧本内容
          const res = await axios.get(`/api/script/room/${roomId.value}`);
          const content = res.data?.content || `# 房间 ${roomId.value} 的剧本\n\n开始你们的创作吧！`;
          const html = marked.parse(content);
          quill.setContents(quill.clipboard.convert(html), "api");
        } catch (err) {
          console.error("加载初始内容失败", err);
          // 使用默认内容
          const defaultContent = `# 房间 ${roomId.value} 的剧本\n\n开始你们的创作吧！`;
          const html = marked.parse(defaultContent);
          quill.setContents(quill.clipboard.convert(html), "api");
        }
      }
    });

  } catch (error) {
    console.error('初始化编辑器失败:', error);
    ElMessage.error('初始化编辑器失败，请刷新页面重试');
    connectionStatus.value = '连接失败';
  }
});

onBeforeUnmount(() => {
  provider?.destroy();
  ydoc?.destroy();
});

// 保存剧本到服务器
async function saveToServer() {
  try {
    if (!quill) {
      ElMessage.error('编辑器未初始化');
      return;
    }

    const turndownService = new TurndownService({
      headingStyle: "atx",
      bulletListMarker: "-",
      codeBlockStyle: "fenced",
    });

    // 添加规则处理 quill 中可能的 <div> 或 <span>
    turndownService.addRule("customBlock", {
      filter: ["div", "span"],
      replacement: function (content) {
        return content;
      },
    });

    const html = quill.root.innerHTML;
    const markdown = turndownService.turndown(html);

    // TODO: 需要后端实现 - 保存房间剧本内容的接口
    // POST /api/script/room/save
    // 参数: { roomId, content, title }
    try {
      const response = await axios.post('/api/script/room/save', {
        roomId: roomId.value,
        content: markdown,
        title: `房间${roomId.value}的剧本`
      });

      if (response.data === 'success' || response.status === 200) {
        ElMessage.success('剧本保存成功！');
        console.log("✅ 保存成功，内容：\n", markdown);
      } else {
        ElMessage.error('保存失败：' + (response.data || '未知错误'));
      }
    } catch (apiError) {
      // 如果后端接口不存在，先在本地保存
      console.warn('后端保存接口未实现，使用本地保存:', apiError.message);
      ElMessage.warning('后端接口未实现，内容已保存到控制台');
      console.log("✅ 本地保存内容：\n", markdown);

      // 可以保存到 localStorage 作为临时方案
      localStorage.setItem(`room_${roomId.value}_script`, markdown);
      ElMessage.success('剧本已临时保存到本地');
    }
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error('保存失败：' + error.message);
  }
}

// 随机颜色
function getRandomColor() {
  const colors = ["#f44336", "#2196f3", "#4caf50", "#ff9800", "#9c27b0"];
  return colors[Math.floor(Math.random() * colors.length)];
}

// 导出剧本
function output() {
  try {
    if (!quill) {
      ElMessage.error('编辑器未初始化');
      return;
    }

    const turndownService = new TurndownService({
      headingStyle: "atx",
      bulletListMarker: "-",
      codeBlockStyle: "fenced",
    });

    // 添加规则处理 quill 中可能的 <div> 或 <span>
    turndownService.addRule("customBlock", {
      filter: ["div", "span"],
      replacement: function (content) {
        return content;
      },
    });

    const html = quill.root.innerHTML;
    const markdown = turndownService.turndown(html);

    // 创建下载链接
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `房间${roomId.value}_剧本_${new Date().toISOString().slice(0, 10)}.md`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    ElMessage.success('剧本导出成功！');
    console.log("✅ 导出 Markdown：\n", markdown);
  } catch (error) {
    console.error('导出失败:', error);
    ElMessage.error('导出失败：' + error.message);
  }
}

function updateMembers(membersList) {
  // 直接更新整个成员列表
  members.value = membersList.map(member => ({
    id: member.id,
    name: member.username,
    avatar: member.avatar || loginImage
  }));
  console.log("成员列表已更新:", members.value);
}
</script>

<style scoped>
.member-header {
    display: flex;
    height: 40px;

    align-items: center;
}
.toggle-btn{
    height: 30px;
    width: 30px;

    border: none;
    background-color: white;
}
.member-area {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  background-color: white;
  border-radius: 8px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;

  /* ❌ 移除固定高度 */
  /* height: 20%; */
}


.member-list {
  display: grid;
  grid-template-columns: repeat(
    auto-fit,
    minmax(120px, 1fr)
  ); /* 自动换行、每列最小120px */
  margin-top: 10px;
  justify-content: center; /* 整体居中 */
  width: 90%;
  max-width: 100%;
}

.member-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.member-item:hover {
  background-color: #f5f5f5;
}

.member-avatar-container {
  position: relative;
  margin-right: 8px;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #ccc;
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

.member-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.no-members {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 20px;
}

.container {
  width: 100%;
  height: 100vh; /* 使用视口高度，确保充满整个屏幕 */

  background-image: url("@/assets/thirdback.jpeg");
  background-size: cover;
  background-repeat: no-repeat;
}
.header {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 60px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.logo {
  font-size: 22px;
  font-weight: bold;
}

.right-menu {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 30px;
}

.menu-item {
  cursor: pointer;
  font-size: 16px;
  color: #333;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: gray;
}

.main-area {
  display: flex;
  padding: 32px;
  gap: 20px; /* 子区域间距 */
  width: calc(100% - 100px);
  min-width: 827px;
  margin: 50px;
  background-color: rgba(255, 255, 255, 0.1); /* 半透明白色背景 */
  backdrop-filter: blur(8px); /* 毛玻璃模糊效果 */
  height: calc(100% - 200px);
  min-height: 500px;
  box-sizing: border-box; /* 确保 padding 不超宽 */
  border-radius: 20px;

  /* 其他样式 */
  border: 1px solid rgba(255, 255, 255, 0.3); /* 轻微的边框，增加层次感 */
}

.edit-area {
  width: 66%;
  min-width: 442px;
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.right-area {
  gap: 20px;
  display: flex;
  flex-direction: column;
  width: 34%;
  min-width: 312px;

  height: 100%;
}
.chat-area {
  height: 85%;
  display: flex;
  flex-direction: column;

  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.button {
  height: 40px;
  background-color: #2712bb;
  color: white;
  border-radius: 10px;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
}

.button:hover {
  background-color: #1e0a9a;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(39, 18, 187, 0.3);
}

.button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease, max-height 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  max-height: 0;
}

.fade-enter-to, .fade-leave-from {
  opacity: 1;
  max-height: 200px;
}
</style>
