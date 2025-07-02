<template>
  <div class="workspace-container">
    <!-- 悬浮工具球 -->
    <FloatingToolball
      v-if="socketState.userRole == 0"
      @add-node="canvasStore.handleAddNode"
      @add-edge="canvasStore.handleCreateEdgeClick"
      @export="handleExport"
    />
    <CharacterToolbar
      v-if="socketState.userRole == 1"
      @add-character="characterStore.handleAddNode"
      @add-relationship="characterStore.handleCreateEdgeClick"
      @export-characters="handleExport"
    />
    <ClueToolbar
      v-if="socketState.userRole == 2"
      @add-clue="clueStore.handleAddClueNode"
      @add-inference="clueStore.handleAddInferenceNode"
      @add-person="clueStore.handleAddPersonNode"
      @add-relationship="clueStore.handleCreateEdgeClick"
      @export-clues="handleExport"
    />
    <AtmosphereToolbar
      v-if="socketState.userRole == 3"
      @add-node="atmosphereStore.handleAddNode"
      @add-edge="atmosphereStore.handleCreateEdgeClick"
      @export-atmo="handleExport"
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

import ClueEdgeSelector from "./ClueCom/ClueEdgeSelector.vue";
import { useCharacterStore } from "@/stores/character";
import { useCanvasStore } from "@/stores/canvasStore";
import { useClueStore } from "@/stores/clue";
import { useAtmosphereStore } from "@/stores/atmosphere";
import { ref, computed } from "vue";

// 传入参数
import { defineProps } from "vue";
import { socketState } from "@/stores/socket";

const canvasStore = useCanvasStore();
const characterStore = useCharacterStore();
const clueStore = useClueStore();
const atmosphereStore = useAtmosphereStore();
const canvasRef = ref(null);

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
</script>

<style scoped>
.workspace-container {
  position: relative;
  width: 100%;
  height: 100%;
  max-height: 900px;
  overflow: hidden;

  padding: 40px;
  border-radius: 24px;
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
