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
      @export="handleExport"
    />
    <FloatingToolball
      v-if="socketState.userRole == 2"
      @add-node="canvasStore.handleAddNode"
      @add-edge="canvasStore.handleCreateEdgeClick"
      @export="handleExport"
    />
    <FloatingToolball
      v-if="socketState.userRole == 3"
      @add-node="canvasStore.handleAddNode"
      @add-edge="canvasStore.handleCreateEdgeClick"
      @export="handleExport"
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
          edges.find((e) => e.id === canvasStore.editingEdgeId)
            ?.data?.type || null
        "
        :initialLabel="
          edges.find((e) => e.id === canvasStore.editingEdgeId)
            ?.data?.label || ''
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
    </div>
  </div>
</template>

<script setup>
import FloatingToolball from './NarrativeCom/FloatingToolball.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import NodeDetailDrawer from './NarrativeCom/NodeDetailDrawer.vue'
import EdgeTypeSelector from './NarrativeCom/EdgeTypeSelector.vue'
import CharacterToolbar from './CharacterCom/CharacterToolbar.vue'
import CharacterDetailPanel from './CharacterCom/CharacterDetailPanel.vue'
import CharacterRelationEditor from './CharacterCom/CharacterRelationEditor.vue'
import { useCharacterStore } from '@/stores/character'
import { useCanvasStore } from '@/stores/canvasStore'
import { ref , computed } from 'vue'

// 传入参数
import { defineProps } from 'vue'
import { socketState } from '@/stores/socket'

// const effectiveNodes = computed(() => props.nodes || canvasStore.nodes)
// const effectiveEdges = computed(() => props.edges || canvasStore.edges)


const canvasStore = useCanvasStore();
const characterStore = useCharacterStore();
const canvasRef = ref(null)

// // 直接使用 store 中的数据驱动画布
const nodes = computed(() => [
  ...(canvasStore.nodes || []),
  ...(characterStore.nodes || []),
]);
const edges = computed(()=>[
  ...(canvasStore.edges || []),
  ...(characterStore.edges || [])
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
  }
};


const handleNodeClick = (data) => {
  if (data.node.type === "custom") {
    canvasStore.handleNodeClick(data);
  } else if (data.node.type === "character") {
    characterStore.handleNodeClick(data);
  }
};
const handleEdgeSelect = (edge) => {
  if (data.node.type === "custom") {
    canvasStore.handleEdgeSelect(edge);
  } else if (data.node.type === "character") {
    characterStore.handleEdgeSelect(edge);
  }
};
const handleNodePositionChange = (payload) => {
    const { id, position } = payload;
  console.log("位置变化了：",nodes.value);
  const node = nodes.value.find((n) => n.id === id);
  if (!node) return;
  console.log("位置：",node.type);


  if (node.type === "custom") {
    canvasStore.handlePositionChange(payload);
  } else if (node.type === "character") {
    characterStore.handlePositionChange(payload);
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
  overflow: hidden;
}
</style>
