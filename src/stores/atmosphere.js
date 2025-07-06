import { defineStore } from 'pinia'
import { reactive, ref, computed } from 'vue'
import { useCanvasStore } from './canvasStore'
import { debounce } from "lodash";

export const useAtmosphereStore = defineStore('atmosphereStore', () => {

  // 引入画布store以共享数据
  const canvasStore = useCanvasStore()

  // 生成结点的ID
  const generateNodeId = () => 'atmo-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

  // 使用computed属性从canvasStore获取氛围节点
  const nodes = computed(() => {
    return canvasStore.nodes.filter(node => node.type === 'atmosphere')
  })

  // 使用computed属性从canvasStore获取氛围相关边
  const edges = computed(() => {
    return canvasStore.edges.filter(edge => {
      // 获取与氛围节点相关的边
      const sourceNode = canvasStore.nodes.find(n => n.id === edge.source)
      const targetNode = canvasStore.nodes.find(n => n.id === edge.target)
      return sourceNode?.type === 'atmosphere' || targetNode?.type === 'atmosphere'
    })
  })

  // 所有节点和边的引用（用于画布显示）
  const allNodes = computed(() => canvasStore.nodes)
  const allEdges = computed(() => canvasStore.edges)

  // 当前选择结点
  const selectedNode = ref(null);

  // 当前选择边的ID
  const editingEdgeId = ref(null);

  // 是否在创建边
  const isCreatingEdge = ref(false);

  // 通过工具栏创建边时，存放的两个结点
  const selectedNodesForEdge = ref([]);

  // 是否展示边选择器
  const showEdgeSelector = ref(false);

  // 氛围与场景关联模式
  const isLinkingMode = ref(false);
  const selectedAtmosphereNode = ref(null);

  // 开启氛围与场景关联模式
  const handleLinkSceneClick = () => {
    console.log("开启氛围与场景关联模式");
    isLinkingMode.value = true;
    selectedAtmosphereNode.value = null;
    // 关闭其他模式
    isCreatingEdge.value = false;
    selectedNodesForEdge.value = [];
    showEdgeSelector.value = false;
  };

  // 退出关联模式
  const exitLinkingMode = () => {
    isLinkingMode.value = false;
    selectedAtmosphereNode.value = null;
  };

  // 用户点击创建边按钮时，调用此方法
  const handleCreateEdgeClick = () => {
    console.log("点击创建边按钮");
    isCreatingEdge.value = true;
    selectedNodesForEdge.value = [];
    showEdgeSelector.value = false;
    // 关闭关联模式
    isLinkingMode.value = false;
    selectedAtmosphereNode.value = null;
  };

  // 用户点击边时，进入边选择器
  const handleEdgeSelect = (edgeId) => {
    console.log("点击的边的Id", edgeId);
    editingEdgeId.value = edgeId;
    showEdgeSelector.value = true;
  };

  const handleAddNode = (event) => {
    const rect = event?.target?.getBoundingClientRect()
    const x = rect ? event.clientX - rect.left : 200
    const y = rect ? event.clientY - rect.top : 200

    const newNode = {
      id: generateNodeId(),
      type: 'atmosphere',
      position: { x, y },
      data: {
        title: '氛围节点',
        timeLabel: '',
        mood: '平静', // 给新节点一个默认情绪
        lighting: '',
        music: '',
        weather: '',
        notes: ''
      }
    }

    canvasStore.nodes.push(newNode)


    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 点击结点，进入结点的信息编辑界面或者是在创建边
  const handleNodeClick = (node) => {
    // 添加参数验证，防止 undefined 错误
    if (!node) {
      console.warn('handleNodeClick: node parameter is undefined');
      return;
    }

    const actualNode = node.node || node; // 处理两种可能的情况

    // 验证 actualNode 是否有效
    if (!actualNode || !actualNode.id) {
      console.warn('handleNodeClick: invalid node data', actualNode);
      return;
    }

    // 氛围与场景关联模式
    if (isLinkingMode.value) {
      if (actualNode.type === 'atmosphere') {
        // 选择氛围节点
        selectedAtmosphereNode.value = actualNode;
        console.log('选择氛围节点:', actualNode.data.title);
      } else if (actualNode.type === 'custom' && selectedAtmosphereNode.value) {
        // 选择场景节点（custom类型），创建关联
        console.log('选择场景节点:', actualNode.data.title);
        createAtmosphereLink(selectedAtmosphereNode.value, actualNode);
        exitLinkingMode();
      } else if (selectedAtmosphereNode.value) {
        console.log('只能关联到场景节点，当前选择的是:', actualNode.type);
      }
      return;
    }

    if (isCreatingEdge.value) {
      if (!selectedNodesForEdge.value.find(n => n.id === actualNode.id)) {
        selectedNodesForEdge.value.push(actualNode);
      }

      if (selectedNodesForEdge.value.length === 2) {
        showEdgeSelector.value = true;
      }
    } else {
      selectedNode.value = { ...actualNode };
      console.log('氛围设计师设置选中节点:', selectedNode.value);
    }
  }

  // 创建氛围与场景的关联
  const createAtmosphereLink = (atmosphereNode, sceneNode) => {
    const newEdge = {
      id: `atmo-link-${atmosphereNode.id}-${sceneNode.id}-${Date.now()}`,
      source: atmosphereNode.id,
      target: sceneNode.id,
      type: 'custom', // 使用现有的 custom 边类型
      data: {
        type: 'atmosphere-influence',
        label: `氛围影响`,
        style: 'dashed',
        color: '#ff6b6b'
      }
    }

    canvasStore.edges.push(newEdge)
    console.log('[DEBUG] 创建氛围关联:', newEdge)
    console.log(`氛围节点 "${atmosphereNode.data.title}" 关联到场景节点 "${sceneNode.data.title}"`)

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 修改结点信息的保存
  const handleDetailSave = (updatedData) => {
    console.log('保存的氛围节点数据：', updatedData);

    if (!updatedData || !updatedData.id || !updatedData.data) {
      console.warn('[handleDetailSave] 无效参数：', updatedData);
      return -1;
    }

    let index = -1;

    // 如果当前有选中节点，才尝试查找并更新
    if (selectedNode.value) {
      index = canvasStore.nodes.findIndex(n => n.id === updatedData.id);

      if (index !== -1) {
        canvasStore.nodes[index].data = {
          ...canvasStore.nodes[index].data,
          ...updatedData.data,
        };

        // ✅ 强制触发响应式更新
        canvasStore.nodes[index] = { ...canvasStore.nodes[index] };

        console.log('更新后的氛围节点数据：', canvasStore.nodes[index]);
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

    console.log("[DEBUG] 当前节点列表：", JSON.stringify(canvasStore.nodes, null, 2));
    console.log("索引：", index);

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
    return index;
  };

  // 结点的删除
  const handleDeleteNode = (nodeId) => {
    const index = canvasStore.nodes.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      canvasStore.nodes.splice(index, 1)

      if (selectedNode.value?.id === nodeId) {
        selectedNode.value = null
      }

      // 删除相关的边
      for (let i = canvasStore.edges.length - 1; i >= 0; i--) {
        if (canvasStore.edges[i].source === nodeId || canvasStore.edges[i].target === nodeId) {
          canvasStore.edges.splice(i, 1)
        }
      }
    }
    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 处理结点的位置变化
  const handlePositionChange = (payload) => {
    if (!payload?.id || !payload?.position) return

    const index = canvasStore.nodes.findIndex(n => n.id === payload.id)
    if (index !== -1) {
      canvasStore.nodes[index].position = {
        x: payload.position.x,
        y: payload.position.y
      }

      // 广播更新
      canvasStore.broadcast && canvasStore.broadcast()
    }
  }

  // 边连接完成事件
  const handleConnectNode = (newEdge) => {
    canvasStore.edges.push(newEdge)
    console.log('[DEBUG] 新增边：', newEdge)

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 边确认
  const handleEdgeConfirm = (edgeData) => {
    if (selectedNodesForEdge.value.length === 2) {
      const newEdge = {
        id: `edge-${selectedNodesForEdge.value[0].id}-${selectedNodesForEdge.value[1].id}-${Date.now()}`,
        source: selectedNodesForEdge.value[0].id,
        target: selectedNodesForEdge.value[1].id,
        type: 'custom',
        data: edgeData
      }
      canvasStore.edges.push(newEdge)
      console.log('[DEBUG] 新增边：', newEdge)
    }

    // 重置状态
    isCreatingEdge.value = false
    selectedNodesForEdge.value = []
    showEdgeSelector.value = false

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 边取消
  const handleEdgeCancel = () => {
    isCreatingEdge.value = false
    selectedNodesForEdge.value = []
    showEdgeSelector.value = false
    editingEdgeId.value = null
  }

  // 边编辑确认
  const handleEdgeEditConfirm = (edgeData) => {
    if (editingEdgeId.value) {
      const edgeIndex = canvasStore.edges.findIndex(e => e.id === editingEdgeId.value)
      if (edgeIndex !== -1) {
        canvasStore.edges[edgeIndex].data = {
          ...canvasStore.edges[edgeIndex].data,
          ...edgeData
        }
        console.log('[DEBUG] 更新边：', canvasStore.edges[edgeIndex])
      }
    }

    // 重置状态
    editingEdgeId.value = null
    showEdgeSelector.value = false

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  // 删除边
  const handleDeleteEdge = () => {
    if (editingEdgeId.value) {
      const edgeIndex = canvasStore.edges.findIndex(e => e.id === editingEdgeId.value)
      if (edgeIndex !== -1) {
        canvasStore.edges.splice(edgeIndex, 1)
        console.log('[DEBUG] 删除边：', editingEdgeId.value)
      }
    }

    // 重置状态
    editingEdgeId.value = null
    showEdgeSelector.value = false

    // 广播更新
    canvasStore.broadcast && canvasStore.broadcast()
  }

  return {
    // 数据
    nodes,
    edges,
    allNodes,
    allEdges,
    selectedNode,
    editingEdgeId,
    isCreatingEdge,
    selectedNodesForEdge,
    showEdgeSelector,
    isLinkingMode,
    selectedAtmosphereNode,

    // 方法
    handleCreateEdgeClick,
    handleLinkSceneClick,
    exitLinkingMode,
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