<template>
  <div class="workspace-container">
    <!-- 悬浮工具球 -->
    <FloatingToolball
      v-if="socketState.userRole == 0"
      @add-node="canvasStore.handleAddNode"
      @add-edge="canvasStore.handleCreateEdgeClick"
      @export="handleExport"
      @ai-generate="handleAiGenerate"
    />
    <CharacterToolbar
      v-if="socketState.userRole == 1"
      @add-character="characterStore.handleAddNode"
      @add-relationship="characterStore.handleCreateEdgeClick"
      @export-characters="handleExport"
      @ai-generate="handleCharacterAiGenerate"
    />
    <ClueToolbar
      v-if="socketState.userRole == 2"
      @add-clue="clueStore.handleAddClueNode"
      @add-inference="clueStore.handleAddInferenceNode"
      @add-person="clueStore.handleAddPersonNode"
      @add-relationship="clueStore.handleCreateEdgeClick"
      @export-clues="handleExport"
      @ai-generate="handleClueAiGenerate"
    />
    <AtmosphereToolbar
      v-if="socketState.userRole == 3"
      @add-node="(e) => { console.log('add-node事件:', e); canvasStore.handleAddNode(e); }"
      @atmo-palette="(e) => { console.log('atmo-palette事件:', e); handleExport(e); }"
      @link-scene="(e) => { console.log('link-scene事件:', e); handleExport(e); }"
      @export-atmo="(e) => { console.log('export-atmo事件:', e); handleExport(e); }"
      @ai-generate="(e) => { console.log('ai-generate事件被接收:', e); handleAtmosphereAiGenerate(e); }"
    />

    <!-- 主画布 -->
    <CanvasArea
      ref="canvasRef"
      v-if="nodes.length > 0"
      :nodes="nodes"
      :edges="edges"
      @delete-node="handleDeleteNode"
      @node-select="handleNodeClick"
      @edge-select="handleEdgeSelect"
      @node-position-change="handleNodePositionChange"
      @connect-node="handleConnectNode"
    />

    <!-- 节点详情抽屉 -->
    <div style="height: 100%; width: 100%">
      <NodeDetailDrawer
        :visible="canvasStore.selectedNode"
        :nodeData="canvasStore.selectedNode"
        @save="handleDetailSave"
        @close="canvasStore.selectedNode = null"
      />

      <EdgeTypeSelector
        v-if="canvasStore.showEdgeSelector"
        :source="canvasStore.selectedNodesForEdge[0]"
        :target="canvasStore.selectedNodesForEdge[1]"
        @confirm="canvasStore.handleEdgeConfirm"
        @cancel="canvasStore.handleEdgeCancel"
      />

      <EdgeTypeSelector
        v-if="canvasStore.showEdgeSelector && canvasStore.editingEdgeId"
        :initialType="
          edges.find((e) => e.id === canvasStore.editingEdgeId)?.data?.type ||
          null
        "
        :initialLabel="
          edges.find((e) => e.id === canvasStore.editingEdgeId)?.data?.label ||
          ''
        "
        :showDelete="true"
        @confirm="canvasStore.handleEdgeEditConfirm"
        @cancel="canvasStore.handleEdgeCancel"
        @delete-edge="canvasStore.handleDeleteEdge"
      />

      // 角色设计师
      <CharacterDetailPanel
        :visible="characterStore.selectedNode"
        :nodeData="characterStore.selectedNode"
        @save="handleDetailSave"
        @close="characterStore.selectedNode = null"
      />

      <!-- 角色关系编辑器 -->
      <CharacterRelationEditor
        v-if="characterStore.showEdgeSelector && !characterStore.editingEdgeId"
        :source="characterStore.selectedNodesForEdge[0]"
        :target="characterStore.selectedNodesForEdge[1]"
        @confirm="characterStore.handleEdgeConfirm"
        @cancel="characterStore.handleEdgeCancel"
      />

      <!-- 编辑现有关系 -->
      <CharacterRelationEditor
        v-if="characterStore.showEdgeSelector && characterStore.editingEdgeId"
        :source="getEdgeSourceNode()"
        :target="getEdgeTargetNode()"
        :initialType="getCurrentEdge()?.data?.type || ''"
        :initialDescription="getCurrentEdge()?.data?.description || ''"
        :initialStrength="getCurrentEdge()?.data?.strength || 5"
        :initialStatus="getCurrentEdge()?.data?.status || 'active'"
        :showDelete="true"
        @confirm="characterStore.handleEdgeEditConfirm"
        @cancel="characterStore.handleEdgeCancel"
        @delete-relation="characterStore.handleDeleteEdge"
      />

      // 线索设计师
      <ClueDetailPanel
        :visible="clueStore.selectedNode?.type == 'clue' || false"
        :clueData="clueStore.selectedNode"
        @save="handleDetailSave"
        @close="clueStore.selectedNode = null"
      />
      <InferenceDetailPanel
        :visible="clueStore.selectedNode?.type == 'inference' || false"
        :nodeData="clueStore.selectedNode"
        @save="handleDetailSave"
        @close="clueStore.selectedNode = null"
      />
      <PersonDetailPanel
        :visible="clueStore.selectedNode?.type == 'person' || false"
        :nodeData="clueStore.selectedNode"
        @save="handleDetailSave"
        @close="clueStore.selectedNode = null"
      />

      <ClueEdgeSelector
        v-if="clueStore.showEdgeSelector"
        :source="clueStore.selectedNodesForEdge[0]"
        :target="clueStore.selectedNodesForEdge[1]"
        @confirm="clueStore.handleEdgeConfirm"
        @cancel="clueStore.handleEdgeCancel"
      />

      <ClueEdgeSelector
        v-if="clueStore.showEdgeSelector && clueStore.editingEdgeId"
        :initialType="
          clueStore.edges.find((e) => e.id === clueStore.editingEdgeId)?.data
            ?.type || null
        "
        :initialLabel="
          clueStore.edges.find((e) => e.id === clueStore.editingEdgeId)?.data
            ?.label || ''
        "
        :showDelete="true"
        @confirm="clueStore.handleEdgeEditConfirm"
        @cancel="clueStore.handleEdgeCancel"
        @delete-edge="clueStore.handleDeleteEdge"
      />

      // 氛围设计师
      <AtmosphereDetailPanel
        :visible="atmosphereStore.selectedNode"
        :nodeData="atmosphereStore.selectedNode"
        @save="handleDetailSave"
        @close="atmosphereStore.selectedNode = null"
      />
      <!-- 氛围调色板 -->
      <AtmospherePalette
        :visible="showPalette"
        @close="showPalette = false"
        @select="handleAtmosphereSelect"
      />

      <!-- 简单的状态指示 -->
      <div v-if="atmosphereStore.isLinkingMode" class="linking-status">
        <span v-if="!atmosphereStore.selectedAtmosphereNode"
          >🔗 点击氛围节点</span
        >
        <span v-else>🎯 点击场景节点建立关联</span>
      </div>
    </div>

    <!-- AI生成对话框 -->
    <AIGenerateDialog
      :visible="showAIDialog"
      :designer-type="currentDesignerType"
      :context-data="aiContextData"
      :generate-result="aiGenerateResult"
      :generate-error="aiGenerateError"
      @cancel="handleAIDialogCancel"
      @generate="handleAIDialogGenerate"
      @close="handleAIDialogClose"
    />
  </div>
</template>

<script setup>
import FloatingToolball from "./NarrativeCom/FloatingToolball.vue";
import CanvasArea from "./NarrativeCom/CanvasArea.vue";
import NodeDetailDrawer from "./NarrativeCom/NodeDetailDrawer.vue";
import EdgeTypeSelector from "./NarrativeCom/EdgeTypeSelector.vue";
import CharacterToolbar from "./CharacterCom/CharacterToolbar.vue";
import CharacterDetailPanel from "./CharacterCom/CharacterDetailPanel.vue";
import CharacterRelationEditor from "./CharacterCom/CharacterRelationEditor.vue";
//线索设计师组件导入
import ClueToolbar from "./ClueCom/ClueToolbar.vue";
import ClueDetailPanel from "./ClueCom/ClueDetailPanel.vue";
import InferenceDetailPanel from "./ClueCom/InferenceDetailPanel.vue";
import PersonDetailPanel from "./ClueCom/PersonDetailPanel.vue";
//氛围设计师组件导入
import AtmosphereToolbar from "./AtmosphereCom/AtmosphereToolbar.vue";
import AtmosphereDetailPanel from "./AtmosphereCom/AtmosphereDetailPanel.vue";
import AtmospherePalette from "./AtmosphereCom/AtmospherePalette.vue";
import AIGenerateDialog from './AIGenerateDialog.vue'

import ClueEdgeSelector from "./ClueCom/ClueEdgeSelector.vue";
import { useCharacterStore } from "@/stores/character";
import { useCanvasStore } from "@/stores/canvasStore";
import { useClueStore } from "@/stores/clue";
import { useAtmosphereStore } from "@/stores/atmosphere";
import { ref, computed } from "vue";

// 传入参数
import { defineProps } from 'vue'
import { socketState } from '@/stores/socket'
import  request  from '@/api/request';

// const effectiveNodes = computed(() => props.nodes || canvasStore.nodes)
// const effectiveEdges = computed(() => props.edges || canvasStore.edges)


const canvasStore = useCanvasStore();
const characterStore = useCharacterStore();
const clueStore = useClueStore();
const atmosphereStore = useAtmosphereStore();
const canvasRef = ref(null);

// AI对话框相关状态
const showAIDialog = ref(false)
const currentDesignerType = ref('narrative')
const aiGenerateResult = ref(null)
const aiGenerateError = ref(null)
const aiContextData = computed(() => {
  return {
    chatCount: socketState.messages?.length || 0,
    nodeCount: nodes.value?.length || 0,
    characterCount: characterStore.nodes?.length || 0,
    recentNodes: nodes.value?.slice(-5) || []
  }
})

// 导出图片
const handleExport = () => {
      if (canvasRef.value && canvasRef.value?.exportCanvas) {
        // 调用CanvasArea组件暴露的导出方法
        canvasRef.value?.exportCanvas();
      } else {
        console.warn('canvasRef or export method not found');
      }
    };

// 直接使用 store 中的数据驱动画布
const nodes = computed(() => [
  ...(canvasStore.nodes || []),
  ...(characterStore.nodes || []),
  ...(clueStore.nodes||[]),
  ...(atmosphereStore.nodes||[]),

]);
const edges = computed(() => [
  ...(canvasStore.edges || []),
  ...(characterStore.edges || []),
  ...(clueStore.edges||[]),
  ...(atmosphereStore.edges||[]),
]);

// const edges = computed(() => canvasStore.edges)
const handleDetailSave = (updatedData) => {
  console.log("当前节点数据", updatedData);

  const index = nodes.value.findIndex((node) => node.id === updatedData.id);
  if (index === -1) {
    console.warn("未找到对应节点", updatedData.id);
    return;
  }

  const targetNode = nodes.value[index];
  let newIndex = -1;

  if (targetNode.type === "custom") {
    newIndex = canvasStore.handleDetailSave(updatedData);
  } else if (targetNode.type === "character") {
    newIndex = characterStore.handleDetailSave(updatedData);
  } else if(targetNode.type === "clue"||targetNode.type === "inference"||targetNode.type === "person"){
    newIndex = clueStore.handleDetailSave(updatedData);
  } else {
    newIndex = atmosphereStore.handleDetailSave(updatedData);
  }

  console.log("接受到的索引", newIndex);
  canvasRef.value?.forceUpdateNode(updatedData.id, nodes.value[index].data);
};

const handleDeleteNode = (id) => {
  const node = nodes.value.find((n) => n.id === id);
  if (!node) return;

  if (node.type === "custom") {
    canvasStore.handleDeleteNode(id);
  } else if (node.type === "character") {
    characterStore.handleDeleteNode(id);
  } else if (node.type === "atmosphere") {
    atmosphereStore.handleDeleteNode(id);
  } else {
    clueStore.handleDeleteNode(id);
  }
};

const handleNodeClick = (data) => {
  if (data.node.type === "custom") {
    canvasStore.handleNodeClick(data);
  } else if (data.node.type === "character") {
    characterStore.handleNodeClick(data);
  } else if (data.node.type === "atmosphere") {
    atmosphereStore.handleNodeClick(data);
  } else {
    clueStore.handleNodeClick(data);
  }
};
const handleEdgeSelect = (edge) => {
  if (data.node.type === "custom") {
    canvasStore.handleEdgeSelect(edge);
  } else if (data.node.type === "character") {
    characterStore.handleEdgeSelect(edge);
  } else if (data.node.type === "atmosphere") {
    atmosphereStore.handleEdgeSelect(edge);
  } else {
    clueStore.handleEdgeSelect(edge);
  }
};
const handleNodePositionChange = (payload) => {
  const { id, position } = payload;
  const node = nodes.value.find((n) => n.id === id);
  if (!node) return;

  if (node.type === "custom") {
    canvasStore.handlePositionChange(payload);
  } else if (node.type === "character") {
    characterStore.handlePositionChange(payload);
  } else if (node.type === "atmosphere") {
    atmosphereStore.handlePositionChange(payload);
  } else {
    clueStore.handlePositionChange(payload);
  }
};
const handleConnectNode = (connection) => {
  // connection: { source, target, sourceHandle, targetHandle }
  // 统一交给 canvasStore 处理边
  canvasStore.handleConnectNode(connection);
};

// 处理AI生成场景
const handleAiGenerate = async () => {
  currentDesignerType.value = 'narrative'
  showAIDialog.value = true
}

// AI对话框处理方法
const handleAIDialogCancel = () => {
  showAIDialog.value = false
  aiGenerateResult.value = null
  aiGenerateError.value = null
}

const handleAIDialogClose = () => {
  showAIDialog.value = false
  aiGenerateResult.value = null
  aiGenerateError.value = null
}

const handleAIDialogGenerate = async ({ userInput, template }) => {
  // 清除之前的结果
  aiGenerateResult.value = null
  aiGenerateError.value = null

  try {
    // 收集当前上下文
    const contextData = {
      chat: socketState.messages.map(msg => ({
        user: msg.username || msg.sender,
        text: msg.content,
        time: msg.time
      })),
      canvasNodes: canvasStore.nodes.map(node => ({
        type: node.type,
        title: node.data?.title || '未命名',
        data: JSON.stringify(node.data).substring(0, 200)
      })),
      characterNodes: characterStore.nodes.map(node => ({
        name: node.data?.name || '未命名角色',
        occupation: node.data?.occupation || ''
      }))
    }

    const result = await request.post('/ai/generate-nodes', {
      userInput: userInput,
      designerType: currentDesignerType.value,
      contextData: JSON.stringify(contextData)
    })

    if (result.success && result.nodes) {
      // 根据设计师类型添加节点到相应的store
      result.nodes.forEach((nodeData, index) => {
        const newNode = {
          id: `ai-${currentDesignerType.value}-${Date.now()}-${index}`,
          type: getNodeTypeByDesigner(currentDesignerType.value),
          position: {
            x: 400 + index * 250,
            y: 300 + index * 120
          },
          data: nodeData
        }

        if (currentDesignerType.value === 'character') {
          characterStore.nodes.push(newNode)
          characterStore.broadcast && characterStore.broadcast()
        } else {
          canvasStore.nodes.push(newNode)
          canvasStore.broadcast && canvasStore.broadcast()
        }
      })

      // 设置成功结果，让对话框显示
      aiGenerateResult.value = {
        success: true,
        message: `成功生成${result.nodes.length}个${getDesignerName(currentDesignerType.value)}节点！`,
        nodeCount: result.nodes.length
      }
    } else {
      // 设置失败结果
      aiGenerateError.value = '生成失败：' + (result.message || '未知错误')
    }
  } catch (error) {
    console.error('AI生成失败:', error)
    // 设置错误结果
    aiGenerateError.value = '生成失败，请检查网络连接'
  }
}

// 辅助方法
const getNodeTypeByDesigner = (designerType) => {
  const typeMap = {
    narrative: 'custom',
    character: 'character',
    clue: 'clue',
    atmosphere: 'atmosphere'
  }
  return typeMap[designerType] || 'custom'
}

const getDesignerName = (designerType) => {
  const nameMap = {
    narrative: '场景',
    character: '角色',
    clue: '线索',
    atmosphere: '氛围'
  }
  return nameMap[designerType] || '节点'
}

// 处理导出功能


// 处理人物设计师AI生成
const handleCharacterAiGenerate = async () => {
  currentDesignerType.value = 'character'
  showAIDialog.value = true
}

// 处理线索设计师AI生成
const handleClueAiGenerate = async () => {
  currentDesignerType.value = 'clue'
  showAIDialog.value = true
}

// 处理氛围设计师AI生成
const handleAtmosphereAiGenerate = async () => {
  console.log('handleAtmosphereAiGenerate 函数被调用了！');
  currentDesignerType.value = 'atmosphere'
  showAIDialog.value = true
}

</script>

<style scoped>
.workspace-container {
  position: relative;
  width: 100%;
  height: 100%;
  max-height: 960px;
  overflow: hidden;

  padding: 40px;
  border-radius: 50px;
}

/* 简单的关联状态指示 */
.linking-status {
  position: fixed;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  z-index: 100;
  backdrop-filter: blur(4px);
}
</style>
