import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'

export const useAtmosphereStore = defineStore('atmosphereStore', () => {

  // 生成结点的ID
  const generateNodeId = () => 'node-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

  // 所有节点数据
  const nodes = ref([{
    id: generateNodeId(),
    type: 'custom',
    position: { x: 100, y: 100 },
    data: {
      title: '氛围设计师的节点',
      timeLabel: 'DAY1 09:00',
      characters: '张三',
      clues: '线索A',
      sceneDescription: '会议室',
      nodeConnections: '与节点2相关',
      notes: '注意时间冲突',
    }
  }])

  // 所有连线
  const edges = reactive([])

  // 当前选择结点
  const selectedNode = ref(null)

  // 当前选择边的ID
  const editingEdgeId = ref(null)

  // 是否在创建边 
  const isCreatingEdge = ref(false)

  // 通过工具栏创建边时，存放的两个结点
  const selectedNodesForEdge = ref([])

  // 是否展示边选择器
  const showEdgeSelector = ref(false)

  // 用户点击创建边按钮时，调用此方法
  const handleCreateEdgeClick = () => {
    console.log('点击创建边按钮')
    isCreatingEdge.value = true
    selectedNodesForEdge.value = []
    showEdgeSelector.value = false
  }

  // 用户点击边时，进入边选择器
  const handleEdgeSelect = (edgeId) => {
    console.log("点击的边的Id", edgeId)
    editingEdgeId.value = edgeId
    showEdgeSelector.value = true
  }

  // 用户在边选择器中，确认创建边
  const handleEdgeConfirm = (edgeType, label) => {
    if (selectedNodesForEdge.value.length === 2) {
      const [sourceNode, targetNode] = selectedNodesForEdge.value;

      // 使用 reactive 包裹新边对象
      const newEdge = reactive({
        id: `edge-${sourceNode.id}-${targetNode.id}-${Date.now()}`,
        source: sourceNode.id,
        target: targetNode.id,
        sourcePosition: 'right',
        targetPosition: 'left',
        type: 'custom',
        data: reactive({  // 嵌套的 data 也必须是响应式
          type: edgeType,
          label: label || ''
        })
      });

      edges.push(newEdge); // 直接 push 到 reactive 数组
    }

    selectedNodesForEdge.value = [];
    isCreatingEdge.value = false;
    showEdgeSelector.value = false;
  };

  // 用户在边选择器中修改边，确认
  const handleEdgeEditConfirm = (edgeType, label) => {
    if (!editingEdgeId.value) return;

    const edge = edges.find(e => e.id === editingEdgeId.value);
    if (edge) {
      // 直接修改 reactive 对象的属性
      edge.data.type = edgeType;
      edge.data.label = label || '';

      // 调用 CanvasArea 的更新方法（确保传递最新数据）
      // canvasRef.value?.forceUpdateEdge(editingEdgeId.value, edge.data);

      editingEdgeId.value = null;
      showEdgeSelector.value = false;
    }
  };

  // 用户在边选择器中，取消创建边
  const handleEdgeCancel = () => {
    selectedNodesForEdge.value = []
    isCreatingEdge.value = false
    showEdgeSelector.value = false
  }

  // 在边选择器中，删除边
  const handleDeleteEdge = () => {
    const index = edges.findIndex(e => e.id === editingEdgeId.value);
    if (index !== -1) {
      edges.splice(index, 1);
    }

    // forceUpdateNode(payload.nodeId, payload.nodeData);
    editingEdgeId.value = null;
    showEdgeSelector.value = false;
  }

  // 连接结点
  const handleConnectNode = (newEdge) => {
    console.log('[DEBUG] 连接结点边：', newEdge)
    edges.push(newEdge)
  }

  // 点击结点，进入结点的信息编辑界面或者是在创建边
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

  // 修改结点信息的保存
  const handleDetailSave = (updatedData) => {
    console.log('保存的节点数据：', updatedData);
  
    if (!updatedData || !updatedData.id || !updatedData.data) {
      console.warn('[handleDetailSave] 无效参数：', updatedData);
      return -1;
    }
  
    let index = -1;
  
    // 如果当前有选中节点，才尝试查找并更新
    if (selectedNode.value) {
      index = nodes.value.findIndex(n => n.id === updatedData.id);
  
      if (index !== -1) {
        nodes.value[index].data = {
          ...nodes.value[index].data,
          ...updatedData.data,
        };
  
        // ✅ 强制触发响应式更新
        nodes.value[index] = { ...nodes.value[index] };
  
        console.log('更新后的节点数据：', nodes.value[index]);
      } else {
        console.warn('未找到对应的节点 ID:', updatedData.id);
        return -1;
      }
    } else {
      console.warn('无选中节点，可能是编辑逻辑未正确触发');
      return -1;
    }
  
    // 最后收起面板
    selectedNode.value = null;
  
    console.log('[DEBUG] 当前节点列表：', JSON.stringify(nodes.value, null, 2));
    console.log("索引：", index);
    return index;
  };
  
  

  // 工具栏中添加结点
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

  // 结点的删除
  const handleDeleteNode = (nodeId) => {
    const index = nodes.value.findIndex(n => n.id === nodeId);
    if (index !== -1) {
      nodes.value.splice(index, 1)

      if (selectedNode.value?.id === nodeId) {
        selectedNode.value = null;
      }

      for (let i = edges.length - 1; i >= 0; i--) {
        if (edges[i].source === nodeId || edges[i].target === nodeId) {
          edges.splice(i, 1)
        }
      }
    }
    // console.log('[DEBUG] 当前节点列表：', JSON.stringify(nodes.value, null, 2))
  }

  // 处理结点的位置变化
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

  return {
    nodes,
    edges,
    selectedNode,
    editingEdgeId,
    isCreatingEdge,
    selectedNodesForEdge,
    showEdgeSelector,
    handleCreateEdgeClick,
    handleEdgeSelect,
    handleEdgeConfirm,
    handleEdgeEditConfirm,
    handleEdgeCancel,
    handleDeleteEdge,
    handleConnectNode,
    handleNodeClick,
    handleDetailSave,
    handleAddNode,
    handleDeleteNode,
    handlePositionChange
  }
})