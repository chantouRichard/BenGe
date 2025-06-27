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
        <div style="display: flex; margin: 0px 20px 20px">
          <h2>共同编辑区</h2>
          <div style="margin-left: auto">
            <el-button class="button" @click="saveToServer">
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
            <h2 style="min-width: 72px">成员区</h2>
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
                <img :src="member.avatar" alt="avatar" class="member-avatar" />
                <span class="member-name">{{ member.name }}</span>
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
            <Chat :userId="userId" @membersUpdated="updateMembers"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from "vue";
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
import { ref } from "vue";

const isMemberOpen = ref(true)

const toggleMemberArea = () => {
  isMemberOpen.value = !isMemberOpen.value
}
// 注册光标模块
Quill.register("modules/cursors", QuillCursors);

const userId = "user_" + Math.floor(Math.random() * 1000);
const userName = "用户_" + userId.slice(-3);
const userColor = getRandomColor();
const userAvatar = loginImage;

// 固定使用roomId为1进行测试
const roomId = 1;
const members = ref([]);

let quill, ydoc, provider, cursorsModule;

onMounted(async () => {
  // 初始化 Yjs 文档
  ydoc = new Y.Doc();
  const ytext = ydoc.getText("quill");

  // 初始化 Quill 编辑器
  quill = new Quill("#editor", {
    theme: "snow",
    modules: {
      cursors: true,
      toolbar: true,
    },
  });

  // 获取 cursors 模块实例
  cursorsModule = quill.getModule("cursors");

  // 初始化 WebSocket Provider
  provider = new WebsocketProvider("ws://localhost:1234", roomId, ydoc);

  // 设置本地用户信息
  provider.awareness.setLocalStateField("user", {
    id: userId,
    name: userName,
    color: userColor,
    avatar: userAvatar,
  });

  // 绑定 Yjs 与 Quill
  new QuillBinding(ytext, quill, provider.awareness);

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

  // 首次加载 Markdown 内容
  provider.once("synced", async () => {
    if (ytext.length === 0) {
      try {
        const res = await axios.get(`/api/collab/load?roomId=${roomId}`);
        const markdown = res.data?.content || "# 欢迎使用协同编辑";
        const html = marked.parse(markdown);
        quill.setContents(quill.clipboard.convert(html), "api");
      } catch (err) {
        console.error("加载初始内容失败", err);
      }
    }
  });
});

onBeforeUnmount(() => {
  provider?.destroy();
  ydoc?.destroy();
});

// 保存为 Markdown
async function saveToServer() {
  const turndownService = new TurndownService({
    headingStyle: "atx",
    bulletListMarker: "-",
    codeBlockStyle: "fenced",
  });

  // 可选：添加规则处理 quill 中可能的 <div> 或 <span>
  turndownService.addRule("customBlock", {
    filter: ["div", "span"],
    replacement: function (content) {
      return content; // 保留内容，忽略标签
    },
  });

  const html = quill.root.innerHTML;
  const markdown = turndownService.turndown(html);
  console.log("✅ 保存 Markdown：\n", markdown);
}

// 随机颜色
function getRandomColor() {
  const colors = ["#f44336", "#2196f3", "#4caf50", "#ff9800", "#9c27b0"];
  return colors[Math.floor(Math.random() * colors.length)];
}

function output() {
  const turndownService = new TurndownService({
    headingStyle: "atx",
    bulletListMarker: "-",
    codeBlockStyle: "fenced",
  });

  // 可选：添加规则处理 quill 中可能的 <div> 或 <span>
  turndownService.addRule("customBlock", {
    filter: ["div", "span"],
    replacement: function (content) {
      return content; // 保留内容，忽略标签
    },
  });

  const html = quill.root.innerHTML;
  const markdown = turndownService.turndown(html);
  console.log("✅ 导出 Markdown：\n", markdown);
}

function updateMembers(membersList) {
  // 直接更新整个成员列表
  members.value = membersList.map(member => ({
    id: member.id,
    name: member.username,
    avatar: member.avatar || userAvatar
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
  margin-bottom: 4px;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #ccc;
}

.member-name {
  font-size: 14px;
  color: #333;
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
}
</style>
