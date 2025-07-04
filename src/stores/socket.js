// socket.js
import { reactive } from "vue";
import { useCanvasStore } from "./canvasStore";
import { useCharacterStore } from "./character";
import { useClueStore } from "./clue";
import { useAtmosphereStore } from "./atmosphere";

// 不要在顶层调用 useStore，先定义变量
let canvasStore = null;
let characterStore = null;
let clueStore = null;
let atmosphereStore = null;

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

  userRole: -1,
  roles: [
    {
      name: "剧情设计师",
      description:
        "擅长构建故事主线与反转，通过精妙布局勾勒出跌宕起伏的剧情，掌控节奏与情感张力，引导玩家沉浸在虚构与现实交织的世界中。",
      task: "在画布上设计剧情主线与关键节点，包括起承转合、高潮反转、结局逻辑等，确保故事线完整且引人入胜。",
    },
    {
      name: "角色设计师",
      description:
        "负责塑造人物性格与关系网络，为每一个角色赋予鲜明动机与成长轨迹，让玩家在扮演中感受真实的情感与冲突。",
      task: "在画布中添加并完善角色节点，设定角色背景、动机、技能、物品与相互关系，构建角色成长路径与互动关系网。",
    },
    {
      name: "线索设计师",
      description:
        "精于埋设线索与误导，通过巧妙布局隐藏真相，引导推理节奏，确保玩家在抽丝剥茧中感受层层惊喜与挑战。",
      task: "在画布中添加线索节点及其关联关系，设计误导型线索、核心线索和关键证据链，明确每条线索的获取方式与逻辑归属。",
    },
    {
      name: "氛围设计师",
      description:
        "以视觉、音效与文本语言营造沉浸式体验，塑造紧张或诡秘的氛围，让每一处场景都充满戏剧张力，增强整体代入感。",
      task: "在画布中标注关键场景与氛围要素（如灯光、音效、环境设定），为每段剧情或线索交付设计匹配的情绪基调与视觉风格。",
    },
  ],
  // 第一阶段存储的方向
  direction: {
    title: "",
    description: "",
  },
});


// 在这里初始化 store，确保只初始化一次
async function ensureStores() {
  if (!canvasStore) {
    const { useCanvasStore } = await import("./canvasStore");
    const { useCharacterStore } = await import("./character");
    const { useClueStore } = await import("./clue");
    const { useAtmosphereStore } = await import("./atmosphere");

    canvasStore = useCanvasStore();
    characterStore = useCharacterStore();
    clueStore = useClueStore();
    atmosphereStore = useAtmosphereStore();
  }
}

async function setupWebSocket() {
  await ensureStores(); // 延迟注册 Pinia Store
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
        roomId: socketState.roomId,
        avatar: socketState.avatar,
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
      handleRoleSelection(msg.roleName, msg.username);
    } else if (
      msg.type === "canvas" ||
      msg.type === "character" ||
      msg.type === "clue" ||
      msg.type === "atmosphere"
    ) {
      handleCanvas(msg);
    } else if (msg.type === "vote") {
      handleVote(msg);
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
    console.log("打印：", existingRole);
    // 如果当前角色是其他角色且该角色已经被用户名选择
    if (
      socketState.roleSelections[existingRole] === username &&
      existingRole !== roleName
    ) {
      // 清空原来选择的角色
      console.log(`${username} 已选择了 ${existingRole}，正在清空该角色的选择`);
      socketState.roleSelections[existingRole] = ""; // 清空原选择
    }
  }

  // 更新当前角色的选择
  socketState.roleSelections[roleName] = username;
  console.log(
    "更新后的 socketState.roleSelections:",
    socketState.roleSelections
  );

  // 更新成员列表
  updateMembers(roleName, username);
  console.log("更新后的成员信息:", socketState.members);
}

// 同步画布
function handleCanvas(msg) {
  console.log("接收到canvas：", msg);

  if (msg.type == "canvas") {
    canvasStore.nodes = msg.nodes || [];
    canvasStore.edges = msg.edges || [];
  } else if (msg.type == "character") {
    characterStore.nodes = msg.characterNodes || [];
    characterStore.edges = msg.characterEdges || [];
  } else if (msg.type == "clue") {
    clueStore.nodes = [
      ...(msg.clueNodes || []),
      ...(msg.inferenceNodes || []),
      ...(msg.personNodes || []),
    ];
    clueStore.edges = msg.clueEdges || [];
  } else {
    atmosphereStore.nodes = msg.atmosphereNodes || [];
    atmosphereStore.edges = msg.atmosphereEdges || [];
  }
}

// 更新成员的角色选择状态
function updateMembers(roleName, username) {
  socketState.members.forEach((member) => {
    if (member.username === username) {
      member.selectedRole = roleName;
    }
  });
}

// 同步投票部分
function handleVote(msg) {
  if (msg.key && msg.username) {
    const member = socketState.members.find((m) => m.username === msg.username);
    if (member) {
      member.key = msg.key;
    }
  }
  if (msg.hasChosen) {
    const member = socketState.members.find((m) => m.username === msg.username);
    if (member) {
      member.hasChosen = msg.hasChosen;
    }
  }
  if (msg.vote) {
    const member = socketState.members.find((m) => m.username === msg.username);
    if (member) {
      member.vote = msg.vote;
    }
  }
  if (msg.hasVoted) {
    const member = socketState.members.find((m) => m.username === msg.username);
    if (member) {
      member.hasVoted = msg.hasVoted;
    }
  }
}

//

export { socketState, setupWebSocket, sendMessage, closeWebSocket };
