import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import { socketState } from "./socket";
import { debounce } from "lodash";

export const useCharacterStore = defineStore("characterStore", () => {
  // 生成结点的ID
  const generateNodeId = () =>
    "node-" + Date.now() + "-" + Math.floor(Math.random() * 1000);

  // 所有角色节点数据
  const nodes = ref([
    {
      id: "node-shen",
      type: "character",
      position: { x: -200, y: 850 },
      data: {
        name: "沈知遥",
        avatar: require("@/assets/avatar/1.jpg"),
        age: 33,
        occupation: "刑警",
        personality: ["冷静", "坚毅", "隐忍"],
        background: "本地警局骨干，曾与死者关系亲密，却在案发中显得异常沉默。",
        skills: ["刑侦推理", "心理观察", "证据分析"],
        items: "录音笔、死者的旧信件",
        notes: "矛盾的调查者，掌握关键真相却犹豫揭露。",
        relationships: [
          {
            type: "旧爱",
            description: "与死者曾有一段旧情",
            targetId: "林妙音",
            strength: 6,
            status: "inactive",
          },
          {
            type: "搭档",
            description: "与李四有过合作，但理念不合",
            targetId: "李四",
            strength: 4,
            status: "active",
          },
        ],
      },
    },
    {
      id: "node-lin",
      type: "character",
      position: { x: 150, y: 930 },
      data: {
        name: "林妙音",
        avatar: require("@/assets/avatar/2.jpg"),
        age: 38,
        occupation: "度假村老板娘",
        personality: ["精明", "冷静", "控制欲强"],
        background: "掌管镜湖度假村，与死者共同开发地产项目，因利益关系交恶。",
        skills: ["财务管理", "谈判", "人际操控"],
        items: "度假村账本、房地产合同副本",
        notes: "金钱利益牵扯颇深，曾是死者的重要合作者。",
        relationships: [
          {
            type: "旧爱",
            description: "与沈知遥有过一段情",
            targetId: "沈知遥",
            strength: 6,
            status: "inactive",
          },
          {
            type: "投资人",
            description: "资助章沐白诊所初建",
            targetId: "章沐白",
            strength: 5,
            status: "active",
          },
        ],
      },
    },
    {
      id: "node-cheng",
      type: "character",
      position: { x: 900, y: 940 },
      data: {
        name: "程少庭",
        avatar: require("@/assets/avatar/3.jpg"),
        age: 24,
        occupation: "服务生",
        personality: ["敏感", "隐忍", "怨恨深重"],
        background: "死者私生子，为寻找真相隐姓埋名进入度假村工作。",
        skills: ["伪装", "窃听", "情绪洞察"],
        items: "父亲照片、DNA检验证明",
        notes: "情绪激烈，容易失控，但掌握重要亲属信息。",
        relationships: [
          {
            type: "父子",
            description: "私生子，身份被隐瞒",
            targetId: "沈文霖",
            strength: 9,
            status: "conflicted",
          },
          {
            type: "观察者",
            description: "时常暗中跟踪章沐白",
            targetId: "章沐白",
            strength: 4,
            status: "active",
          },
        ],
      },
    },
    {
      id: "node-zhang",
      type: "character",
      position: { x: 570, y: 1140 },
      data: {
        name: "章沐白",
        avatar: require("@/assets/avatar/4.jpg"),
        age: 41,
        occupation: "心理医生",
        personality: ["神秘", "疏离", "冷静"],
        background: "为富人群体提供特殊催眠治疗，与死者有精神操控实验合作。",
        skills: ["心理催眠", "精神诱导", "语言操控"],
        items: "催眠机、患者档案",
        notes: "关键心理线人物，行为动机复杂。",
        relationships: [
          {
            type: "医生-病人",
            description: "为死者进行心理治疗",
            targetId: "沈文霖",
            strength: 7,
            status: "past",
          },
          {
            type: "资助人",
            description: "林妙音出资支持其诊所",
            targetId: "林妙音",
            strength: 5,
            status: "active",
          },
        ],
      },
    },
    
  ]);

  // 所有连线
  const edges = [
    {
      id: "relationship-node-shen-lin",
      source: "node-shen",
      sourceHandle: "right-source",
      target: "node-lin",
      targetHandle: "left",
      type: "relationship",
      data: {
        type: "旧爱",
        label: "旧爱",
        description: "与死者曾有一段旧情",
        strength: "6",
        status: "inactive",
        importance: null,
        participationType: null,
        showLabel: true,
        style: null,
      },
    },
    {
      id: "relationship-node-shen-li",
      source: "node-shen",
      sourceHandle: "right-source",
      target: "node-li",
      targetHandle: "left",
      type: "relationship",
      data: {
        type: "搭档",
        label: "搭档",
        description: "与李四有过合作，但理念不合",
        strength: "4",
        status: "active",
        showLabel: true,
      },
    },
    {
      id: "relationship-node-lin-zhang",
      source: "node-lin",
      sourceHandle: "right-source",
      target: "node-zhang",
      targetHandle: "left",
      type: "relationship",
      data: {
        type: "资助人",
        label: "资助人",
        description: "林妙音资助章沐白的诊所",
        strength: "5",
        status: "active",
        showLabel: true,
      },
    },
    {
      id: "relationship-node-cheng-zhang",
      source: "node-cheng",
      sourceHandle: "right-source",
      target: "node-zhang",
      targetHandle: "left",
      type: "relationship",
      data: {
        type: "观察者",
        label: "观察者",
        description: "时常暗中跟踪章沐白",
        strength: "4",
        status: "active",
        showLabel: true,
      },
    },
    {
      id: "relationship-node-liu-zhang",
      source: "node-liu",
      sourceHandle: "right-source",
      target: "node-zhang",
      targetHandle: "left",
      type: "relationship",
      data: {
        type: "访谈对象",
        label: "访谈对象",
        description: "采访章沐白获取创作灵感",
        strength: "3",
        status: "active",
        showLabel: true,
      },
    },
  ];

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

  // 当前创建的边类型 ('relationship' 或 'character-scene')
  const edgeType = ref("relationship");

  // 用户点击创建边按钮时，调用此方法
  const handleCreateEdgeClick = () => {
    // console.log("点击创建边按钮");
    isCreatingEdge.value = true;
    selectedNodesForEdge.value = [];
    showEdgeSelector.value = false;
  };

  // 用户点击边时，进入边选择器
  const handleEdgeSelect = (edgeId) => {
    // console.log("点击的边的Id", edgeId);
    editingEdgeId.value = edgeId;
    showEdgeSelector.value = true;
  };

  // 用户在关系选择器中，确认创建角色关系
  const handleEdgeConfirm = (relationData) => {
    if (selectedNodesForEdge.value.length === 2) {
      const [sourceNode, targetNode] = selectedNodesForEdge.value;

      // 创建角色关系边
      const newEdge = reactive({
        id: `relationship-${sourceNode.id}-${targetNode.id}-${Date.now()}`,
        source: sourceNode.id,
        target: targetNode.id,
        sourceHandle: "right-source",
        targetHandle: "left",
        type: "relationship",
        data: reactive({
          type: relationData.type,
          description: relationData.description,
          strength: relationData.strength,
          status: relationData.status,
          label: relationData.type || "关系",
        }),
      });

      edges.push(newEdge);

      // 同时在角色节点中记录关系
      const sourceNodeIndex = nodes.value.findIndex(
        (n) => n.id === sourceNode.id
      );
      const targetNodeIndex = nodes.value.findIndex(
        (n) => n.id === targetNode.id
      );

      if (sourceNodeIndex !== -1) {
        if (!nodes.value[sourceNodeIndex].data.relationships) {
          nodes.value[sourceNodeIndex].data.relationships = [];
        }
        nodes.value[sourceNodeIndex].data.relationships.push({
          targetId: targetNode.id,
          type: relationData.type,
          description: relationData.description,
          strength: relationData.strength,
          status: relationData.status,
        });
      }
      broadcast();
    }

    selectedNodesForEdge.value = [];
    isCreatingEdge.value = false;
    showEdgeSelector.value = false;
    edgeType.value = "relationship";
  };

  // 用户在角色-场景编辑器中，确认创建角色-场景关系
  const handleCharacterSceneEdgeConfirm = (sceneData) => {
    if (selectedNodesForEdge.value.length === 2) {
      const [sourceNode, targetNode] = selectedNodesForEdge.value;

      // 创建角色-场景边
      const newEdge = reactive({
        id: `character-scene-${sourceNode.id}-${targetNode.id}-${Date.now()}`,
        source: sourceNode.id,
        target: targetNode.id,
        sourceHandle: "right-source",
        targetHandle: "left",
        type: "character-scene",
        data: reactive({
          participationType: sceneData.participationType,
          importance: sceneData.importance,
          description: sceneData.description,
          label: sceneData.label,
          style: sceneData.style,
          showLabel: true,
        }),
      });

      edges.push(newEdge);
      broadcast();
    }

    selectedNodesForEdge.value = [];
    isCreatingEdge.value = false;
    showEdgeSelector.value = false;
    edgeType.value = "relationship";
  };

  // 用户在角色-场景编辑器中，确认修改角色-场景关系
  const handleCharacterSceneEdgeEditConfirm = (sceneData) => {
    if (!editingEdgeId.value) {
      // console.warn("没有正在编辑的边ID");
      return;
    }

    const edge = edges.find((e) => e.id === editingEdgeId.value);

    if (edge) {
      // 更新边数据
      edge.data = {
        ...edge.data,
        participationType: sceneData.participationType,
        importance: sceneData.importance,
        description: sceneData.description,
        label: sceneData.label,
        style: sceneData.style,
      };

      editingEdgeId.value = null;
      showEdgeSelector.value = false;
      edgeType.value = "relationship";

      broadcast();
    } else {
      // console.error("未找到要编辑的角色-场景边:", editingEdgeId.value);
    }
  };

  // 用户在关系选择器中修改关系，确认
  const handleEdgeEditConfirm = (relationData) => {
    if (!editingEdgeId.value) return;

    const edge = edges.find((e) => e.id === editingEdgeId.value);
    if (edge) {
      // 更新关系边数据
      edge.data.type = relationData.type;
      edge.data.description = relationData.description;
      edge.data.strength = relationData.strength;
      edge.data.status = relationData.status;
      edge.data.label = relationData.type || "关系";

      // 同时更新角色节点中的关系记录
      const sourceNodeIndex = nodes.value.findIndex(
        (n) => n.id === edge.source
      );
      if (
        sourceNodeIndex !== -1 &&
        nodes.value[sourceNodeIndex].data.relationships
      ) {
        const relationIndex = nodes.value[
          sourceNodeIndex
        ].data.relationships.findIndex((r) => r.targetId === edge.target);
        if (relationIndex !== -1) {
          nodes.value[sourceNodeIndex].data.relationships[relationIndex] = {
            targetId: edge.target,
            type: relationData.type,
            description: relationData.description,
            strength: relationData.strength,
            status: relationData.status,
          };
        }
      }

      editingEdgeId.value = null;
      showEdgeSelector.value = false;
    }
    broadcast();
  };

  // 用户在边选择器中，取消创建边
  const handleEdgeCancel = () => {
    selectedNodesForEdge.value = [];
    isCreatingEdge.value = false;
    showEdgeSelector.value = false;
    editingEdgeId.value = null; // 重置编辑状态
    edgeType.value = "relationship"; // 重置边类型
  };

  // 在边选择器中，删除边
  const handleDeleteEdge = () => {
    const index = edges.findIndex((e) => e.id === editingEdgeId.value);
    if (index !== -1) {
      edges.splice(index, 1);
    }

    // forceUpdateNode(payload.nodeId, payload.nodeData);
    editingEdgeId.value = null;
    showEdgeSelector.value = false;

    broadcast();
  };

  // 连接结点
  const handleConnectNode = (newEdge) => {
    edges.push(newEdge);
    broadcast();
  };

  // 点击结点，进入结点的信息编辑界面或者是在创建边
  const handleNodeClick = (node) => {
    const actualNode = node.node || node; // 处理两种可能的情况

    if (isCreatingEdge.value) {
      if (!selectedNodesForEdge.value.find((n) => n.id === actualNode.id)) {
        selectedNodesForEdge.value.push(actualNode);
        broadcast();
      }

      if (selectedNodesForEdge.value.length === 2) {
        const [sourceNode, targetNode] = selectedNodesForEdge.value;

        // 判断连接类型
        const isCharacterToCharacter =
          sourceNode.type === "character" && targetNode.type === "character";
        const isCharacterToScene =
          (sourceNode.type === "character" && targetNode.type === "custom") ||
          (sourceNode.type === "custom" && targetNode.type === "character");

        if (isCharacterToCharacter) {
          // 角色间关系 - 显示角色关系编辑器
          showEdgeSelector.value = true;
          edgeType.value = "relationship";
        } else if (isCharacterToScene) {
          // 角色-场景关系 - 显示角色场景编辑器
          showEdgeSelector.value = true;
          edgeType.value = "character-scene";
        } else {
          // 不支持的连接类型
          // console.warn(
          //   "不支持的连接类型:",
          //   sourceNode.type,
          //   "->",
          //   targetNode.type
          // );
          alert("角色设计师只支持角色与角色、角色与场景之间的连接");

          // 重置连接状态
          isCreatingEdge.value = false;
          selectedNodesForEdge.value = [];
          showEdgeSelector.value = false;
        }
      }
    } else {
      selectedNode.value = { ...actualNode };
    }
  };

  // 修改结点信息的保存
  const handleDetailSave = (updatedData) => {
    // console.log("保存的节点数据：", updatedData);

    if (!updatedData || !updatedData.id || !updatedData.data) {
      // console.warn("[handleDetailSave] 无效参数：", updatedData);
      return -1;
    }

    let index = -1;

    // 如果当前有选中节点，才尝试查找并更新
    if (selectedNode.value) {
      index = nodes.value.findIndex((n) => n.id === updatedData.id);

      if (index !== -1) {
        nodes.value[index].data = {
          ...nodes.value[index].data,
          ...updatedData.data,
        };

        // ✅ 强制触发响应式更新
        nodes.value[index] = { ...nodes.value[index] };

        // console.log("更新后的节点数据：", nodes.value[index]);
      } else {
        // console.warn("未找到对应的节点 ID:", updatedData.id);
        return -1;
      }
    } else {
      // console.warn("无选中节点，可能是编辑逻辑未正确触发");
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
      type: "character",
      position: { x, y },
      data: {
        name: "新角色",
        avatar: require("@/assets/avatar/1.jpg"),
        age: null,
        occupation: "",
        personality: [],
        background: "",
        skills: [],
        items: "",
        notes: "",
        relationships: [],
      },
    };
    nodes.value.push(newNode);
    broadcast();
  };

  // 结点的删除
  const handleDeleteNode = (nodeId) => {
    const index = nodes.value.findIndex((n) => n.id === nodeId);
    if (index !== -1) {
      nodes.value.splice(index, 1);

      if (selectedNode.value?.id === nodeId) {
        selectedNode.value = null;
      }

      for (let i = edges.length - 1; i >= 0; i--) {
        if (edges[i].source === nodeId || edges[i].target === nodeId) {
          edges.splice(i, 1);
        }
      }
    }
    broadcast();
  };

  // 处理结点的位置变化
  const handlePositionChange = (payload) => {
    const { id, position } = payload;
    const nodeIndex = nodes.value.findIndex((n) => n.id === id);
    if (nodeIndex !== -1) {
      nodes.value[nodeIndex].position = position;
      nodes.value[nodeIndex] = { ...nodes.value[nodeIndex] }; // ✅ 强制 Vue 感知变化
    }
    broadcast();
  };
  // 广播节点和边的信息
  const broadcast = debounce(() => {
        if (
      socketState?.socket &&
      socketState.socket.readyState === WebSocket.OPEN
    ) {
    socketState.socket.send(
      JSON.stringify({
        type: "character",
        characterNodes: nodes.value,
        characterEdges: edges,
      })
    );
    }
    else{
      // console.error("未连接websocket");
    }
  }, 300);

  return {
    nodes,
    edges,
    selectedNode,
    editingEdgeId,
    isCreatingEdge,
    selectedNodesForEdge,
    showEdgeSelector,
    edgeType,
    handleCreateEdgeClick,
    handleEdgeSelect,
    handleEdgeConfirm,
    handleCharacterSceneEdgeConfirm,
    handleCharacterSceneEdgeEditConfirm,
    handleEdgeEditConfirm,
    handleEdgeCancel,
    handleDeleteEdge,
    handleConnectNode,
    handleNodeClick,
    handleDetailSave,
    handleAddNode,
    handleDeleteNode,
    handlePositionChange,
    broadcast,
  };
});
