<template>
  <div class="workspace-container">
    <!-- 氛围设计师专用工具栏 -->
    <AtmosphereToolbar
      @add-node="atmosphereStore.handleAddNode"
      @atmo-palette="handleAtmospherePalette"
      @link-scene="handleLinkScene"
      @export-atmo="handleExport"
      @ai-generate="handleAiGenerate"
    />

    <!-- 主画布 - 使用canvasStore的数据以实现与NarrativeWorkspace同步 -->
    <CanvasArea
      ref="canvasRef"
      v-if="canvasStore.nodes.length > 0"
      :nodes="canvasStore.nodes"
      :edges="canvasStore.edges"
      @delete-node="atmosphereStore.handleDeleteNode"
      @node-select="atmosphereStore.handleNodeClick"
      @edge-select="atmosphereStore.handleEdgeSelect"
      @node-position-change="atmosphereStore.handlePositionChange"
      @connect-node="atmosphereStore.handleConnectNode"
    />

    <!-- 氛围节点详情面板 -->
    <div style="height: 100%;width: 100%;">
      <AtmosphereDetailPanel
        :visible="atmosphereStore.selectedNode"
        :nodeData="atmosphereStore.selectedNode"
        @save="handleDetailSave"
        @close="atmosphereStore.selectedNode = null"
      />

      <!-- 氛围设计师专注于氛围节点本身，不需要复杂的连线功能 -->
    </div>

    <!-- 氛围调色板 -->
    <AtmospherePalette
      :visible="showPalette"
      @close="showPalette = false"
      @select="handleAtmosphereSelect"
    />

    <!-- 简单的状态指示 -->
    <div v-if="atmosphereStore.isLinkingMode" class="linking-status">
      <span v-if="!atmosphereStore.selectedAtmosphereNode">🔗 点击氛围节点</span>
      <span v-else>🎯 点击场景节点建立关联</span>
    </div>
  </div>
</template>

<script setup>
import AtmosphereToolbar from './AtmosphereCom/AtmosphereToolbar.vue'
import AtmosphereDetailPanel from './AtmosphereCom/AtmosphereDetailPanel.vue'
import AtmospherePalette from './AtmosphereCom/AtmospherePalette.vue'
import CanvasArea from './NarrativeCom/CanvasArea.vue'
import { useAtmosphereStore } from '@/stores/atmosphere'
import { useCanvasStore } from '@/stores/canvasStore'
import { ref , computed } from 'vue'
import { socketState } from '@/stores/socket'
import request from '@/api/request'

// 传入参数
import { defineProps } from 'vue'

const props = defineProps({
  nodes: Array,
  edges: Array,
})

// 初始化store - 同时使用两个store（参考角色设计师模式）
const atmosphereStore = useAtmosphereStore()
const canvasStore = useCanvasStore()
const canvasRef = ref(null)

// 直接使用 canvasStore 中的数据驱动画布（用于强制更新）
const nodes = computed(() => canvasStore.nodes)

// 氛围调色板状态
const showPalette = ref(false)

// 氛围详情保存
const handleDetailSave = (updatedData) => {
  const index = atmosphereStore.handleDetailSave(updatedData);
  console.log("接受到的索引",index);
  console.log("当前节点数据",nodes.value)
  canvasRef.value?.forceUpdateNode(updatedData.id, nodes.value[index].data);
}

// 氛围调色板功能
const handleAtmospherePalette = () => {
  console.log('打开氛围调色板')
  showPalette.value = true
}

// 处理氛围选择
const handleAtmosphereSelect = (atmosphere) => {
  console.log('选择的氛围:', atmosphere)

  // 创建新的氛围节点
  const newNode = {
    id: 'atmo-' + Date.now() + '-' + Math.floor(Math.random() * 1000),
    type: 'atmosphere',
    position: { x: 300, y: 200 }, // 默认位置
    data: atmosphere.data
  }

  // 添加到画布
  canvasStore.nodes.push(newNode)
  console.log('[DEBUG] 从调色板创建氛围节点：', newNode)

  // 关闭调色板
  showPalette.value = false

  // 广播更新
  canvasStore.broadcast && canvasStore.broadcast()
}

// 关联场景功能
const handleLinkScene = () => {
  if (atmosphereStore.isLinkingMode) {
    // 如果已经在关联模式，则退出
    atmosphereStore.exitLinkingMode()
    console.log('退出氛围与场景关联模式')
  } else {
    // 进入关联模式
    atmosphereStore.handleLinkSceneClick()
    console.log('进入氛围与场景关联模式')
    console.log('请先点击氛围节点，再点击场景节点建立关联')
  }
}

// 处理AI生成氛围
const handleAiGenerate = async () => {
  console.log('AtmosphereDesign: handleAiGenerate 函数被调用了！');
  const userInput = prompt('请描述您要生成的氛围需求：', '生成与场景匹配的氛围节点，包括灯光、音乐、天气等')
  if (!userInput) return

  try {
    const contextData = {
      chat: socketState.messages.map(msg => ({
        user: msg.username || msg.sender,
        text: msg.content,
        time: msg.time
      })),
      canvasNodes: canvasStore.nodes.map(node => ({
        type: node.type,
        title: node.data?.title || '未命名',
        data: JSON.stringify(node.data).substring(0, 200)
      })),
      characterNodes: [] // 氛围设计师可能没有角色数据
    }

    const result = await request.post('/ai/generate-nodes', {
      userInput: userInput,
      designerType: 'atmosphere',
      contextData: JSON.stringify(contextData)
    })

    if (result.success && result.nodes) {
      result.nodes.forEach((nodeData, index) => {
        const newNode = {
          id: 'ai-atmosphere-' + Date.now() + '-' + index,
          type: 'atmosphere',
          position: {
            x: 400 + index * 250,
            y: 300 + index * 120
          },
          data: nodeData
        }
        canvasStore.nodes.push(newNode)
      })

      canvasStore.broadcast && canvasStore.broadcast()
      alert(`成功生成${result.nodes.length}个氛围节点！`)
    } else {
      alert('生成失败：' + (result.message || '未知错误'))
    }
  } catch (error) {
    console.error('AI生成失败:', error)
    alert('生成失败，请检查网络连接')
  }
}

// 导出氛围表功能
const handleExport = () => {
  console.log('导出氛围表')
  const lines = canvasStore.nodes.map(n => {
    const d = n.data
    return `## ${d.title || 'Untitled'} (${d.timeLabel || '-'})\n` +
           `- **情绪氛围**: ${d.mood || ''}\n` +
           `- **灯光设定**: ${d.lighting || ''}\n` +
           `- **音效/BGM**: ${d.music || ''}\n` +
           `- **天气环境**: ${d.weather || ''}\n` +
           `- **备注**: ${d.notes || ''}\n`
  }).join('\n')

  const blob = new Blob([lines], {type:'text/markdown'})
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'atmosphere.md'
  a.click()
  URL.revokeObjectURL(a.href)
}

</script>

<style scoped>
.workspace-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 简单的关联状态指示 */
.linking-status {
  position: fixed;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  z-index: 100;
  backdrop-filter: blur(4px);
}
</style>