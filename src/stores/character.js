import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { socketState } from './socket';

export const useCharacterStore = defineStore('characterStore', () => {

  // 引入画布store以共享数据
  const canvasStore = useCanvasStore()

  // 生成结点的ID
  const generateNodeId = () => 'node-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

  // 使用computed属性从canvasStore获取角色节点
  const nodes = computed(() => {
    return canvasStore.nodes.filter(node => node.type === 'character')
  })

  // 使用computed属性从canvasStore获取角色关系边
  const edges = computed(() => {
    return canvasStore.edges.filter(edge => edge.type === 'relationship')
  })

  // 获取所有节点（包括非角色节点，用于关系连接）
  const allNodes = computed(() => canvasStore.nodes)
  const allEdges = computed(() => canvasStore.edges)

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

  // 初始化默认角色节点（如果画布中还没有角色节点）
  const initializeDefaultCharacter = () => {
    const hasCharacterNodes = canvasStore.nodes.some(node => node.type === 'character')
    if (!hasCharacterNodes) {
      const defaultCharacter = {
        id: generateNodeId(),
        type: 'character',
        position: { x: 100, y: 100 },
        data: {
          name: '张三',
          avatar: require('@/assets/avatar/1.jpg'),
          age: 28,
          occupation: '律师',
          personality: ['冷静', '理性', '正义感强'],
          background: '毕业于知名法学院，专攻刑事辩护，有着强烈的正义感和敏锐的洞察力。',
          skills: ['法律知识', '逻辑推理', '谈判技巧'],
          items: '一支父亲留下的钢笔，法学院毕业证书',
          notes: '主要推理角色，善于发现细节线索',
          relationships: []
        }
      }
      canvasStore.nodes.push(defaultCharacter)
    }
  }

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

  // 用户在关系选择器中，确认创建角色关系
  const handleEdgeConfirm = (relationData) => {
    if (selectedNodesForEdge.value.length === 2) {
      const [sourceNode, targetNode] = selectedNodesForEdge.value;

      // 创建角色关系边
      const newEdge = reactive({
        id: `relationship-${sourceNode.id}-${targetNode.id}-${Date.now()}`,
        source: sourceNode.id,
        target: targetNode.id,
        sourceHandle: 'right-source',
        targetHandle: 'left',
        type: 'relationship',
        data: reactive({
          type: relationData.type,
          description: relationData.description,
          strength: relationData.strength,
          status: relationData.status,
          label: relationData.type || '关系'
        })
      });

      // 添加到canvasStore中
      canvasStore.edges.push(newEdge);

      // 同时在角色节点中记录关系
      const sourceNodeIndex = canvasStore.nodes.findIndex(n => n.id === sourceNode.id);
      const targetNodeIndex = canvasStore.nodes.findIndex(n => n.id === targetNode.id);
      
      if (sourceNodeIndex !== -1) {
        if (!canvasStore.nodes[sourceNodeIndex].data.relationships) {
          canvasStore.nodes[sourceNodeIndex].data.relationships = [];
        }
        canvasStore.nodes[sourceNodeIndex].data.relationships.push({
          targetId: targetNode.id,
          type: relationData.type,
          description: relationData.description,
          strength: relationData.strength,
          status: relationData.status
        });
      }
      broadcast();
    }

    selectedNodesForEdge.value = [];
    isCreatingEdge.value = false;
    showEdgeSelector.value = false;
  };

  // 用户在关系选择器中修改关系，确认
  const handleEdgeEditConfirm = (relationData) => {
    if (!editingEdgeId.value) return;

    const edge = canvasStore.edges.find(e => e.id === editingEdgeId.value);
    if (edge) {
      // 更新关系边数据
      edge.data.type = relationData.type;
      edge.data.description = relationData.description;
      edge.data.strength = relationData.strength;
      edge.data.status = relationData.status;
      edge.data.label = relationData.type || '关系';

      // 同时更新角色节点中的关系记录
      const sourceNodeIndex = canvasStore.nodes.findIndex(n => n.id === edge.source);
      if (sourceNodeIndex !== -1 && canvasStore.nodes[sourceNodeIndex].data.relationships) {
        const relationIndex = canvasStore.nodes[sourceNodeIndex].data.relationships.findIndex(
          r => r.targetId === edge.target
        );
        if (relationIndex !== -1) {
          canvasStore.nodes[sourceNodeIndex].data.relationships[relationIndex] = {
            targetId: edge.target,
            type: relationData.type,
            description: relationData.description,
            strength: relationData.strength,
            status: relationData.status
          };
        }
      }

      editingEdgeId.value = null;
      showEdgeSelector.value = false;

      // 广播更新
      canvasStore.broadcast && canvasStore.broadcast()
    }
    broadcast();
  };

  // 用户在边选择器中，取消创建边
  const handleEdgeCancel = () => {
    selectedNodesForEdge.value = []
    isCreatingEdge.value = false
    showEdgeSelector.value = false
    editingEdgeId.value = null  // 重置编辑状态
  }

  // 在边选择器中，删除边
  const handleDeleteEdge = () => {
    const index = canvasStore.edges.findIndex(e => e.id === editingEdgeId.value);
    if (index !== -1) {
      canvasStore.edges.splice(index, 1);
    }

    editingEdgeId.value = null;
    showEdgeSelector.value = false;

    broadcast();
  }

  // 连接结点
  const handleConnectNode = (newEdge) => {
    edges.push(newEdge)
    broadcast();
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

    if (isCreatingEdge.value) {
      if (!selectedNodesForEdge.value.find(n => n.id === actualNode.id)) {
        selectedNodesForEdge.value.push(actualNode);
        broadcast();
      }

      if (selectedNodesForEdge.value.length === 2) {
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
      index = canvasStore.nodes.findIndex(n => n.id === updatedData.id);
  
      if (index !== -1) {
        canvasStore.nodes[index].data = {
          ...canvasStore.nodes[index].data,
          ...updatedData.data,
        };
  
        // ✅ 强制触发响应式更新
        canvasStore.nodes[index] = { ...canvasStore.nodes[index] };
  
        console.log('更新后的节点数据：', canvasStore.nodes[index]);
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
  
    broadcast();
    return index;
  };
  
  // 工具栏中添加角色
  const handleAddNode = (event) => {
    const rect = event?.target?.getBoundingClientRect();
    const x = rect ? event.clientX - rect.left : Math.random() * 300 + 100;
    const y = rect ? event.clientY - rect.top : Math.random() * 300 + 100;

    const newNode = {
      id: generateNodeId(),
      type: 'character',
      position: { x, y },
      data: {
        name: '新角色',
        avatar: require('@/assets/avatar/1.jpg'),
        age: null,
        occupation: '',
        personality: [],
        background: '',
        skills: [],
        items: '',
        notes: '',
        relationships: []
      }
    };
    nodes.value.push(newNode);
    broadcast();
  }

  // 结点的删除
  const handleDeleteNode = (nodeId) => {
    const index = canvasStore.nodes.findIndex(n => n.id === nodeId);
    if (index !== -1) {
      canvasStore.nodes.splice(index, 1);

      if (selectedNode.value?.id === nodeId) {
        selectedNode.value = null;
      }

      // 删除相关的边
      for (let i = canvasStore.edges.length - 1; i >= 0; i--) {
        if (canvasStore.edges[i].source === nodeId || canvasStore.edges[i].target === nodeId) {
          canvasStore.edges.splice(i, 1);
        }
      }

      // 广播更新
      canvasStore.broadcast && canvasStore.broadcast()
    }
    broadcast();
  }

  // 处理结点的位置变化
  const handlePositionChange = (payload) => {
    const { id, position } = payload
    console.log('111结点位置变化：', id, position)
    const nodeIndex = nodes.value.findIndex(n => n.id === id)
    if (nodeIndex !== -1) {
      nodes.value[nodeIndex].position = position
      nodes.value[nodeIndex] = { ...nodes.value[nodeIndex] } // ✅ 强制 Vue 感知变化
      // console.log(`[DEBUG] 节点 ${id} 位置已更新为：`, nodes.value);
    }
    broadcast();
  };
    // 广播节点和边的信息
    const broadcast = () => {
      socketState.socket.send(
        JSON.stringify({ type: "character", characterNodes: nodes.value, characterEdges: edges })
      );
      console.log("广播的节点信息：", {
        nodes: nodes.value,
        edges: edges,
      });
    };

  // 初始化默认角色（在store创建时调用）
  initializeDefaultCharacter()

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
    
    // 方法
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
    handlePositionChange,
    initializeDefaultCharacter
  }
})