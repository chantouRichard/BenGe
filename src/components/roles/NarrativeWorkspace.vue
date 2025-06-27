<template>
  <div class="workspace-container">
    <!-- 悬浮工具球 -->
    <FloatingToolball @add-node="handleAddNode" @export="handleExport" />

    <!-- 主画布 -->
    <CanvasArea v-if="nodes.length > 0" :nodes="nodes" :edges="edges" @delete-node="handleDeleteNode"
      @node-select="handleNodeClick" @node-position-change="handlePositionChange"/>

    <!-- 节点详情抽屉 -->
    <NodeDetailDrawer v-if="selectedNode" :visible="selectedNode" :nodeData="selectedNode" @save="handleDetailSave"
      @close="selectedNode = null" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FloatingToolball from './NarrativeCom/FloatingToolball.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import NodeDetailDrawer from './NarrativeCom/NodeDetailDrawer.vue'

// 使用局部变量管理状态
const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)

const generateNodeId = () => 'node-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

// 示例初始化数据
const initDemoData = () => {
  nodes.value = [
    {
      id: generateNodeId(),
      type: 'custom',
      position: { x: 100, y: 100 },
      data: {
        title: '测试场景',
        timeLabel: 'DAY1 09:00',
        characters: '张三',
        clues: '线索A',
        sceneDescription: '会议室',
        nodeConnections: '与节点2相关',
        notes: '注意时间冲突',
      }
    }
  ]
}

// 初始化数据
initDemoData()

// 事件处理
const handleNodeClick = (node) => {
  console.log("点击了结点", node);
  selectedNode.value = { ...node };
  console.log("选中了结点", selectedNode);
}
// 添加结点
const handleAddNode = (event) => {
  const rect = event?.target?.getBoundingClientRect(); // 获取点击位置
  const x = rect ? event.clientX - rect.left : 200; // 相对画布位置
  const y = rect ? event.clientY - rect.top : 200;

  const newNode = {
    id: nodes.value.length + 1,
    type: 'custom',
    position: { x, y }, // 动态位置
    data: {
      title: '测试场景',
      timeLabel: '',
      characters: '',
      clues: '',
      sceneDescription: '',
      nodeConnections: '',
      notes: ''
    }
  };
  nodes.value.push(newNode);
}
// 删除结点
const handleDeleteNode = (nodeId) => {
  const index = nodes.value.findIndex(n => n.id === nodeId);
  if (index !== -1) {
    nodes.value.splice(index, 1); // 删除节点
    if (selectedNode.value?.id === nodeId) {
      selectedNode.value = null; // 如果删除的是选中节点，清除选择
    }
    // 更新 edges（如果有依赖）
    edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId);
  }
}
// 处理节点位置变更
const handlePositionChange = ({ id, position }) => {
  const nodeIndex = nodes.value.findIndex(n => n.id === id);
  if (nodeIndex !== -1) {
    nodes.value[nodeIndex].position = position;
  }
};
// 详情保存回调
const handleDetailSave = (updatedData) => {
  // console.log('保存的节点数据：', updatedData)
  if (selectedNode.value) {
    const index = nodes.value.findIndex(n => n.id === updatedData.id)
    // console.log('更新节点数据位置 Index：', index)

    if (index !== -1) {
      nodes.value[index].data = {
        ...nodes.value[index].data,
        ...updatedData.data
      }
      console.log('更新后的节点数据：', nodes.value);
    } else {
      console.warn('未找到节点 id:', updatedData.id)
    }
  }

  selectedNode.value = null // 收起面板
}
</script>

<style scoped>
.workspace-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
</style>