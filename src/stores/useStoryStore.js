import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useStoryStore = defineStore('story', () => {
  // 当前选中的节点
  const selectedNode = ref(null)
  
  // 所有节点数据
  const nodes = ref([])
  
  // 所有连线
  const edges = ref([])

  // 操作方法
  const setSelectedNode = (node) => {
    selectedNode.value = node
  }

  const addNode = (node) => {
    nodes.value.push({
      id: Date.now().toString(),
      ...node
    })
  }

  return { 
    selectedNode,
    nodes,
    edges,
    setSelectedNode,
    addNode
  }
})