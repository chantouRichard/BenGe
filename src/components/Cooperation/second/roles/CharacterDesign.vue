<template>
  <div class="workspace-container">
    <!-- 角色设计师专用工具栏 -->
    <CharacterToolbar 
      @add-character="canvasStore.handleAddChaNode" 
      @add-relationship="canvasStore.handleCreateChaEdgeClick" 
      @character-template="handleCharacterTemplate"
      @export-characters="handleExportCharacters"
    />

    <!-- 主画布 -->
    <CanvasArea
      ref="canvasRef"
      v-if="canvasStore.nodes.length > 0"
      :nodes="canvasStore.nodes"
      :edges="canvasStore.edges"
      @delete-node="canvasStore.handleDeleteNode"
      @node-select="canvasStore.handleNodeClick"
      @edge-select="canvasStore.handleEdgeSelect"
      @node-position-change="canvasStore.handlePositionChange"
      @connect-node="canvasStore.handleConnectNode"
    />

    <!-- 角色详情面板 -->
    <div style="height: 100%;width: 100%;">
      <CharacterDetailPanel 
        :visible="canvasStore.selectedNode" 
        :nodeData="canvasStore.selectedNode" 
        @save="handleDetailSave"
        @close="canvasStore.selectedNode = null" 
      />

      <!-- 角色关系编辑器 -->
      <CharacterRelationEditor 
        v-if="canvasStore.showEdgeSelector && !canvasStore.editingEdgeId" 
        :source="canvasStore.selectedNodesForEdge[0]" 
        :target="canvasStore.selectedNodesForEdge[1]"
        @confirm="canvasStore.handleEdgeConfirm" 
        @cancel="canvasStore.handleEdgeCancel" 
      />

      <!-- 编辑现有关系 -->
      <CharacterRelationEditor 
        v-if="canvasStore.showEdgeSelector && canvasStore.editingEdgeId"
        :source="getEdgeSourceNode()"
        :target="getEdgeTargetNode()"
        :initialType="getCurrentEdge()?.data?.type || ''"
        :initialDescription="getCurrentEdge()?.data?.description || ''"
        :initialStrength="getCurrentEdge()?.data?.strength || 5"
        :initialStatus="getCurrentEdge()?.data?.status || 'active'"
        :showDelete="true"
        @confirm="canvasStore.handleEdgeEditConfirm"
        @cancel="canvasStore.handleEdgeCancel" 
        @delete-relation="canvasStore.handleDeleteEdge" 
      />
    </div>
  </div>
</template>

<script setup>
import CharacterToolbar from './CharacterCom/CharacterToolbar.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import CharacterDetailPanel from './CharacterCom/CharacterDetailPanel.vue'
import CharacterRelationEditor from './CharacterCom/CharacterRelationEditor.vue'
import { useCanvasStore } from '@/stores/canvasStore'
import { ref , computed, watch } from 'vue'

// 传入参数
// defineProps 是编译宏，不需要导入

const props = defineProps({
  nodes: Array,
  edges: Array,
})

// 先初始化store
const canvasStore = useCanvasStore()
const canvasRef = ref(null)


// 直接使用 store 中的数据驱动画布
const nodes = computed(() => canvasStore.nodes)

// 获取当前编辑的边
const getCurrentEdge = () => {
  return canvasStore.edges.find(e => e.id === canvasStore.editingEdgeId)
}

// 获取边的源节点
const getEdgeSourceNode = () => {
  const edge = getCurrentEdge()
  return edge ? canvasStore.nodes.find(n => n.id === edge.source) : null
}

// 获取边的目标节点
const getEdgeTargetNode = () => {
  const edge = getCurrentEdge()
  return edge ? canvasStore.nodes.find(n => n.id === edge.target) : null
}

// 角色详情保存
const handleDetailSave = (updatedData) => {
  const index = canvasStore.handleDetailSave(updatedData);
  console.log("接受到的索引",index);
  console.log("当前节点数据",nodes.value)
  canvasRef.value?.forceUpdateNode(updatedData.id, nodes.value[index].data);
}

// 角色模板功能
const handleCharacterTemplate = () => {
  console.log('打开角色模板选择')
  // TODO: 实现角色模板功能
}

// 导出角色表功能
const handleExportCharacters = () => {
  console.log('导出角色表')
  // TODO: 实现角色表导出功能
}

</script>

<style scoped>
.workspace-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>