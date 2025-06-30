<template>
  <div class="workspace-container">
    <!-- 悬浮工具球 -->
    <FloatingToolball @add-node="canvasStore.handleAddNode" @add-edge="canvasStore.handleCreateEdgeClick" @export="handleExport" />

    <!-- 主画布 -->
    <CanvasArea ref="canvasRef" v-if="canvasStore.nodes.length > 0" :nodes="canvasStore.nodes" :edges="canvasStore.edges" @delete-node="canvasStore.handleDeleteNode"
      @node-select="canvasStore.handleNodeClick" @edge-select="canvasStore.handleEdgeSelect"
      @node-position-change="canvasStore.handlePositionChange" @connect-node="canvasStore.handleConnectNode" />

    <!-- 节点详情抽屉 -->
    <div style="height: 100%;width: 100%;">
      <NodeDetailDrawer :visible="canvasStore.selectedNode" :nodeData="canvasStore.selectedNode" @save="handleDetailSave"
        @close="canvasStore.selectedNode = null" />

      <EdgeTypeSelector v-if="canvasStore.showEdgeSelector" :source="canvasStore.selectedNodesForEdge[0]" :target="canvasStore.selectedNodesForEdge[1]"
        @confirm="canvasStore.handleEdgeConfirm" @cancel="canvasStore.handleEdgeCancel" />

      <EdgeTypeSelector v-if="canvasStore.showEdgeSelector && canvasStore.editingEdgeId"
        :initialType="canvasStore.edges.find(e => e.id === canvasStore.editingEdgeId)?.data?.type || null"
        :initialLabel="canvasStore.edges.find(e => e.id === canvasStore.editingEdgeId)?.data?.label || ''"
        :showDelete="true"
        @confirm="canvasStore.handleEdgeEditConfirm"
        @cancel="canvasStore.handleEdgeCancel" @delete-edge="canvasStore.handleDeleteEdge" />
    </div>
  </div>
</template>

<script setup>
import FloatingToolball from './NarrativeCom/FloatingToolball.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import NodeDetailDrawer from './NarrativeCom/NodeDetailDrawer.vue'
import EdgeTypeSelector from './NarrativeCom/EdgeTypeSelector.vue'
import { useCanvasStore } from '@/stores/canvasStore'
import { ref , computed } from 'vue'

// 传入参数
import { defineProps } from 'vue'

// const effectiveNodes = computed(() => props.nodes || canvasStore.nodes)
// const effectiveEdges = computed(() => props.edges || canvasStore.edges)


const canvasStore = useCanvasStore()
const canvasRef = ref(null)

// // 直接使用 store 中的数据驱动画布
const nodes = computed(() => canvasStore.nodes)
// const edges = computed(() => canvasStore.edges)
const handleDetailSave = (updatedData) => {
  const index = canvasStore.handleDetailSave(updatedData);
  console.log("接受到的索引",index);
  console.log("当前节点数据",nodes.value)
  canvasRef.value?.forceUpdateNode(updatedData.id, nodes.value[index].data);
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