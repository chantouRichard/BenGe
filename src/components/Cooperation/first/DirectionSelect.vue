<template>
  <div class="stage-container">
    <!-- 左侧方向选择区域 -->
    <div class="left-panel" :style="{ width: leftPanelWidth + '%' }">
      <h3 class="panel-title">剧本方向选择</h3>

      <!-- 目标区域和存放区域并排放置 -->
      <div class="direction-main">
        <div class="direction-container">
          <!-- 左侧目标区域 - 最多放置3个卡片 -->
          <div class="target-area">
            <h4 class="area-title">已选择方向 (最多3个)</h4>
            <div
                class="drop-zone"
                @dragover.prevent="handleDragOver"
                @drop="handleDrop"
                @dragleave="handleDragLeave"
                ref="targetAreaRef"
            >
              <div
                  v-for="(direction, index) in selectedDirections"
                  :key="'selected-'+direction.id"
                  class="direction-card"
                  draggable="true"
                  @dragstart="handleDragStart($event, direction, 'selected')"
              >
                <div class="card-content">{{ direction.content }}</div>
                <button class="remove-btn" @click="removeFromSelected(index)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
              <div
                  v-if="selectedDirections.length === 0"
                  class="drop-hint"
              >
                将卡片拖拽到此处
              </div>
            </div>
          </div>

          <!-- 右侧存放区域 - 可滚动查看所有卡片 -->
          <div class="storage-area">
            <h4 class="area-title">可选方向</h4>
            <div
                class="scroll-container"
                @dragover.prevent="handleDragOver"
                @drop="handleDrop"
                @dragleave="handleDragLeave"
                ref="storageAreaRef"
            >
              <div
                  v-for="(direction, index) in storedDirections"
                  :key="'stored-'+direction.id"
                  class="direction-card"
                  draggable="true"
                  @dragstart="handleDragStart($event, direction, 'stored')"
              >
                <div class="card-content">{{ direction.content }}</div>
                <button class="remove-btn" @click="removeFromStored(index)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
              <div
                  v-if="storedDirections.length === 0"
                  class="empty-hint"
              >
                暂无方向，请添加
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入框和添加按钮 -->
      <div class="bottom-fixed-area">
        <div class="input-area">
          <textarea
              v-model="newDirectionInput"
              placeholder="输入剧本主题标签描述"
              @keydown.enter.prevent="addNewDirection"
          ></textarea>
          <button
              class="add-btn"
              :disabled="!newDirectionInput.trim()"
              @click="addNewDirection"
          >
            <i class="fas fa-plus"></i> 添加
          </button>
        </div>

        <!-- 按钮区域 -->
        <div class="submit-area">
          <!-- 提交按钮 -->
          <button
              class="submit-btn"
              :disabled="selectedDirections.length === 0"
              @click="submitSelectedDirections"
          >
            提交选择
          </button>
          <!-- 确认按钮 -->
          <button
              class="confirm-submit-btn"
              @click="confirmAndProceed"
              :disabled="!hasSelectedDirections"
          >
            确认方向，进入下一阶段
          </button>
        </div>
      </div>
    </div>

    <!-- 拖拽调整区域宽度的分隔线 -->
    <div
        class="resize-handle"
        :style="{ left: leftPanelWidth + '%' }"
        @mousedown="startResize"
    ></div>

    <!-- 右侧对话区域 -->
    <div class="right-panel" :style="{ width: (100 - leftPanelWidth) + '%' }">
      <!-- 对话历史 -->
      <div class="chat-history" ref="chatHistoryRef">
        <div
            v-for="(message, index) in chatHistory"
            :key="index"
            :class="['message-card', message.sender === 'user' ? 'user-message' : 'ai-message']"
        >
          <div class="message-header">
            <div class="message-sender">{{ message.sender === 'user' ? '您' : 'AI助手' }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
          <div class="message-content" v-if="!message.isTyping">{{ message.content }}</div>
          <div v-else class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>

      <!-- 快捷提示区域 -->
      <div class="quick-prompts-panel">
        <div class="prompt-header">
          <h4>创作提示</h4>
          <button class="toggle-btn" @click="togglePrompts">
            <i :class="['fas', showPrompts ? 'fa-chevron-up' : 'fa-chevron-down']"></i>
            {{ showPrompts ? '收起' : '展开' }}
          </button>
        </div>

        <transition name="slide">
          <div v-if="showPrompts" class="prompt-content">
            <!-- 提示词分类标签 -->
            <div class="prompt-categories">
              <div
                  v-for="(category, idx) in promptCategories"
                  :key="idx"
                  :class="['category-tab', activeCategory === category.id ? 'active' : '']"
                  @click="activeCategory = category.id"
              >
                <i :class="getCategoryIcon(category.id)"></i>
                {{ category.name }}
              </div>
            </div>

            <!-- 快捷提示词网格 -->
            <div class="quick-prompts-grid">
              <div
                  v-for="(prompt, idx) in filteredPrompts"
                  :key="idx"
                  class="prompt-chip"
                  @click="insertAtCursor(prompt.text)"
                  :title="prompt.description"
              >
                {{ prompt.label }}
              </div>
            </div>
          </div>
        </transition>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <textarea
            ref="messageInputRef"
            v-model="messageInput"
            :style="{ height: inputHeight }"
            placeholder="描述您想要的剧本方向，例如：'我想要一个发生在清朝的悬疑剧本...'"
            @input="adjustInputHeight"
            @keydown.enter.prevent="sendMessage"
        ></textarea>
        <button
            class="send-btn"
            :disabled="!messageInput.trim() || isProcessing"
            @click="sendMessage"
        >
          <i v-if="isProcessing" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-paper-plane"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, defineEmits, defineProps } from 'vue';
import '@fortawesome/fontawesome-free/css/all.min.css';

// 面板宽度调整
const leftPanelWidth = ref(60);
const isResizing = ref(false);
const startX = ref(0);
const startWidth = ref(0);

// 左侧面板
const newDirectionInput = ref('');
const selectedDirections = ref([]); // 目标区域中的卡片
const storedDirections = ref([]);  // 存放区域中的卡片
const draggedItem = ref(null);
const draggedFrom = ref('');
const targetAreaRef = ref(null);
const storageAreaRef = ref(null);

// 右侧聊天区域
const chatHistory = ref([]);
const messageInput = ref('');
const isProcessing = ref(false);
const inputHeight = ref('50px');
const chatHistoryRef = ref(null);
const messageInputRef = ref(null);
const showPrompts = ref(true);
const activeCategory = ref('all');

// 提示词分类
const promptCategories = [
  { id: 'all', name: '全部' },
  { id: 'setting', name: '时代背景' },
  { id: 'genre', name: '类型风格' },
  { id: 'theme', name: '主题元素' }
];

// 提示词库
const promptLibrary = [
  { label: '古代背景', text: '我想要一个发生在古代的剧本，', category: 'setting', description: '设置故事发生在古代时期' },
  { label: '现代都市', text: '我想要一个现代都市背景的剧本，', category: 'setting', description: '设置故事发生在现代城市' },
  { label: '未来世界', text: '我想要一个未来世界背景的剧本，', category: 'setting', description: '设置故事发生在未来世界' },
  { label: '奇幻世界', text: '我想要一个奇幻世界背景的剧本，', category: 'setting', description: '设置故事发生在奇幻世界' },
  { label: '悬疑推理', text: '我想要一个悬疑推理类型的剧本，', category: 'genre', description: '设置剧本为悬疑推理类型' },
  { label: '爱情喜剧', text: '我想要一个爱情喜剧类型的剧本，', category: 'genre', description: '设置剧本为爱情喜剧类型' },
  { label: '科幻冒险', text: '我想要一个科幻冒险类型的剧本，', category: 'genre', description: '设置剧本为科幻冒险类型' },
  { label: '历史正剧', text: '我想要一个历史正剧类型的剧本，', category: 'genre', description: '设置剧本为历史正剧类型' },
  { label: '友情', text: '我想要一个关于友情的剧本，', category: 'theme', description: '以友情为主题' },
  { label: '成长', text: '我想要一个关于成长的剧本，', category: 'theme', description: '以成长为主题' },
  { label: '复仇', text: '我想要一个关于复仇的剧本，', category: 'theme', description: '以复仇为主题' },
  { label: '救赎', text: '我想要一个关于救赎的剧本，', category: 'theme', description: '以救赎为主题' }
];

// 左侧面板方法
const addNewDirection = () => {
  if (!newDirectionInput.value.trim()) return;

  const newDirection = {
    id: Date.now(),
    content: newDirectionInput.value.trim(),
    coreIdea: '这是自动生成的核心创意，可以后续编辑'
  };

  storedDirections.value.push(newDirection);
  newDirectionInput.value = '';

  nextTick(() => {
    const container = storageAreaRef.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
};

const handleDragStart = (event, item, from) => {
  draggedItem.value = item;
  draggedFrom.value = from;
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', '');
};

const handleDragOver = (event) => {
  event.preventDefault();
  event.dataTransfer.dropEffect = 'move';
  event.currentTarget.classList.add('drag-over');
};

const handleDragLeave = (event) => {
  event.currentTarget.classList.remove('drag-over');
};

const handleDrop = (event) => {
  event.preventDefault();
  event.currentTarget.classList.remove('drag-over');

  if (!draggedItem.value) return;

  // 判断拖拽目标区域
  const isTargetArea = event.currentTarget === targetAreaRef.value;

  if (isTargetArea) {
    // 拖拽到目标区域
    if (selectedDirections.value.length < 3 && !selectedDirections.value.some(dir => dir.id === draggedItem.value.id)) {
      // 从存放区域移除
      if (draggedFrom.value === 'stored') {
        const index = storedDirections.value.findIndex(dir => dir.id === draggedItem.value.id);
        if (index !== -1) {
          storedDirections.value.splice(index, 1);
          selectedDirections.value.push(draggedItem.value);
        }
      }
    }
  } else {
    // 拖拽回存放区域
    if (draggedFrom.value === 'selected') {
      const index = selectedDirections.value.findIndex(dir => dir.id === draggedItem.value.id);
      if (index !== -1) {
        selectedDirections.value.splice(index, 1);
        storedDirections.value.push(draggedItem.value);
      }
    }
  }

  draggedItem.value = null;
  draggedFrom.value = '';
};

const removeFromSelected = (index) => {
  const [removed] = selectedDirections.value.splice(index, 1);
  storedDirections.value.push(removed);
};

const removeFromStored = (index) => {
  storedDirections.value.splice(index, 1);
};

const hasSelectedDirections = computed(() => {
  return selectedDirections.value?.length > 0;
});

const confirmAndProceed = async () => {
  if (!hasSelectedDirections.value) return;

  try {
    const submissionData = {
      userId: props.userInfo.id || 'user_' + Math.floor(Math.random() * 1000),
      username: props.userInfo.name || '用户' + Math.floor(Math.random() * 1000),
      directions: selectedDirections.value.map(dir => ({
        id: dir.id,
        content: dir.content,
        coreIdea: dir.coreIdea
      })),
      timestamp: new Date().toISOString()
    };

    // 2. 发送到后端进行处理
    const response = await fetch('https://', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(submissionData)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    // 3. 触发切换到Vote组件
    emit('next-stage', {
      directions: selectedDirections.value,
      processingResult: result
    });

    // 添加到聊天历史
    chatHistory.value.push({
      sender: 'ai',
      content: '已确认方向，进入投票阶段！',
      timestamp: new Date()
    });

  } catch (error) {
    console.error('确认方向失败:', error);
    chatHistory.value.push({
      sender: 'ai',
      content: '确认方向失败，请重试',
      timestamp: new Date()
    });
  } finally {
    scrollToBottom();
    emit('next-stage', {});
  }
};

const submitSelectedDirections = async () => {
  if (selectedDirections.value.length === 0) return;

  try {
    // 准备要提交的数据
    const submissionData = {
      directions: selectedDirections.value.map(dir => ({
        id: dir.id,
        content: dir.content,
        coreIdea: dir.coreIdea
      })),
      timestamp: new Date().toISOString()
    };

    // 显示提交中状态
    isProcessing.value = true;

    // 调用API接口
    const response = await fetch('https://', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(submissionData)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    // 添加到聊天历史
    chatHistory.value.push({
      sender: 'ai',
      content: '已成功提交您的剧本方向选择！',
      timestamp: new Date()
    });

    // 触发父组件更新成员选择
    emit('submission', {
      directions: selectedDirections.value,
      username: props.userInfo.name || '用户' + Math.floor(Math.random() * 1000)
    });

    console.log('提交成功:', result);
  } catch (error) {
    console.error('提交方向失败:', error);
    chatHistory.value.push({
      sender: 'ai',
      content: `提交失败`,
      timestamp: new Date()
    });
  } finally {
    isProcessing.value = false;
    scrollToBottom();

    emit('submission', {
      directions: selectedDirections.value,
      username: props.userInfo.name || '用户' + Math.floor(Math.random() * 1000)
    });
  }
};

// 右侧面板方法
const filteredPrompts = computed(() => {
  return promptLibrary.filter(prompt => {
    return activeCategory.value === 'all' || prompt.category === activeCategory.value;
  });
});

const getCategoryIcon = (categoryId) => {
  switch(categoryId) {
    case 'setting': return 'fas fa-clock';
    case 'genre': return 'fas fa-film';
    case 'theme': return 'fas fa-palette';
    default: return 'fas fa-th-list';
  }
};

const togglePrompts = () => {
  showPrompts.value = !showPrompts.value;
};

const insertAtCursor = (text) => {
  const textarea = messageInputRef.value;
  if (!textarea) return;

  const startPos = textarea.selectionStart;
  const endPos = textarea.selectionEnd;
  const beforeText = messageInput.value.substring(0, startPos);
  const afterText = messageInput.value.substring(endPos);

  messageInput.value = beforeText + text + afterText;

  nextTick(() => {
    textarea.focus();
    const newCursorPos = startPos + text.length;
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    adjustInputHeight();
  });
};

const adjustInputHeight = () => {
  const textarea = messageInputRef.value;
  if (textarea) {
    textarea.style.height = 'auto';
    const newHeight = Math.min(Math.max(textarea.scrollHeight, 50), 150);
    inputHeight.value = `${newHeight}px`;
  }
};

const sendMessage = async () => {
  if (!messageInput.value.trim() || isProcessing.value) return;

  chatHistory.value.push({
    sender: 'user',
    content: messageInput.value,
    timestamp: new Date()
  });

  isProcessing.value = true;
  messageInput.value = '';

  try {
    chatHistory.value.push({
      sender: 'ai',
      content: '',
      isTyping: true,
      timestamp: new Date()
    });

    // 模拟API响应
    setTimeout(() => {
      chatHistory.value.pop();
      chatHistory.value.push({
        sender: 'ai',
        content: '我已收到您的剧本方向描述。',
        timestamp: new Date()
      });
      isProcessing.value = false;
      scrollToBottom();
    }, 1000);
  } catch (error) {
    console.error('Error sending message:', error);
    chatHistory.value.push({
      sender: 'ai',
      content: '抱歉，发送消息时出现了错误。',
      timestamp: new Date()
    });
    isProcessing.value = false;
    scrollToBottom();
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight;
    }
  });
};

// 通用方法
const formatTime = (date) => {
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

const startResize = (e) => {
  isResizing.value = true;
  startX.value = e.clientX;
  startWidth.value = leftPanelWidth.value;
  document.addEventListener('mousemove', handleResize);
  document.addEventListener('mouseup', stopResize);
};

const handleResize = (e) => {
  if (!isResizing.value) return;
  const deltaX = e.clientX - startX.value;
  const containerWidth = document.querySelector('.stage-container').offsetWidth;
  const deltaPercent = (deltaX / containerWidth) * 100;
  leftPanelWidth.value = Math.min(Math.max(30, startWidth.value + deltaPercent), 70);
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener('mousemove', handleResize);
  document.removeEventListener('mouseup', stopResize);
};

// 定义props和emit
const props = defineProps({
  userInfo: {
    type: Object,
    default: () => ({
      id: 'user_' + Math.floor(Math.random() * 1000),
      name: '用户' + Math.floor(Math.random() * 1000)
    })
  }
});

const emit = defineEmits(['submission', 'next-stage']);

// 初始化
onMounted(() => {
  // 初始化一些示例数据
  storedDirections.value = [
    { id: 1, content: '古代宫廷权谋剧', coreIdea: '围绕皇位继承展开的权谋斗争' },
    { id: 2, content: '现代都市爱情故事', coreIdea: '两个性格迥异的都市白领的爱情故事' },
    { id: 3, content: '科幻太空探险', coreIdea: '人类首次星际旅行中发现的宇宙奥秘' },
    { id: 4, content: '校园青春成长', coreIdea: '一群高中生的成长与蜕变' }
  ];

  if (chatHistory.value.length === 0) {
    chatHistory.value.push({
      sender: 'ai',
      content: '你好！我是你的剧本创作助手。请告诉我你想要创作的剧本类型、时代背景或主题，我将为你提供创作建议。',
      timestamp: new Date()
    });
  }

  nextTick(() => {
    scrollToBottom();
    if (messageInputRef.value) {
      messageInputRef.value.focus();
    }
  });
});
</script>

<style scoped>
/* 原有的样式保持不变 */
.drop-zone.drag-over,
.scroll-container.drag-over {
  background-color: rgba(99, 102, 241, 0.1);
  border-color: #6366F1;
}

.stage-container {
  display: flex;
  height: 100%;
  position: relative;
  min-height: 0;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 15px;
  box-sizing: border-box;
  overflow: hidden;
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 15px 0;
  text-align: center;
  flex-shrink: 0;
}

.direction-main {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.direction-container {
  display: flex;
  gap: 15px;
  height: 100%;
  min-height: 0;
}

.target-area, .storage-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.area-title {
  font-size: 16px;
  font-weight: 500;
  color: #555;
  margin: 0 0 10px 0;
  font-family: 'Montserrat', 'Arial', sans-serif;
  flex-shrink: 0;
}

.drop-zone, .scroll-container {
  flex: 1;
  min-height: 150px;
  max-height: 100%;
  overflow-y: auto;
  border: 2px dashed #ccc;
  border-radius: 6px;
  padding: 10px;
  transition: all 0.2s;
}

.scroll-container {
  border: 1px solid #eee;
}

.drop-hint {
  width: 100%;
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
  margin: auto;
}

.empty-hint {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
  margin: auto;
}

.direction-card {
  background-color: white;
  border-radius: 6px;
  padding: 12px 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  cursor: move;
  position: relative;
  transition: all 0.2s;
  border-left: 4px solid #6366F1;
  margin-bottom: 8px;
}

.direction-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.card-content {
  font-size: 14px;
  line-height: 1.4;
  padding-right: 20px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.remove-btn {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 20px;
  height: 20px;
  border: none;
  background: none;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.remove-btn:hover {
  color: #ff4757;
  background-color: rgba(255, 71, 87, 0.1);
}

.bottom-fixed-area {
  margin-top: auto;
  flex-shrink: 0;
  width: 100%;
}

.input-area {
  display: flex;
  gap: 8px;
  width: 100%;
}

.input-area textarea {
  flex: 1;
  min-height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  resize: none;
  font-size: 14px;
  line-height: 1.4;
  font-family: 'Georgia', 'Times New Roman', serif;
  box-sizing: border-box;
}

.input-area textarea:focus {
  outline: none;
  border-color: #6366F1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.add-btn {
  width: 80px;
  height: 80px;
  padding: 8px;
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.add-btn:hover:not(:disabled) {
  background-color: #4F46E5;
}

.add-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.submit-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 15px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #10B981;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background-color: #0D9F6E;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
}

.submit-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.confirm-submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 10px;
}

.confirm-submit-btn:hover:not(:disabled) {
  background-color: #3e8e41;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.confirm-submit-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  opacity: 0.7;
}

.resize-handle {
  position: absolute;
  top: 0;
  width: 10px;
  height: 100%;
  background-color: transparent;
  cursor: col-resize;
  z-index: 10;
  transform: translateX(-50%);
}

.resize-handle:hover,
.resize-handle:active {
  background-color: rgba(99, 102, 241, 0.1);
}

/* 右侧面板 */
.right-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.chat-history {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
}

.message-card {
  max-width: 85%;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s forwards;
  margin-bottom: 12px;
}

.user-message {
  align-self: flex-end;
  background-color: #e1f5fe;
  border-bottom-right-radius: 2px;
}

.ai-message {
  align-self: flex-start;
  background-color: white;
  border-bottom-left-radius: 2px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.message-sender {
  font-size: 13px;
  font-weight: 600;
  color: #555;
}

.message-time {
  font-size: 12px;
  color: #888;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  white-space: pre-line;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 5px 0;
}

.typing-indicator span {
  height: 8px;
  width: 8px;
  border-radius: 50%;
  background-color: #6366F1;
  display: inline-block;
  margin: 0 2px;
  animation: bounce 1.5s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-8px); }
}

.quick-prompts-panel {
  background-color: white;
  border-top: 1px solid #eaeaea;
  overflow: hidden;
  flex-shrink: 0;
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
}

.prompt-header h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
  font-family: 'Montserrat', 'Arial', sans-serif;
}

.toggle-btn {
  background: none;
  border: none;
  color: #6366F1;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.prompt-content {
  padding: 0 16px 12px;
}

.prompt-categories {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 4px 10px;
  font-size: 12px;
  background-color: #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.category-tab:hover {
  background-color: #e0e0ff;
}

.category-tab.active {
  background-color: #6366F1;
  color: white;
}

.quick-prompts-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.prompt-chip {
  padding: 5px 10px;
  background-color: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-chip:hover {
  background-color: #e0e0ff;
  border-color: #d0d0ff;
  transform: translateY(-2px);
}

.input-area {
  display: flex;
  padding: 12px 16px;
  background-color: white;
  border-top: 1px solid #eaeaea;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.input-area textarea {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  outline: none;
  transition: all 0.3s;
  font-family: 'Georgia', 'Times New Roman', serif;
  min-height: 50px;
  max-height: 150px;
  box-sizing: border-box;
}

.input-area textarea:focus {
  border-color: #6366F1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #6366F1;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background-color: #4F46E5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.send-btn:disabled {
  background-color: #d1d1d1;
  cursor: not-allowed;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
  overflow: hidden;
}
</style>