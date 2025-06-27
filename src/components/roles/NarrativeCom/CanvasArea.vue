<template>
  <div class="canvas-container">
    <VueFlow 
      class="vue-flow" 
      :nodes="props.nodes" 
      :edges="props.edges" 
      :node-types="nodeTypes"
      :snap-to-grid="true"
      :snap-grid="[15, 15]"
      @node-click="handleNodeClick"
      @node-drag="handleNodeDrag"
      @node-drag-stop="handleNodeDragStop"
      fit-view
    >
      <template #node-custom="{ id, type, data, position, selected }">
        <CustomNode 
          :id="id" 
          :type="type" 
          :data="{ ...data, selected }" 
          :position="position"
          @click="handleNodeClick"
          @delete="handleDeleteNode"
        />
      </template>
    </VueFlow>
  </div>
</template>

<script setup>
import { VueFlow } from '@vue-flow/core'
import { defineProps, defineEmits } from 'vue'
import CustomNode from './CustomNode.vue'

const props = defineProps({
  nodes: {
    type: Array,
    required: true,
    validator: nodes => nodes.every(n => n.position && typeof n.position.x === 'number')
  },
  edges: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['node-select', 'delete-node', 'node-position-change'])

// 处理节点事件
const handleNodeClick = (node) => {
  emit('node-select', node)
}

const handleDeleteNode = (nodeId) => {
  emit('delete-node', nodeId)
}

const handleNodeDrag = (node) => {
  emit('node-position-change', {
    id: node.id,
    position: node.position,
    isDragging: true // 标记拖动中状态
  })
}

const handleNodeDragStop = (node) => {
  if (!node?.position) return
  
  emit('node-position-change', {
    id: node.id,
    position: {
      x: Math.round(node.position.x / 15) * 15, // 对齐网格
      y: Math.round(node.position.y / 15) * 15
    },
    isDragging: false
  })
}

const nodeTypes = {
  custom: CustomNode
}
</script>

<style>
.canvas-container {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

.vue-flow {
  background: repeating-linear-gradient(0deg, #f7f7f7, #f7f7f7 24px, #e2e2e2 25px);
}

/* 覆盖VueFlow默认节点样式 */
.vue-flow__node {
  position: absolute !important;
  transform: none !important;
  width: auto !important;
  height: auto !important;
}

/* 确保自定义节点不受干扰 */
.custom-node-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
</style>