<template>
  <div class="workspace-container">
    <!-- 角色设计师专用工具栏 -->
    <CharacterToolbar 
      @add-character="characterStore.handleAddNode" 
      @add-relationship="characterStore.handleCreateEdgeClick" 
      @character-template="handleCharacterTemplate"
      @export-characters="handleExportCharacters"
    />

    <!-- 主画布 -->
    <CanvasArea
      ref="canvasRef"
      v-if="effectiveNodes.length > 0"
      :nodes="effectiveNodes"
      :edges="effectiveEdges"
      @delete-node="characterStore.handleDeleteNode"
      @node-select="characterStore.handleNodeClick"
      @edge-select="characterStore.handleEdgeSelect"
      @node-position-change="characterStore.handlePositionChange"
      @connect-node="characterStore.handleConnectNode"
    />

    <!-- 角色详情面板 -->
    <div style="height: 100%;width: 100%;">
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
import CharacterToolbar from './CharacterCom/CharacterToolbar.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import CharacterDetailPanel from './CharacterCom/CharacterDetailPanel.vue'
import CharacterRelationEditor from './CharacterCom/CharacterRelationEditor.vue'
import { useCharacterStore } from '@/stores/character'
import { ref , computed, watch } from 'vue'

// 传入参数
// defineProps 是编译宏，不需要导入

const props = defineProps({
  nodes: Array,
  edges: Array,
})

// 先初始化store
const characterStore = useCharacterStore()
const canvasRef = ref(null)

// 监听边数组变化（用于调试时可以取消注释）
// watch(() => characterStore.edges, (newEdges) => {
//   console.log('[DEBUG] CharacterDesign - 边数组变化:', newEdges)
// }, { deep: true })

// 然后定义计算属性
const effectiveNodes = computed(() => {
  return props.nodes || characterStore.nodes
})
const effectiveEdges = computed(() => props.edges || characterStore.edges)

// 直接使用 store 中的数据驱动画布
const nodes = computed(() => characterStore.nodes)

// 获取当前编辑的边
const getCurrentEdge = () => {
  return characterStore.edges.find(e => e.id === characterStore.editingEdgeId)
}

// 获取边的源节点
const getEdgeSourceNode = () => {
  const edge = getCurrentEdge()
  return edge ? characterStore.nodes.find(n => n.id === edge.source) : null
}

// 获取边的目标节点
const getEdgeTargetNode = () => {
  const edge = getCurrentEdge()
  return edge ? characterStore.nodes.find(n => n.id === edge.target) : null
}

// 角色详情保存
const handleDetailSave = (updatedData) => {
  const index = characterStore.handleDetailSave(updatedData);
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