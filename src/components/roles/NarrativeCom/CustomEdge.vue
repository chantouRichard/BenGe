<template>
    <g :data-id="id">
        <path class="custom-edge-path" :d="edgePath" fill="none" :style="edgeStyle" :marker-end="markerEnd" />
        <EdgeLabelRenderer v-if="labelVisible">
            <div :style="labelStyle" class="edge-label">
                {{ edgeLabel || edgeTypeName }}
            </div>
        </EdgeLabelRenderer>
    </g>
</template>

<script setup>
import { computed, defineProps, toRef, watch } from 'vue'
import { getBezierPath, EdgeLabelRenderer } from '@vue-flow/core'

const props = defineProps({
    id: String,
    source: String,
    target: String,
    sourceX: Number,
    sourceY: Number,
    targetX: Number,
    targetY: Number,
    sourcePosition: String,
    targetPosition: String,
    sourceHandle: String,
    targetHandle: String,
    data: {
        type: Object,
        default: () => ({})
    }
});

const data = toRef(props, 'data') // 显式追踪 props.data 引用

watch(
  () => props.data,
  (newVal) => {
    console.log('[DEBUG] edge.data changed', newVal);
  },
  { deep: true, immediate: true }
);
// 响应式处理
const edgeLabel = computed(() => data?.value.label || '')
// 根据选择结点的方位，设置起点和终点的位置
const inferPositionFromHandle = (handleId) => {
    if (!handleId) return 'bottom'
    if (handleId.includes('left')) return 'left'
    if (handleId.includes('right')) return 'right'
    if (handleId.includes('top')) return 'top'
    if (handleId.includes('bottom')) return 'bottom'
    return 'bottom'
}

const sourcePos = computed(() => inferPositionFromHandle(props.sourceHandle))
const targetPos = computed(() => inferPositionFromHandle(props.targetHandle))

const edgePath = computed(() => {
    const [path] = getBezierPath({
        sourceX: props.sourceX,
        sourceY: props.sourceY,
        targetX: props.targetX,
        targetY: props.targetY,
        sourcePosition: sourcePos.value,
        targetPosition: targetPos.value
    })
    return path
})

const edgeTypeName = computed(() => {
    const map = {
        causal: '因果',
        dependency: '依赖',
        composition: '包含',
        containment: '包含',
        inherit: '继承',
        inheritance: '继承',
        parallel: '并行',
        conflict: '冲突',
        conditional: '条件'
    }
    return map[data?.value.type] || '关联'
})

const labelVisible = computed(() => !!data?.value.label || !!data?.value.type)

const edgeStyle = computed(() => {
    const type = data?.value.type
    switch (type) {
        case 'dependency':
        case 'conditional':
            return { stroke: '#4A90E2', strokeWidth: 2, strokeDasharray: '5,5' }
        case 'causal':
            return { stroke: '#2ecc71', strokeWidth: 2 }
        case 'composition':
        case 'containment':
            return { stroke: '#9b59b6', strokeWidth: 2 }
        case 'inherit':
        case 'inheritance':
            return { stroke: '#34495e', strokeWidth: 2, strokeDasharray: '3,3' }
        case 'conflict':
            return { stroke: '#e74c3c', strokeWidth: 2, strokeDasharray: '6,4' }
        case 'parallel':
            return { stroke: '#f39c12', strokeWidth: 2 }
        default:
            return { stroke: '#7f8c8d', strokeWidth: 2 }
    }
})

const markerEnd = computed(() => 'url(#arrow)')

const labelStyle = {
    position: 'absolute',
    transform: 'translate(-50%, -50%)',
    backgroundColor: '#fff',
    padding: '2px 6px',
    borderRadius: '4px',
    fontSize: '12px',
    color: '#333',
    border: '1px solid #E0E7FF',
    whiteSpace: 'nowrap'
}
</script>

<style scoped>
.edge-label {
    pointer-events: none;
}
</style>