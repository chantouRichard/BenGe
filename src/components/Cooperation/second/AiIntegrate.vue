<template>
  <teleport to="body">
    <div class="AiIntegrate-overlay" @click="handleOverlayClick">
      <div
        v-if="showAI"
        class="AiIntegrate-panel"
        style="display: flex; justify-content: center; align-items: center"
      >
        <video
          class="loading-video"
          autoplay
          muted
          loop
          playsinline
          src="@/assets/first/book.mp4"
        ></video>
        <h2>AI正在生成完整剧本</h2>
      </div>

      <div v-else class="AiIntegrate-panel" @click.stop>
        <div class="AiIntegrate-header">
          <h3>AI生成完整剧本</h3>
          <button class="close-btn" @click="$emit('close')">×</button>
        </div>

        <div class="AiIntegrate-content">
          <v-md-editor
            v-model="editorContent"
            height="400px"
            :mode="'editable'"
          />
          <div style="text-align: right; margin-top: 16px">
            <button @click="emitContent" class="confirm-btn">确认使用</button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import VMdEditor from "@kangc/v-md-editor";
import "@kangc/v-md-editor/lib/style/base-editor.css";
import githubTheme from "@kangc/v-md-editor/lib/theme/github.js";
import "@kangc/v-md-editor/lib/theme/style/github.css";
import Prism from "prismjs";
import { socketState } from "@/stores/socket";

VMdEditor.use(githubTheme, {
  Prism,
});

const props = defineProps({
  AIcontent: { type: String, default: "# 测试文本" },
});
const emit = defineEmits(["close", "select"]);

const editorContent = ref(props.AIcontent);
const showAI = ref(true);

onMounted(() => {
  AiIntegrate();
});



const handleOverlayClick = () => {
  emit("close");
};

import { enterThirdStage } from "@/api/script";
const emitContent = async () => {
  socketState.CompleteScriptContent = editorContent.value;
  console.log("点击确认：", socketState.CompleteScriptContent);

  await enterThirdStage(socketState.roomId,socketState.CompleteScriptContent);
};

// 产生AI整合的内容
import { collectContextData } from "@/utils/contextCollector";
const contextData = collectContextData();
import { generateCooperateFramework } from "@/api/script";
const AiIntegrate = async () => {
  //   setTimeout(() => {
  //     editorContent.value = `
  // # 剧本标题：迷雾之城

  // ## 故事背景
  // 在一座被常年迷雾笼罩的古老城市中，流传着一段关于“雾中杀人案”的传说。七位毫无交集的人物突然被邀请前往一栋偏远的山庄，在那里他们将揭开一场隐藏已久的真相……

  // ## 登场角色
  // 1. **林默（侦探）**：曾因揭露警方腐败而被开除，现为私人调查员。
  // 2. **江雪（医生）**：性格冷静，擅长心理剖析，似乎知道每个人的秘密。
  // 3. **陆川（作家）**：创作以“真实犯罪”为题材的小说，对这次邀请格外敏感。
  // 4. **白雯（富家女）**：外表高傲，实则内心脆弱，与受害人有不为人知的关系。

  // ## 剧情结构

  // ### 起
  // 1. 七人接到神秘邀请函，前往山庄参加所谓的“剧本体验”。
  // 2. 到达后大雾封山，通讯中断，一具尸体出现在客厅。

  // ### 承
  // 1. 每人收到一张线索卡片，内容彼此矛盾。
  // 2. 众人开始怀疑彼此，逐步揭示出死者生前掌握众人的秘密。
  // 3. 林默逐步还原事件过程，发现这并非第一次杀人事件。

  // ### 转
  // 1. 第二位受害者出现，江雪突然失踪。
  // 2. 陆川的小说竟提前记录了所有线索细节，被众人质疑为凶手。
  // 3. 调查中众人找到一份“山庄日记”，揭示房主十年前被害的真相。

  // ### 合
  // 1. 林默通过重建案发现场，揭开凶手的真实身份。
  // 2. 凶手因十年前之恨而复仇，但最终良知复苏，向警方自首。
  // 3. 大雾散去，众人各自离开，城市再次归于平静。

  // ## 核心主题
  // 1. 信任与欺骗、真相与记忆、命运的轮回。

  // ## 结局提示
  // 本剧本适合沉浸式角色扮演，有多条分支结局，鼓励玩家从不同角色视角探索真相。

  //     `.trim();
  //     showAI.value = false;
  //   }, 3000);
  console.log("contextData:", contextData);
  const data = {
    contextData: JSON.stringify(contextData),
    roomId: socketState.roomId,
  };
  const res = await generateCooperateFramework(data);
  console.log("res:", res);
  showAI.value = false;
};
</script>

<style scoped>
.AiIntegrate-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.AiIntegrate-panel {
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  width: 720px;
  height: 600px;
  margin: 20px;
}

.AiIntegrate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.AiIntegrate-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #95a5a6;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f8f9fa;
  color: #2c3e50;
}

.AiIntegrate-content {
  padding: 24px;

  width: 100%;
  height: 70%;
  background-color: white;
}
.confirm-btn {
  padding: 8px 16px;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.confirm-btn:hover {
  background-color: #1e40af;
}

.loading-video {
  width: 200px; /* 你可以改成 100% 或 cover 效果 */
  height: 200px;
  object-fit: contain;
}

.title {
  font-size: 24px;
  font-weight: bold;
}
</style>
