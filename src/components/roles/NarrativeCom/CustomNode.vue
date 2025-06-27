<template>
  <div 
    class="custom-node"
    @click="$emit('click', $event)"
    :style="stylePosition"
  >
    <div class="node-content">
      <span class="node-title">{{ props.data?.title || '未命名节点' }}</span>
      <button 
        class="delete-button"
        @click.stop="handleDelete"
      >
        ×
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, defineProps, defineEmits } from 'vue'

const props = defineProps({
  id: String,
  type: String,
  data: Object,
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 })
  }
})

const emit = defineEmits(['click', 'delete'])

// 安全访问位置，避免 undefined 报错
const stylePosition = computed(() => {
  const x = props.position?.x ?? 0
  const y = props.position?.y ?? 0
  return {
    left: `${x}px`,
    top: `${y}px`
  }
})

const handleDelete = (event) => {
  event.stopPropagation()
  emit('delete', props.id)
}
</script>


<style scoped>
.custom-node {
  position: relative;
  width: 150px;
  padding: 10px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1;
  cursor: pointer;
  /* transform: translate(-50%, -50%); 居中调整 */
  visibility: visible !important;
}

.node-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.node-title {
  font-size: 14px;
  color: #333;
}

.delete-button {
  background: #ff4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-button:hover {
  background: #cc0000;
}
</style>