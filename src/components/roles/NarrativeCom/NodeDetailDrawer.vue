<template>
  <transition name="slide-fade">
    <div 
      v-if="visible"
      class="detail-drawer"
      @click.self="handleClose"
    >
      <div class="drawer-content">
        <!-- 头部 -->
        <div class="drawer-header">
          <h3>节点详情</h3>
          <n-button 
            circle
            size="small"
            @click="handleClose"
          >
            <n-icon><close-icon /></n-icon>
          </n-button>
        </div>

        <!-- 表单主体 -->
        <n-form
          ref="formRef"
          :model="formData"
          label-placement="top"
        >
          <!-- 节点时间 -->
          <n-form-item label="节点时间" path="timeLabel">
            <n-input
              v-model:value="formData.timeLabel"
              placeholder="例：DAY1 09:00"
            />
          </n-form-item>

          <!-- 涉及人物 -->
          <n-form-item label="涉及人物" path="characters">
            <n-input
              v-model:value="formData.characters"
              placeholder="输入涉及的人物"
            />
          </n-form-item>

          <!-- 涉及线索 -->
          <n-form-item label="涉及线索" path="clues">
            <n-input
              v-model:value="formData.clues"
              placeholder="输入涉及的线索"
            />
          </n-form-item>

          <!-- 场景描述 -->
          <n-form-item label="场景描述" path="sceneDescription">
            <n-input
              v-model:value="formData.sceneDescription"
              type="textarea"
              placeholder="输入场景的大致描述..."
              :autosize="{ minRows: 3 }"
            />
          </n-form-item>

          <!-- 其他节点联系 -->
          <n-form-item label="其他节点联系" path="nodeConnections">
            <n-input
              v-model:value="formData.nodeConnections"
              type="textarea"
              placeholder="输入与其他节点的联系说明..."
              :autosize="{ minRows: 3 }"
            />
          </n-form-item>

          <!-- 后续注意点 -->
          <n-form-item label="后续注意点" path="notes">
            <n-input
              v-model:value="formData.notes"
              type="textarea"
              placeholder="输入后续需要注意的事项..."
              :autosize="{ minRows: 3 }"
            />
          </n-form-item>
        </n-form>

        <!-- 操作按钮 -->
        <div class="drawer-footer">
          <n-button @click="handleClose">取消</n-button>
          <n-button 
            type="primary"
            @click="handleSave"
          >
            保存
          </n-button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue';
import { defineEmits, defineProps } from 'vue';
import { NForm, NFormItem, NInput, NButton, NIcon } from 'naive-ui'; // 移除未使用的 NSpace, NRadio, NRadioGroup
import { Close as CloseIcon } from '@vicons/ionicons5';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: () => ({ data: {} })
  }
});

const emit = defineEmits(['close', 'save']);

// 表单数据和原始数据备份
const formData = ref({
  timeLabel: '',
  characters: '',
  clues: '',
  sceneDescription: '',
  nodeConnections: '',
  notes: ''
});
const originalData = ref({});

// 监听节点变化，加载原始数据
watch(() => props.node, (newNode) => {
  if (newNode) {
    originalData.value = { ...newNode.data }; // 备份原始数据
    formData.value = {
      timeLabel: newNode.data?.timeLabel || '',
      characters: newNode.data?.characters || '',
      clues: newNode.data?.clues || '',
      sceneDescription: newNode.data?.sceneDescription || '',
      nodeConnections: newNode.data?.nodeConnections || '',
      notes: newNode.data?.notes || ''
    };
  } else {
    formData.value = { timeLabel: '', characters: '', clues: '', sceneDescription: '', nodeConnections: '', notes: '' }; // 初始化为空
  }
}, { immediate: true });

// 监听 visible 变化
watch(() => props.visible, (newVal) => {
  console.log('Visible:', newVal);
}, { immediate: true });

// 保存处理
const handleSave = () => {
  emit('save', { ...formData.value });
  handleClose();
};

// 关闭处理（恢复原始数据）
const handleClose = () => {
  formData.value = { ...originalData.value }; // 取消时恢复原始数据
  emit('close');
};
</script>

<style scoped>
.detail-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  max-width: 400px;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.drawer-content {
  width: 100%;
  height: 100%;
  background: white;
  padding: 20px;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.drawer-footer {
  margin-top: auto;
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 动画效果 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>