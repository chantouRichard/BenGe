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

function setupWebSocket() {
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

    socketState.socket.send(
      JSON.stringify({
        type: "auth",
        token,
        roomId:socketState.roomId,
        avatar:socketState.avatar,
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
    } else if (msg.type === "role") {
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
  // 遍历所有角色，查找是否有该用户已选择了其他角色
  for (let existingRole in socketState.roleSelections) {
    console.log("打印：",existingRole);
    // 如果当前角色是其他角色且该角色已经被用户名选择
    if (socketState.roleSelections[existingRole] === username && existingRole !== roleName) {
      // 清空原来选择的角色
      console.log(`${username} 已选择了 ${existingRole}，正在清空该角色的选择`);
      socketState.roleSelections[existingRole] = ''; // 清空原选择
    }
  }

  // 更新当前角色的选择
  socketState.roleSelections[roleName] = username;
  console.log("更新后的 socketState.roleSelections:", socketState.roleSelections);

  // 更新成员列表
  updateMembers(roleName, username);
  console.log("更新后的成员信息:", socketState.members);
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
