// socket.js
import { reactive } from "vue";
import { useCanvasStore } from "./canvasStore";

const canvasStore = useCanvasStore();

const socketState = reactive({
  socket: null,
  currentUserId: null,
  currentUsername: null,
  messages: [],
  members: [],
  isConnected: false,
  roomId: null,
  avatar: null,
  newMessage: "",

  roleSelections: {}, // 存储每个角色的选择状态，key是角色索引，value是已选择的用户名

  //   节点和边的信息
  nodes: {},
  edges: {},
});

function setupWebSocket(roomId, avatar) {
  if (socketState.socket && socketState.isConnected) {
    console.warn("WebSocket 已经连接");
    return;
  }

  const token = localStorage.getItem("token");
  if (!token) {
    console.error("未找到 token，无法建立 WebSocket 连接");
    return;
  }

  socketState.socket = new WebSocket("ws://localhost:7122/ws");

  socketState.socket.onopen = () => {
    console.log("WebSocket 连接已建立");
    socketState.roomId = roomId;
    socketState.avatar = avatar;

    socketState.socket.send(
      JSON.stringify({
        type: "auth",
        token,
        roomId,
        avatar,
      })
    );
    socketState.isConnected = true;
  };

  socketState.socket.onmessage = (event) => {
    const msg = JSON.parse(event.data);

    console.log("event:", msg);

    if (msg.type === "userInfo") {
      socketState.currentUserId = msg.userId;
      socketState.currentUsername = msg.username;
      console.log(
        "接收到用户信息:",
        socketState.currentUserId,
        socketState.currentUsername
      );
    } else if (msg.type === "chat") {
      socketState.messages.push({
        ...msg,
        isMe: msg.userId === socketState.currentUserId,
        sender: msg.username,
        content: msg.content,
        time: msg.time,
        avatar: msg.avatar || socketState.avatar,
      });
      console.log("数组：", socketState.messages);
    } else if (msg.type === "system") {
      socketState.messages.push({
        type: "system",
        content: msg.message,
        time: getCurrentTime(),
        isMe: false,
        isSystem: true,
      });
    } else if (msg.type === "members") {
      handleMembersUpdate(msg.members || []);
    } else if (msg.type === "error") {
      console.error("WebSocket 错误:", msg.message);
      alert("错误: " + msg.message);
    } else if (msg.type === "roleSelection") {
      // 这里更新角色选择的用户名
      handleRoleSelection(msg.roleName, msg.username);
    } else if(msg.type === "canvas"){
        handleCanvas(msg);
    }
  };

  socketState.socket.onclose = () => {
    console.log("WebSocket 连接已关闭");
    socketState.isConnected = false;
  };

  socketState.socket.onerror = (err) => {
    console.error("WebSocket 连接错误:", err);
    socketState.isConnected = false;
  };
}

function handleMembersUpdate(incomingMembers) {
  const existing = socketState.members || [];

  const mergedMap = new Map();

  existing.forEach((member) => {
    mergedMap.set(member.id, member);
  });

  incomingMembers.forEach((member) => {
    mergedMap.set(member.id, member);
  });

  socketState.members = Array.from(mergedMap.values());
}

function sendMessage() {
  console.log("sendMessage:", socketState.newMessage);
  if (socketState.isConnected && socketState.socket) {
    const messageData = {
      type: "chat",
      content: socketState.newMessage,
      time: getCurrentTime(),
      avatar: socketState.avatar,
    };
    socketState.socket.send(JSON.stringify(messageData));
    socketState.newMessage = "";
  } else {
    console.error("WebSocket 连接未就绪");
  }
}

function getCurrentTime() {
  const now = new Date();
  return `${now.getHours()}:${now.getMinutes().toString().padStart(2, "0")}`;
}

function closeWebSocket() {
  if (socketState.socket) {
    socketState.socket.close();
  }
}

// 角色选择广播
function handleRoleSelection(roleName, username) {
  // 更新角色选择信息，roleSelections 以角色名为键，值是用户名
  socketState.roleSelections[roleName] = username;

  // 更新成员列表
  updateMembers(roleName, username);
}

// 同步画布
function handleCanvas(msg){

    canvasStore.nodes = msg.nodes;
    canvasStore.edges = msg.edges;
}

// 更新成员的角色选择状态
function updateMembers(roleName, username) {
  socketState.members.forEach((member) => {
    if (member.username === username) {
      member.selectedRole = roleName;
    }
  });
}

//

export { socketState, setupWebSocket, sendMessage, closeWebSocket };
