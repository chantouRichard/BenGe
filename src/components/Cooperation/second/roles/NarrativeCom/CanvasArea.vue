<template>
  <div class="canvas-container">
    <VueFlow
      class="vue-flow"
      :nodes="props.nodes"
      :edges="props.edges"
      :node-types="nodeTypes"
      :edge-types="edgeTypes"
      :connectable="true"
      :snap-to-grid="true"
      :snap-grid="[15, 15]"
      :fit-view="true"
      @node-click="handleNodeClick"
      @edge-click="handleEdgeClick"
      @node-drag="handleNodeDrag"
      @node-drag-stop="handleNodeDragStop"
      @connect="handleConnect"
      @edge-update="handleEdgeUpdate"
    >
      <!-- 自定义结点 -->
      <template #node-custom="{ id, type, data, position }">
        <CustomNode
          :key="id"
          :id="id"
          :type="type"
          :data="data"
          :position="position"
          @delete="handleDeleteNode"
        />
      </template>

      <!-- 角色卡片节点 -->
      <template #node-character="{ id, type, data, position }">
        <CharacterCard
          :key="id"
          :id="id"
          :type="type"
          :data="data"
          :position="position"
          @delete="handleDeleteNode"
        />
      </template>

      <!-- 自定义边 -->
      <template #edge-custom="edgeProps">
        <CustomEdge v-bind="edgeProps"/>
      </template>

      <!-- 角色关系边 -->
      <template #edge-relationship="edgeProps">
        <CharacterRelationEdge v-bind="edgeProps"/>
      </template>
    </VueFlow>
  </div>
</template>

<script setup>
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { defineProps, defineEmits, defineExpose, markRaw } from 'vue'
import CustomNode from './CustomNode.vue'
import CustomEdge from './CustomEdge.vue'
import CharacterCard from '../CharacterCom/CharacterCard.vue'
import CharacterRelationEdge from '../CharacterCom/CharacterRelationEdge.vue'

const props = defineProps({
  nodes: {
    type: Array,
    required: true,
    validator: nodes => {
      console.log('CanvasArea - 验证节点数据:', nodes)
      return nodes.every(n => {
        console.log('节点:', n.id, '类型:', n.type, '位置:', n.position)
        return n.position && typeof n.position.x === 'number'
      })
    }
  },
  edges: {
    type: Array,
    default: () => []
  }
})

// // 监听数据变化
// watch(
//   () => props.nodes.map(node => node.data?.title),
//   (newTitles) => {
//     // console.log('节点标题变化:', newTitles);
//   },
//   { deep: false }
// )

// watch(() => props.nodes, (newVal, oldVal) => {
//   // console.log('[DEBUG] nodes发生变化')
//   console.log('新节点列表：', newVal)
//   console.log('旧节点列表：', oldVal)
// }, { deep: true })

const emit = defineEmits(['node-select', "edge-select" , 'delete-node' , 'node-position-change', "connect-node"])

// 处理节点事件
const handleNodeClick = (node) => {
  emit('node-select', node)
}
// 处理边点击修改
const handleEdgeClick = (e) => {
  // 查看具体的数据格式
  console.log("选择的边", e)
  emit("edge-select", e.edge.id)
}
// 删除结点
const handleDeleteNode = (nodeId) => {
  emit('delete-node', nodeId)
}
// 实现结点拖拽
const handleNodeDrag = (node) => {
  const payload = { id: node.node.id, position: node.node.position };
  // console.log('emit payload:', payload)
  emit('node-position-change', payload)
}

const handleNodeDragStop = (node) => {
  // console.log('拖拽停止:', node); // 调试日志
  if (!node?.node.position) return

  const payload = { id: node.node.id, position: node.node.position };
  emit('node-position-change', payload)
}

const nodeTypes = {
  custom: CustomNode,
  character: CharacterCard
}

console.log('CanvasArea - 注册的节点类型:', nodeTypes)

const edgeTypes = {
  custom: markRaw(CustomEdge),
  relationship: markRaw(CharacterRelationEdge)
}

// 强制刷新结点
const { updateNode } = useVueFlow();
const { updateEdge } = useVueFlow();
const forceUpdateNode = (id, newData) => {
  console.log('forceUpdateNode', id, newData)
  updateNode(id, {
    data: { ...newData },
  })
}
const forceUpdateEdge = (id, newData) => {
  console.log('forceUpdateEdge', id, newData)
  // 创建新对象触发响应式更新
  // const updatedEdge = {
  //   id,
  //   data: {
  //     ...edge.data,
  //     ...newData
  //   }
  // };

  // // 使用 VueFlow 的 updateEdge 方法
  // updateEdge(updatedEdge, true); // true 表示完全替换边对象
  updateEdge(id, {
    data: { ...newData}
  })
};

defineExpose({
  forceUpdateNode,
  forceUpdateEdge
})

// 边连接完成事件（拖动新边）
const handleConnect = (params) => {
  console.log(params);
  const newEdge = {
    id: `edge-${params.source}-${params.target}-${Date.now()}`,
    source: params.source,
    target: params.target,
    sourceHandle: params.sourceHandle, // 指定起点 handle
    targetHandle: params.targetHandle, // 指定终点 handle
    type: 'custom',
    data: {
      type: 'dependency',
      label: '新边'
    }
  }
  emit('connect-node', newEdge)
}

// 边位置更新事件（编辑边连接点）
const handleEdgeUpdate = ({ edge, connection }) => {
  edge.source = connection.source
  edge.target = connection.target
  edge.sourceHandle = connection.sourceHandle
  edge.targetHandle = connection.targetHandle
}

// 实时获取结点坐标，提供给 CustomEdge 用于渲染
// const getEdgeCoordinates = (sourceId, targetId) => {
//   const sourceNode = props.nodes.find(n => n.id === sourceId)
//   const targetNode = props.nodes.find(n => n.id === targetId)
//   if (!sourceNode || !targetNode) return {}

//   const { x: sourceX, y: sourceY } = sourceNode.position
//   const { x: targetX, y: targetY } = targetNode.position

//   return {
//     sourceX: sourceX + 100, // 默认节点宽度中心偏移
//     sourceY: sourceY + 40,
//     targetX: targetX + 100,
//     targetY: targetY + 40
//   }
// }
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
  /* background: transparent; */
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

@import '../../../../../../node_modules/@vue-flow/core/dist/style.css';
@import '../../../../../../node_modules/@vue-flow/core/dist/theme-default.css';
</style>