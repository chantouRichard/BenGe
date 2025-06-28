<template>
  <div class="workspace-container">
    <!-- 悬浮工具球 -->
    <FloatingToolball @add-node="handleAddNode" @add-edge="handleCreateEdgeClick" @export="handleExport" />

    <!-- 主画布 -->
    <CanvasArea ref="canvasRef" v-if="nodes.length > 0" :nodes="nodes" :edges="edges" @delete-node="handleDeleteNode"
      @node-select="handleNodeClick" @edge-select="handleEdgeSelect" @node-position-change="handlePositionChange" @connect-node="handleConnectNode" />

    <!-- 节点详情抽屉 -->
     <div style="height: 100%;width: 100%;">
    <NodeDetailDrawer :visible="selectedNode" :nodeData="selectedNode" @save="handleDetailSave"
      @close="selectedNode = null" />

    <EdgeTypeSelector v-if="showEdgeSelector" :source="selectedNodesForEdge[0]" :target="selectedNodesForEdge[1]"
      @confirm="handleEdgeConfirm" @cancel="handleEdgeCancel" />

    <EdgeTypeSelector v-if="showEdgeSelector && editingEdgeId"
      :initialType="edges.find(e => e.id === editingEdgeId)?.data?.type || null"
      :initialLabel="edges.find(e => e.id === editingEdgeId)?.data?.label || ''" @confirm="handleEdgeEditConfirm"
      @cancel="handleEdgeCancel" />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import FloatingToolball from './NarrativeCom/FloatingToolball.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import NodeDetailDrawer from './NarrativeCom/NodeDetailDrawer.vue'
import EdgeTypeSelector from './NarrativeCom/EdgeTypeSelector.vue'

// 使用局部变量管理状态
const nodes = ref([])
const edges = reactive([])
const canvasRef = ref(null)
const selectedNode = ref(null)
const isCreatingEdge = ref(false)
const selectedNodesForEdge = ref([])
const editingEdgeId = ref(null)
const showEdgeSelector = ref(false)

// 用户点击创建边按钮
const handleCreateEdgeClick = () => {
  console.log('点击创建边按钮')
  isCreatingEdge.value = true
  selectedNodesForEdge.value = []
  showEdgeSelector.value = false
}

// 点击边，进入边的修改页面
const handleEdgeSelect = (edgeId) => {
  console.log("点击的边的Id", edgeId)
  editingEdgeId.value = edgeId
  showEdgeSelector.value = true
}

// 创建边最终确认
const handleEdgeConfirm = (edgeType, label) => {
  if (selectedNodesForEdge.value.length === 2) {
    const [sourceNode, targetNode] = selectedNodesForEdge.value;

    const newEdge = {
      id: `edge-${sourceNode.id}-${targetNode.id}-${Date.now()}`,
      source: sourceNode.id,
      target: targetNode.id,
      sourcePosition: 'right',
      targetPosition: 'left',
      type: 'custom',
      data: reactive({
        type: edgeType,
        label: label || ''
      })
    };

    edges.value.push(newEdge);
  }

  selectedNodesForEdge.value = []
  isCreatingEdge.value = false
  showEdgeSelector.value = false
}

// 修改边的确认
const handleEdgeEditConfirm = (edgeType, label) => {
  if (!editingEdgeId.value) return;

  const index = edges.value.findIndex(e => e.id === editingEdgeId.value);
  if (index !== -1) {
    // 创建新对象确保响应式更新
    const updatedEdge = {
      ...edges.value[index],
      data: {
        ...edges.value[index].data,
        type: edgeType,
        label: label || ''
      }
    };
    
    // 正确调用 forceUpdateEdge
    canvasRef.value?.forceUpdateEdge(editingEdgeId.value, updatedEdge.data);
    
    // 更新本地状态
    edges.value[index] = updatedEdge;
    
    editingEdgeId.value = null;
    showEdgeSelector.value = false;
  }
};

// 取消创建边
const handleEdgeCancel = () => {
  selectedNodesForEdge.value = []
  isCreatingEdge.value = false
  showEdgeSelector.value = false
}

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
  const actualNode = node.node || node; // 处理两种可能的情况

  if (isCreatingEdge.value) {
    console.log("当前选择结点", actualNode)
    if (!selectedNodesForEdge.value.find(n => n.id === actualNode.id)) {
      selectedNodesForEdge.value.push(actualNode);
    }

    if (selectedNodesForEdge.value.length === 2) {
      console.log('已选择两个节点：', selectedNodesForEdge.value);
      showEdgeSelector.value = true;
    }
  } else {
    selectedNode.value = { ...actualNode };
  }
}
// 添加结点
const handleAddNode = (event) => {
  const rect = event?.target?.getBoundingClientRect(); // 获取点击位置
  const x = rect ? event.clientX - rect.left : 200; // 相对画布位置
  const y = rect ? event.clientY - rect.top : 200;

  const newNode = {
    id: generateNodeId(),
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
  console.log('[DEBUG] 当前节点列表：', JSON.stringify(nodes.value, null, 2))

}
const handleDeleteNode = (nodeId) => {
  const index = nodes.value.findIndex(n => n.id === nodeId);
  if (index !== -1) {
    nodes.value.splice(index, 1)

    if (selectedNode.value?.id === nodeId) {
      selectedNode.value = null;
    }

    edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId);
  }
  console.log('[DEBUG] 当前节点列表：', JSON.stringify(nodes.value, null, 2))

}
// 结点位置变化
const handlePositionChange = (payload) => {
  const { id, position } = payload
  // console.log('[DEBUG] 结点位置变化：', id, position)
  const nodeIndex = nodes.value.findIndex(n => n.id === id)
  if (nodeIndex !== -1) {
    nodes.value[nodeIndex].position = position
    nodes.value[nodeIndex] = { ...nodes.value[nodeIndex] } // ✅ 强制 Vue 感知变化
    // console.log(`[DEBUG] 节点 ${id} 位置已更新为：`, nodes.value);
  }
}
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

    canvasRef.value?.forceUpdateNode(updatedData.id, nodes.value[index].data);

    nodes.value[index] = { ...nodes.value[index] }
  }

  selectedNode.value = null // 收起面板
  console.log('[DEBUG] 当前节点列表：', JSON.stringify(nodes.value, null, 2))

}

// 连接结点
const handleConnectNode = (newEdge) => {
  console.log('[DEBUG] 连接结点边：', newEdge)
  edges.value.push(newEdge)
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