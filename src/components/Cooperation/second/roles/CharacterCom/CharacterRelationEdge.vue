<template>
  <BaseEdge 
    :id="id"
    :style="edgeStyle"
    :source-x="sourceX"
    :source-y="sourceY"
    :target-x="targetX"
    :target-y="targetY"
    :label-style="labelStyle"
    :label-bg-style="labelBgStyle"
    :label-x="labelX"
    :label-y="labelY"
    :marker-end="markerEnd"
  >
    <text 
      :x="labelX" 
      :y="labelY" 
      class="relationship-label"
      :style="labelTextStyle"
    >
      {{ relationshipLabel }}
    </text>
    
    <!-- 关系强度指示器 -->
    <circle
      v-for="(dot, index) in strengthDots"
      :key="index"
      :cx="dot.x"
      :cy="dot.y"
      :r="dot.radius"
      :fill="strengthColor"
      :opacity="0.7"
    />
  </BaseEdge>
</template>

<script setup>
import { computed, defineProps } from 'vue'
import { BaseEdge } from '@vue-flow/core'

const props = defineProps({
  id: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  data: {
    type: Object,
    default: () => ({})
  }
})

// 关系数据
const relationData = computed(() => props.data || {})

// 关系标签
const relationshipLabel = computed(() => {
  const type = relationData.value.type || ''
  const typeMap = {
    'friend': '朋友',
    'lover': '恋人',
    'family': '家人',
    'colleague': '同事',
    'mentor': '师生',
    'enemy': '敌人',
    'rival': '竞争',
    'partner': '合作',
    'superior': '上下级',
    'other': '其他'
  }
  return typeMap[type] || type
})

// 根据关系类型确定颜色
const relationshipColor = computed(() => {
  const type = relationData.value.type || ''
  const status = relationData.value.status || 'active'
  
  if (status === 'conflict') {
    return '#ff4757' // 冲突关系 - 红色
  }
  
  const colorMap = {
    'friend': '#4a90e2',     // 朋友 - 蓝色
    'lover': '#ff6b9d',      // 恋人 - 粉色
    'family': '#26de81',     // 家人 - 绿色
    'colleague': '#7b68ee',  // 同事 - 紫色
    'mentor': '#ffa726',     // 师生 - 橙色
    'enemy': '#ff4757',      // 敌人 - 红色
    'rival': '#ff7675',      // 竞争 - 浅红色
    'partner': '#00b894',    // 合作 - 青色
    'superior': '#6c5ce7',   // 上下级 - 深紫色
    'other': '#95a5a6'       // 其他 - 灰色
  }
  
  if (status === 'passive') {
    return colorMap[type] || '#95a5a6' + '80' // 潜在关系 - 半透明
  }
  
  return colorMap[type] || '#95a5a6'
})

// 关系强度颜色
const strengthColor = computed(() => {
  const strength = relationData.value.strength || 5
  if (strength >= 8) return '#27ae60' // 强关系 - 绿色
  if (strength >= 5) return '#f39c12' // 中等关系 - 橙色
  return '#e74c3c' // 弱关系 - 红色
})

// 边样式
const edgeStyle = computed(() => ({
  stroke: relationshipColor.value,
  strokeWidth: Math.max(2, (relationData.value.strength || 5) / 2),
  strokeDasharray: relationData.value.status === 'passive' ? '5,5' : 'none'
}))

// 标签位置
const labelX = computed(() => (props.sourceX + props.targetX) / 2)
const labelY = computed(() => (props.sourceY + props.targetY) / 2)

// 标签样式
const labelStyle = computed(() => ({
  fill: '#ffffff',
  fontSize: '12px',
  fontWeight: 'bold',
  textAnchor: 'middle',
  dominantBaseline: 'middle'
}))

// 标签背景样式
const labelBgStyle = computed(() => ({
  fill: relationshipColor.value,
  fillOpacity: 0.9,
  rx: 8,
  ry: 4
}))

// 标签文字样式
const labelTextStyle = computed(() => ({
  fill: '#ffffff',
  fontSize: '12px',
  fontWeight: 'bold',
  textAnchor: 'middle',
  dominantBaseline: 'middle',
  pointerEvents: 'none'
}))

// 箭头标记
const markerEnd = computed(() => {
  return `url(#arrowhead-${relationData.value.type || 'default'})`
})

// 关系强度点
const strengthDots = computed(() => {
  const strength = relationData.value.strength || 5
  const dots = []
  const numDots = Math.ceil(strength / 2) // 每2点强度显示一个点
  
  for (let i = 0; i < numDots && i < 5; i++) {
    const progress = (i + 1) / (numDots + 1)
    dots.push({
      x: props.sourceX + (props.targetX - props.sourceX) * progress,
      y: props.sourceY + (props.targetY - props.sourceY) * progress,
      radius: 2
    })
  }
  
  return dots
})
</script>

<style scoped>
.relationship-label {
  pointer-events: none;
  user-select: none;
}
</style> 