<template>
  <div class="framework-stage-container">
    <!-- 左侧剧本内容展示区 -->
    <div class="script-content-panel" :style="{ width: leftPanelWidth + '%' }">
      <div class="panel-header">
        <h3 class="panel-title">剧本框架</h3>
        
        <div class="panel-actions">
          <button class="complete-script-btn" @click="generateCompleteScript">
            <i class="fas fa-file-alt"></i>
            生成完整剧本
          </button>
          <button class="analysis-btn" @click="toggleAnalysisCanvas">
            <i class="fas fa-chart-pie"></i>
            剧本分析
          </button>
        </div>
      </div>
      
      <div class="script-content-wrapper">
        <transition name="content-transition" mode="out-in">
          <div :key="contentUpdateCounter" class="script-content">
            <div v-if="scriptContent" v-html="formattedContent"></div>
            <div v-else class="empty-content">
              <div class="empty-icon">📝</div>
              <p>开始对话，创建您的剧本框架</p>
            </div>
          </div>
        </transition>
      </div>
    </div>
    
    <!-- 拖拽调整区域宽度的分隔线 -->
    <div 
      class="resize-handle" 
      :style="{ left: leftPanelWidth + '%' }"
      @mousedown="startResize"
    ></div>
    
    <!-- 右侧面板 -->
    <div class="interaction-panel" :class="{ 'panel-collapsed': !showRightPanel }" :style="{ width: showRightPanel ? (100 - leftPanelWidth) + '%' : '5%' }">
      <!-- 当面板收起时显示的展开按钮 -->
      <button v-if="!showRightPanel" class="expand-btn" @click="toggleRightPanel">
        <i class="fas fa-chevron-left"></i>
      </button>
      
      <!-- 收起按钮，始终显示在左侧边界 -->
      <button v-if="showRightPanel" class="collapse-btn" @click="toggleRightPanel">
        <i class="fas fa-chevron-right"></i>
      </button>
      
      <!-- 只在面板展开时显示内容 -->
      <template v-if="showRightPanel">
        <!-- 对话区域头部 -->
        <div class="chat-header">
          <h4>对话历史</h4>
        </div>
        
        <!-- 对话区域 -->
        <transition name="slide">
          <div v-if="showChatHistory" class="chat-section" ref="chatSectionRef">
            <div class="chat-messages" ref="chatHistoryRef">
              <div 
                v-for="(message, index) in displayChatHistory" 
                :key="index"
                :class="['message-card', message.sender === 'user' ? 'user-message' : 'ai-message']"
              >
                <div class="message-avatar" v-if="message.sender === 'ai'">
                  <div class="avatar-circle ai">AI</div>
                </div>
                
                <div class="message-container">
                  <div class="message-bubble">
                    <div class="message-content" v-if="!message.isTyping" v-html="formatMessage(message.content)"></div>
                    <div v-else class="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                  <!-- 只显示AI消息的时间 -->
                  <div class="message-time" v-if="message.sender === 'ai'">{{ formatTime(message.timestamp) }}</div>
                </div>
                
                <div class="message-avatar" v-if="message.sender === 'user'">
                  <div class="avatar-circle user">{{ userInitial }}</div>
                </div>
              </div>
            </div>
          </div>
        </transition>
        
        <!-- 提示词区域 -->
        <div class="prompt-suggestions">
          <div class="prompt-header">
            <h4>创作提示</h4>
            <div class="category-tabs">
              <button 
                v-for="category in promptCategories" 
                :key="category.id"
                :class="['category-tab', currentCategory === category.id ? 'active' : '']"
                @click="setCurrentCategory(category.id)"
              >
                <i :class="getCategoryIcon(category.id)"></i>
                {{ category.name }}
              </button>
            </div>
            <button class="toggle-btn" @click="togglePrompts">
              <i :class="['fas', showPrompts ? 'fa-chevron-up' : 'fa-chevron-down']"></i>
              {{ showPrompts ? '收起' : '展开' }}
            </button>
          </div>
          
          <transition name="slide">
            <div v-if="showPrompts" class="prompt-cards">
              <div 
                v-for="prompt in filteredPrompts" 
                :key="prompt.label"
                class="prompt-card"
                @click="insertPrompt(prompt.text)"
              >
                <div class="prompt-icon">
                  <i :class="getCategoryIcon(prompt.category)"></i>
                </div>
                <div class="prompt-info">
                  <div class="prompt-title">{{ prompt.label }}</div>
                  <div class="prompt-desc">{{ prompt.description }}</div>
                </div>
              </div>
            </div>
          </transition>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-area-wrapper">
          <div class="input-area">
            <div class="input-container">
              <textarea 
                ref="messageInputRef"
                v-model="messageInput"
                :style="{ height: inputHeight }"
                placeholder="输入您的创作指令..."
                @input="adjustInputHeight"
                @keydown.enter.prevent="sendMessage"
              ></textarea>
            </div>
            
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
      </template>
    </div>
    <!-- 剧本分析画布 -->
    <transition name="canvas-slide">
      <div v-if="showAnalysisCanvas" class="analysis-canvas">
        <div class="canvas-backdrop" @click="toggleAnalysisCanvas"></div>
        <div class="canvas-content-wrapper">
          <div class="canvas-header">
            <h3><i class="fas fa-chart-bar"></i> 剧本分析</h3>
            <button class="close-canvas-btn" @click="toggleAnalysisCanvas">
              <i class="fas fa-times"></i>
            </button>
          </div>
          <div class="canvas-content">
            <div v-if="isAnalyzing" class="analysis-loading">
              <div class="analysis-icon">
                <i class="fas fa-brain fa-pulse"></i>
              </div>
              <h3 class="analysis-loading-title">AI正在分析您的剧本</h3>
              <div class="analysis-progress">
                <span><i class="fas fa-cog fa-spin"></i> 分析剧情结构</span>
                <span><i class="fas fa-users fa-spin"></i> 评估角色设计</span>
                <span><i class="fas fa-lightbulb fa-spin"></i> 提取创意亮点</span>
              </div>
            </div>
            <div v-else-if="scriptStore.analysis" class="analysis-result">
              <div v-html="marked(scriptStore.analysis)"></div>
            </div>
            <div v-else class="analysis-empty">
              <div class="empty-icon"><i class="fas fa-chart-pie fa-3x"></i></div>
              <p>暂无分析结果，请稍后再试</p>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 剧本生成画布 -->
    <transition name="canvas-slide">
      <div v-if="showGeneratingCanvas" class="generating-canvas">
        <div class="canvas-backdrop"></div>
        <div class="canvas-content-wrapper">
          <div class="canvas-header">
            <h3><i class="fas fa-magic"></i> 生成完整剧本</h3>
          </div>
          <div class="canvas-content">
            <div class="generating-loading">
              <div class="generating-icon">
                <i class="fas fa-pen-fancy fa-pulse"></i>
              </div>
              <h3 class="generating-loading-title">AI正在生成完整剧本</h3>
              <div class="generating-progress">
                <span><i class="fas fa-feather fa-spin"></i> 整合框架内容</span>
                <span><i class="fas fa-book fa-spin"></i> 扩展剧情细节</span>
                <span><i class="fas fa-theater-masks fa-spin"></i> 完善角色对话</span>
                <span><i class="fas fa-check-circle fa-spin"></i> 润色最终文稿</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, defineProps, onMounted, nextTick, watch } from 'vue';
import { usescriptStore } from '@/stores/scriptStore';
import { marked } from 'marked';
import '@fortawesome/fontawesome-free/css/all.min.css';

const scriptStore = usescriptStore();

const props = defineProps({
  userInfo: {
    type: String,
    required: true
  }
});

// 用户头像显示的首字母
const userInitial = computed(() => {
  return props.userInfo ? props.userInfo.charAt(0).toUpperCase() : 'U';
});

// 本地状态
const localChatHistory = ref([]); // 本地临时消息
const messageInput = ref('');
const isProcessing = ref(false);
const isAnalyzing = ref(false);
const inputHeight = ref('50px');
const chatHistoryRef = ref(null);
const messageInputRef = ref(null);
const chatSectionRef = ref(null);
const contentUpdateCounter = ref(0);
const lastUserMessageId = ref(null); // 跟踪最后一条用户消息的ID

// 合并存储的聊天历史和本地临时消息
const displayChatHistory = computed(() => {
  // 从store获取持久化的聊天历史
  const storedHistory = scriptStore.chatHistory || [];
  
  // 合并持久化历史和本地临时消息
  return [...storedHistory, ...localChatHistory.value];
});

// 面板宽度调整
const leftPanelWidth = ref(45); // 默认左侧面板宽度45%
const isResizing = ref(false);
const startX = ref(0);
const startWidth = ref(0);

// 提示词显示控制
const showPrompts = ref(true);
const currentCategory = ref('all'); // 当前选中的分类

// 对话历史显示控制
const showChatHistory = ref(true);

// 右侧面板显示控制
const showRightPanel = ref(true);

// 剧本分析画布控制
const showAnalysisCanvas = ref(false);

// 控制生成动画显示
const showGeneratingCanvas = ref(false);

// 剧本内容
const scriptContent = computed(() => {
  return scriptStore.scriptContent || '';
});

// 格式化剧本内容（支持Markdown）
const formattedContent = computed(() => {
  return scriptContent.value ? marked(scriptContent.value) : '';
});

// 提示词分类
const promptCategories = [
  { id: 'all', name: '全部' },
  { id: 'character', name: '角色设计' },
  { id: 'plot', name: '情节构建' },
  { id: 'scene', name: '场景设置' },
  { id: 'rule', name: '游戏规则' }
];

// 获取分类图标
const getCategoryIcon = (categoryId) => {
  switch(categoryId) {
    case 'character': return 'fas fa-user-circle';
    case 'plot': return 'fas fa-book-open';
    case 'scene': return 'fas fa-map-marked-alt';
    case 'rule': return 'fas fa-gavel';
    default: return 'fas fa-th-list';
  }
};

// 提示词库
const promptLibrary = [
  // 角色设计类提示词
  { 
    label: '主角设定', 
    text: '请为这个奇幻世界设计一个主要角色，包括：\n1. 角色背景和身份\n2. 特殊能力或技能\n3. 性格特点\n4. 与故事的关联', 
    category: 'character', 
    description: '设计主要角色的详细信息' 
  },
  { 
    label: '反派角色', 
    text: '请设计一个有深度的反派角色，包括：\n1. 成为反派的动机\n2. 独特的能力或资源\n3. 与主角的关系\n4. 隐藏的弱点', 
    category: 'character', 
    description: '创建有深度的反派角色' 
  },
  { 
    label: '配角群像', 
    text: '请设计3-5个配角，每个角色都应该：\n1. 有独特的身份和背景\n2. 具备特殊技能或知识\n3. 在故事中扮演特定角色\n4. 与主角有某种联系', 
    category: 'character', 
    description: '设计多个配角形成角色群像' 
  },
  { 
    label: '角色关系网', 
    text: '请设计角色之间的复杂关系网，包括：\n1. 各角色之间的联系\n2. 潜在的矛盾和冲突\n3. 隐藏的秘密或误解\n4. 可能的结盟或背叛', 
    category: 'character', 
    description: '构建角色之间的关系网络' 
  },
  
  // 情节构建类提示词
  { 
    label: '核心谜题', 
    text: '请设计本剧本的核心谜题，包括：\n1. 谜题的本质和背景\n2. 关键线索的分布\n3. 误导性线索\n4. 解谜的关键步骤', 
    category: 'plot', 
    description: '设计剧本的核心谜题' 
  },
  { 
    label: '事件时间线', 
    text: '请设计一条清晰的事件时间线，包括：\n1. 案件发生前的关键事件\n2. 案件发生的具体过程\n3. 玩家介入后的事件发展\n4. 可能的结局分支', 
    category: 'plot', 
    description: '构建完整的事件时间线' 
  },
  { 
    label: '线索设计', 
    text: '请设计一系列巧妙的线索，包括：\n1. 物理线索（物品、痕迹等）\n2. 证词线索（对话、回忆等）\n3. 环境线索（场景特征、气候等）\n4. 线索的获取难度和顺序', 
    category: 'plot', 
    description: '设计多样化的线索系统' 
  },
  { 
    label: '剧情转折', 
    text: '请设计2-3个重要的剧情转折点，包括：\n1. 转折的触发条件\n2. 转折对故事的影响\n3. 玩家可能的反应\n4. 如何保持故事连贯性', 
    category: 'plot', 
    description: '设计引人入胜的剧情转折' 
  },
  
  // 场景设置类提示词
  { 
    label: '主要场景', 
    text: '请设计3-5个关键场景，每个场景包括：\n1. 场景的物理描述\n2. 场景的氛围和感觉\n3. 场景中可交互的元素\n4. 场景中隐藏的线索或秘密', 
    category: 'scene', 
    description: '设计关键场景的详细信息' 
  },
  { 
    label: '世界观构建', 
    text: '请详细描述这个奇幻世界的设定，包括：\n1. 世界的基本规则和特性\n2. 主要地理区域和文化\n3. 魔法或科技系统\n4. 历史背景和重要事件', 
    category: 'scene', 
    description: '构建完整的世界观背景' 
  },
  { 
    label: '场景转换', 
    text: '请设计场景之间的转换机制，包括：\n1. 玩家如何在场景间移动\n2. 时间流逝的处理方式\n3. 场景变化对剧情的影响\n4. 特殊场景的解锁条件', 
    category: 'scene', 
    description: '设计流畅的场景转换系统' 
  },
  { 
    label: '氛围营造', 
    text: '请设计如何营造奇幻悬疑的氛围，包括：\n1. 视觉描述和细节\n2. 声音和音乐元素\n3. 情绪渲染的方式\n4. 紧张感和神秘感的构建', 
    category: 'scene', 
    description: '设计营造沉浸式氛围的方法' 
  },
  
  // 游戏规则类提示词
  { 
    label: '能力系统', 
    text: '请设计角色能力系统，包括：\n1. 基本能力类型和分类\n2. 能力的获取和提升方式\n3. 能力的限制和消耗\n4. 能力之间的相互作用', 
    category: 'rule', 
    description: '设计角色能力系统规则' 
  },
  { 
    label: '推理机制', 
    text: '请设计推理机制，包括：\n1. 玩家如何收集和分析线索\n2. 推理的验证方式\n3. 错误推理的后果\n4. 推理成功的奖励', 
    category: 'rule', 
    description: '设计推理和解谜机制' 
  },
  { 
    label: '互动规则', 
    text: '请设计角色互动规则，包括：\n1. 角色间对话和信息交换\n2. 合作和竞争机制\n3. 信任和背叛系统\n4. 团队决策的处理方式', 
    category: 'rule', 
    description: '设计角色互动和合作规则' 
  },
  { 
    label: '胜利条件', 
    text: '请设计游戏的胜利条件，包括：\n1. 主要目标和次要目标\n2. 个人和团队胜利条件\n3. 多结局的触发条件\n4. 评分或成就系统', 
    category: 'rule', 
    description: '设计游戏胜利和结局系统' 
  }
];

// 根据当前分类过滤提示词
const filteredPrompts = computed(() => {
  if (currentCategory.value === 'all') {
    return promptLibrary;
  } else {
    return promptLibrary.filter(prompt => prompt.category === currentCategory.value);
  }
});

// 设置当前分类
const setCurrentCategory = (categoryId) => {
  currentCategory.value = categoryId;
};

// 开始调整宽度
const startResize = (e) => {
  isResizing.value = true;
  startX.value = e.clientX;
  startWidth.value = leftPanelWidth.value;
  
  document.addEventListener('mousemove', handleResize);
  document.addEventListener('mouseup', stopResize);
};

// 处理宽度调整
const handleResize = (e) => {
  if (!isResizing.value) return;
  
  const deltaX = e.clientX - startX.value;
  const containerWidth = document.querySelector('.framework-stage-container').offsetWidth;
  const deltaPercent = (deltaX / containerWidth) * 100;
  
  // 限制最小和最大宽度
  leftPanelWidth.value = Math.min(Math.max(30, startWidth.value + deltaPercent), 70);
};

// 停止调整宽度
const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener('mousemove', handleResize);
  document.removeEventListener('mouseup', stopResize);
};

// 切换提示词显示
const togglePrompts = () => {
  showPrompts.value = !showPrompts.value;
};

// 切换右侧面板显示
const toggleRightPanel = () => {
  showRightPanel.value = !showRightPanel.value;
  
  // 调整左侧面板宽度
  if (showRightPanel.value) {
    leftPanelWidth.value = 45; // 恢复默认宽度
  } else {
    leftPanelWidth.value = 95; // 收起时左侧面板几乎占满
  }
};

// 切换剧本分析画布
const toggleAnalysisCanvas = async () => {
  showAnalysisCanvas.value = !showAnalysisCanvas.value;
  try {
    // 当显示分析画布时，禁止背景滚动
    if (showAnalysisCanvas.value) {
      document.body.style.overflow = 'hidden';
      await startAnalysis();
    } else {
      document.body.style.overflow = '';
    }
  } catch (error) {
    console.error("切换画布失败", error);
  }
};

const startAnalysis = async () => {
  if (isAnalyzing.value) return;

  isAnalyzing.value = true;

  try{
    console.log('开始分析');
    await scriptStore.UpdateAnalysis();
  } catch (error) {
   console.error("分析失败", error); 
  } finally {
    isAnalyzing.value = false;
  }
}

// 在光标位置插入文本
const insertPrompt = (text) => {
  const textarea = messageInputRef.value;
  if (!textarea) return;
  
  const startPos = textarea.selectionStart;
  const endPos = textarea.selectionEnd;
  const beforeText = messageInput.value.substring(0, startPos);
  const afterText = messageInput.value.substring(endPos);
  
  messageInput.value = beforeText + text + afterText;
  
  nextTick(() => {
    // 设置光标位置到插入文本之后
    textarea.focus();
    const newCursorPos = startPos + text.length;
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    adjustInputHeight();
  });
};

// 调整输入框高度
const adjustInputHeight = () => {
  const textarea = messageInputRef.value;
  if (textarea) {
    textarea.style.height = 'auto';
    const newHeight = Math.min(Math.max(textarea.scrollHeight, 50), 150);
    inputHeight.value = `${newHeight}px`;
  }
};


// 格式化消息内容（支持Markdown）
const formatMessage = (content) => {
  return marked(content);
};

const sendMessage = async () => {
  if (!messageInput.value.trim() || isProcessing.value) return;

  // 生成临时消息ID
  const tempMessageId = Date.now().toString();

  // 添加用户消息到本地临时历史（不会持久化）
  localChatHistory.value.push({
    id: tempMessageId,
    sender: 'user',
    content: messageInput.value,
    timestamp: new Date()
  });

  // 保存最后一条用户消息ID，用于后续去重
  lastUserMessageId.value = tempMessageId;

  isProcessing.value = true;
  
  adjustInputHeight();

  try {
    // 添加AI正在输入的提示
    localChatHistory.value.push({
      sender: 'ai',
      content: '思考中...',
      isTyping: true,
      timestamp: new Date()
    });

    // 调用API更新剧本内容
    const result = await scriptStore.UpdateFramework(messageInput.value);
    
    // 清空输入框
    messageInput.value = '';
    // 移除正在输入的提示
    localChatHistory.value.pop();
    
    if (result) {
      // 更新剧本内容（触发动画）
      contentUpdateCounter.value++;
      
      // 清空本地临时消息（因为后端会返回完整的历史记录）
      localChatHistory.value = [];
      
    }
  } catch (error) {
    console.error("更新剧本内容失败", error);
    
    // 移除正在输入的提示
    localChatHistory.value.pop();
    
    // 添加错误提示
    localChatHistory.value.push({
    sender: 'ai',
    content: '抱歉，更新剧本内容时出现错误，请重试。',
    timestamp: new Date()
  });
} finally {
    isProcessing.value = false;
    messageInput.value = '';
    inputHeight.value = '50px';
    
    // 聊天历史滚动到底部
    nextTick(() => {
      if (chatHistoryRef.value) {
        chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight;
      }
      if (messageInputRef.value) {
        messageInputRef.value.focus();
      }
      });
  }
};

// 进入下一阶段
const generateCompleteScript = async () => {
  try {
    showGeneratingCanvas.value = true;
    isProcessing.value = true; // 设置处理中状态.这时候不允许对话
    await scriptStore.GenerateScript();
  } catch (error) {
    console.error("生成剧本失败", error);
  } finally {
    isProcessing.value = false; // 处理完成后重置状态
    showGeneratingCanvas.value = false;
  }
};

// 格式化时间
const formatTime = (date) => {
  if (!date) return '';
  const dateObj = date instanceof Date ? date : new Date(date);
  const hours = dateObj.getHours().toString().padStart(2, '0');
  const minutes = dateObj.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

// 当聊天历史更新时，自动滚动到底部
watch(() => displayChatHistory.value.length, () => {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight;
    }
  });
});

// 组件挂载后
onMounted(() => {
  // 如果没有聊天历史，添加欢迎消息
  if (displayChatHistory.value.length === 0) {
    localChatHistory.value.push({
      sender: 'ai',
      content: '欢迎进入框架设计阶段！您可以开始完善剧本框架，添加角色、情节和场景等元素。请告诉我您想如何发展这个故事。',
      timestamp: new Date()
    });
  }
  
  // 聊天历史滚动到底部
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight;
    }
  });
  
  // 输入框聚焦
  if (messageInputRef.value) {
    messageInputRef.value.focus();
  }
});
</script>

<style scoped>
.framework-stage-container {
  display: flex;
  height: 100%;
  position: relative;
  overflow: hidden;
  font-family: 'Segoe UI', Arial, sans-serif;
}

/* 左侧剧本内容展示区 */
.script-content-panel {
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
  transition: width 0.3s ease;
  z-index: 1;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
  font-family: 'Montserrat', 'Arial', sans-serif;
}

.panel-actions {
  display: flex;
  gap: 10px;
}

.complete-script-btn {
  background-color: #10B981;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-right: 10px;
}

.complete-script-btn:hover {
  background-color: #059669;
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

.script-content-wrapper {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

.script-content {
  line-height: 1.8;
  color: #333;
  font-size: 15px;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  text-align: center;
  color: #888;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

/* 内容更新动画 */
.content-transition-enter-active,
.content-transition-leave-active {
  transition: all 0.5s ease;
}

.content-transition-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.content-transition-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 拖拽调整区域宽度的分隔线 */
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
.interaction-panel {
  display: flex;
  flex-direction: column;
  background-color: #f9f9f9;
  transition: width 0.3s ease;
  position: relative;
  overflow: hidden;
}

/* 右侧面板收起状态 */
.panel-collapsed {
  overflow: visible !important;
}

/* 展开按钮 */
.expand-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.expand-btn:hover {
  background-color: #4F46E5;
  transform: translate(-50%, -50%) scale(1.1);
}

/* 收起按钮 */
.collapse-btn {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translate(-50%, -50%);
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
  z-index: 20;
}

.collapse-btn:hover {
  background-color: #4F46E5;
  transform: translate(-50%, -50%) scale(1.1);
}

/* 对话区域头部 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: white;
  border-bottom: 1px solid #eaeaea;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.chat-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: auto;
  max-height: none;
  transition: max-height 0.3s ease;
}

.chat-messages {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
  background-color: #f9f9f9;
  max-height: calc(100vh - 300px); /* 动态计算最大高度 */
}

/* 消息卡片样式 */
.message-card {
  display: flex;
  width: fit-content;
  max-width: 90%;
  opacity: 0;
  animation: fadeIn 0.3s forwards;
  border-radius: 12px;
  overflow: visible; /* 修改为visible，允许内容溢出 */
  height: auto;
}

.user-message {
  align-self: flex-end;
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: white;
  margin-left: auto;
}

.ai-message {
  align-self: flex-start;
  background-color: white;
  color: #333;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  max-width: 95%;
  width: auto;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-avatar {
  padding: 12px 0 12px 12px;
  display: flex;
  align-items: flex-start;
}

.user-message .message-avatar {
  padding: 12px 12px 12px 0;
  order: 2;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
}

.avatar-circle.user {
  background-color: white;
  color: #6366F1;
}

.avatar-circle.ai {
  background: linear-gradient(135deg, #6366F1, #4F46E5);
  color: white;
}

.message-container {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  overflow: visible; /* 确保内容可见 */
}

.message-bubble {
  padding: 0;
  position: relative;
  overflow: visible; /* 确保内容可见 */
}

.message-content {
  line-height: 1.6;
  word-break: break-word;
  font-size: 14px;
  padding: 0 5px;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  max-width: 100%;
  overflow: visible; /* 确保内容可见 */
}

.message-content p {
  margin: 0.5em 0;
}

.message-content ul, 
.message-content ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.message-content pre {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 0.5em;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0.5em 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-width: 100%;
  font-size: 13px; /* 稍微减小代码块字体大小 */
}

.user-message .message-content pre {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-message .message-content {
  color: white;
}

.message-time {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
  align-self: flex-end;
}

/* 输入提示动画 */
.typing-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
}

.typing-indicator span {
  height: 8px;
  width: 8px;
  margin: 0 2px;
  background-color: #6366F1;
  border-radius: 50%;
  display: inline-block;
  opacity: 0.4;
}

.typing-indicator span:nth-child(1) {
  animation: pulse 1s infinite 0s;
}

.typing-indicator span:nth-child(2) {
  animation: pulse 1s infinite 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation: pulse 1s infinite 0.4s;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(1); opacity: 0.4; }
}

/* 提示词区域 */
.prompt-suggestions {
  background-color: white;
  border-top: 1px solid #eaeaea;
  box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.05);
  z-index: 5;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.prompt-header {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  position: relative;
}

.prompt-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex-shrink: 0;
  margin-right: 15px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  flex: 1;
}

.category-tab {
  padding: 6px 12px;
  border-radius: 20px;
  background-color: #f0f0f0;
  color: #555;
  border: none;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.category-tab i {
  font-size: 12px;
}

.category-tab.active {
  background-color: #6366F1;
  color: white;
}

.toggle-btn {
  background: none;
  border: none;
  color: #6366F1;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.toggle-btn:hover {
  background-color: rgba(99, 102, 241, 0.1);
}

/* 提示词卡片区域 */
.prompt-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  padding: 10px 15px;
  overflow-y: auto;
  max-height: 200px;
}

.prompt-card {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid #eaeaea;
}

.prompt-card:hover {
  background-color: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
}

.prompt-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #6366F1;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prompt-info {
  flex: 1;
}

.prompt-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  color: #333;
}

.prompt-desc {
  font-size: 12px;
  color: #666;
}

/* 输入区域 */
.input-area-wrapper {
  padding: 15px;
  background-color: white;
  border-top: 1px solid #eaeaea;
}

.input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-container {
  flex: 1;
  position: relative;
}

.input-area textarea {
  width: 100%;
  min-height: 50px;
  max-height: 150px;
  padding: 12px 15px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background-color: #f9f9f9;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  font-family: inherit;
}

.input-area textarea:focus {
  border-color: #6366F1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.send-btn {
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background-color: #4F46E5;
  transform: scale(1.05);
}

.send-btn:disabled {
  background-color: #c4c4c4;
  cursor: not-allowed;
}

/* 分析按钮 */
.analysis-btn {
  background-color: #6366F1;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.analysis-btn:hover {
  background-color: #4F46E5;
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

/* 滑动动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
}

/* 生成剧本画布 */
.generating-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 生成加载状态 */
.generating-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.generating-icon {
  font-size: 48px;
  color: #10B981;
  margin-bottom: 20px;
}

.generating-loading-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.generating-progress {
  display: flex;
  flex-direction: column;
  gap: 15px;
  color: #666;
}

.generating-progress span {
  display: flex;
  align-items: center;
  gap: 10px;
}

.generating-progress i {
  color: #10B981;
}

/* 剧本分析画布 */
.analysis-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: center;
}

.canvas-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(3px);
}

.canvas-content-wrapper {
  position: relative;
  width: 80%;
  max-width: 1000px;
  height: 80%;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.canvas-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eaeaea;
}

.canvas-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-canvas-btn {
  background: none;
  border: none;
  color: #666;
  font-size: 18px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-canvas-btn:hover {
  background-color: #f0f0f0;
  color: #333;
}

.canvas-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 分析加载状态 */
.analysis-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

.analysis-icon {
  font-size: 48px;
  color: #6366F1;
  margin-bottom: 20px;
}

.analysis-loading-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.analysis-progress {
  display: flex;
  flex-direction: column;
  gap: 15px;
  color: #666;
}

.analysis-progress span {
  display: flex;
  align-items: center;
  gap: 10px;
}

.analysis-progress i {
  color: #6366F1;
}

/* 分析结果 */
.analysis-result {
  line-height: 1.8;
  color: #333;
}

.analysis-result h1,
.analysis-result h2,
.analysis-result h3 {
  color: #4F46E5;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.analysis-result ul,
.analysis-result ol {
  padding-left: 1.5em;
}

.analysis-result blockquote {
  border-left: 4px solid #6366F1;
  padding-left: 1em;
  margin-left: 0;
  color: #555;
}

/* 分析为空状态 */
.analysis-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #888;
}

.analysis-empty .empty-icon {
  color: #ccc;
  margin-bottom: 20px;
}

/* 画布滑动动画 */
.canvas-slide-enter-active,
.canvas-slide-leave-active {
  transition: all 0.3s ease;
}

.canvas-slide-enter-from,
.canvas-slide-leave-to {
  opacity: 0;
  transform: scale(0.9);
}


/* 流式内容相关样式 */
.streaming-content {
  position: relative;
  padding: 10px 0;
}

.streaming-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 15px;
}

.streaming-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.streaming-title h4 {
  margin: 0;
  font-size: 16px;
  color: #4F46E5;
}

.progress-bar {
  height: 6px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4F46E5, #6366F1);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.cursor-blink {
  display: inline-block;
  width: 2px;
  height: 18px;
  background-color: #4F46E5;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 错误状态样式 */
.streaming-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  text-align: center;
  color: #dc2626;
}

.error-icon {
  font-size: 40px;
  margin-bottom: 15px;
}

.retry-btn {
  background-color: #4F46E5;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  margin-top: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.retry-btn:hover {
  background-color: #4338CA;
}

.cancel-btn {
  background-color: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 13px;
  cursor: pointer;
}

.cancel-btn:hover {
  background-color: #e5e7eb;
}

.generate-framework-btn {
  background-color: #4F46E5;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.generate-framework-btn:hover {
  background-color: #4338CA;
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}

.generate-framework-btn:disabled {
  background-color: #c7d2fe;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
</style>