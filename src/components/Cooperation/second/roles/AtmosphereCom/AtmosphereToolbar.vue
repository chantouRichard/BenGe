<template>
  <div
    class="atmosphere-toolball"
    :style="toolballStyle"
    @mouseenter="isExpanded = true"
    @mouseleave="isExpanded = false"
    @mousedown="startDrag"
  >
    <!-- 主按钮 -->
    <button class="main-button">
      <img src="../../../../../assets/second/role3.jpeg" alt="氛围工具" class="icon-main" />
    </button>

    <!-- 展开面板 -->
    <transition name="panel-expand">
      <div v-if="isExpanded" class="tool-panel">
        <button
          v-for="(btn, i) in buttons"
          :key="btn.action"
          class="tool-button"
          :style="{ '--delay-index': i }"
          :title="btn.tooltip"
          @click="$emit(btn.action)"
          @mouseenter="handleButtonHover(btn, true)"
          @mouseleave="handleButtonHover(btn, false)"
        >
          <img
            :src="btn.icon"
            :alt="btn.tooltip"
            class="icon-tool"
            :style="{
              transform: btn.hover ? 'scale(1.15)' : 'none',
              filter: btn.hover
                ? 'drop-shadow(0 0 8px ' + btn.color + ')'
                : 'none',
            }"
          />
          <span class="tooltip">{{ btn.tooltip }}</span>
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";

// 氛围设计师专用按钮配置
const buttons = ref([
  {
    icon: require("@/assets/icons/plus-circle.svg"),
    action: "add-node",
    tooltip: "添加氛围节点",
    color: "rgba(135, 206, 235, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/magic-wand.svg"),
    action: "atmo-palette",
    tooltip: "氛围模板",
    color: "rgba(255, 182, 193, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/link.svg"),
    action: "link-scene",
    tooltip: "关联场景",
    color: "rgba(221, 160, 221, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/file-text.svg"),
    action: "export-atmo",
    tooltip: "导出氛围表",
    color: "rgba(144, 238, 144, 0.7)",
    hover: false,
  },
]);

// 交互状态
const isExpanded = ref(false);
const isDragging = ref(false);
const position = ref({ x: 40, y: 40 });
const dragStartPos = ref(null);

// 动态样式
const toolballStyle = computed(() => ({
  left: `${position.value.x}px`,
  top: `${position.value.y}px`,
  "--panel-width": `${buttons.value.length * 56 + 16}px`,
}));

// 处理按钮悬停
const handleButtonHover = (btn, isHover) => {
  btn.hover = isHover;
  if (isHover) {
    console.log('悬停在工具:', btn.tooltip);
  }
};

// 拖拽逻辑
const startDrag = (e) => {
  e.preventDefault();
  e.stopPropagation();

  isDragging.value = true;
  const clientX = e.clientX || e.touches[0].clientX;
  const clientY = e.clientY || e.touches[0].clientY;
  dragStartPos.value = {
    x: clientX - position.value.x,
    y: clientY - position.value.y,
  };

  const moveHandler = (moveEvent) => {
    const clientX = moveEvent.clientX || moveEvent.touches[0].clientX;
    const clientY = moveEvent.clientY || moveEvent.touches[0].clientY;
    position.value = {
      x: clientX - dragStartPos.value.x,
      y: clientY - dragStartPos.value.y,
    };
  };

  const endHandler = () => {
    window.removeEventListener("mousemove", moveHandler);
    window.removeEventListener("touchmove", moveHandler);
    window.removeEventListener("mouseup", endHandler);
    window.removeEventListener("touchend", endHandler);
    isDragging.value = false;
  };

  window.addEventListener("mousemove", moveHandler);
  window.addEventListener("touchmove", moveHandler);
  window.addEventListener("mouseup", endHandler);
  window.addEventListener("touchend", endHandler);
};

// 发出事件
defineEmits([
  'add-node',
  'atmo-palette',
  'link-scene',
  'export-atmo'
]);
</script>

<style scoped>
.atmosphere-toolball {
  position: fixed;
  z-index: 1000;
  display: flex;
  align-items: center;
  cursor: grab;
  user-select: none;
  -webkit-user-drag: none;
}

.atmosphere-toolball:active {
  cursor: grabbing;
}

.main-button {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15), 
              0 0 0 1px rgba(255, 255, 255, 0.1),
              0 0 20px 2px rgba(135, 206, 235, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  display: grid;
  place-items: center;
  overflow: hidden;
}

.main-button:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2), 
              0 0 0 1px rgba(255, 255, 255, 0.2),
              0 0 30px 4px rgba(135, 206, 235, 0.7);
  background-color: rgba(173, 216, 230, 0.9);
}

/* 图标样式 */
.icon-main {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.icon-tool {
  width: 22px;
  height: 22px;
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  border-radius: 50%;
  object-fit: cover;
}

.tool-panel {
  display: flex;
  padding: 0 12px;
  height: 56px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1),
              inset 0 0 0 1px rgba(255, 255, 255, 0.4);
  margin-left: 12px;
  align-items: center;
  overflow: visible; /* 允许 tooltip 显示在面板外 */
}

.tool-button {
  position: relative;
  width: 40px;
  height: 40px;
  margin: 0 4px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  display: grid;
  place-items: center;
  animation: toolButtonSlideIn 0.5s ease calc(var(--delay-index) * 0.1s) both;
}

.tool-button:hover {
  transform: scale(1.1) translateY(-2px);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.tooltip {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.95);
  background: rgba(0, 0, 0, 0.85);
  padding: 8px 12px;
  border-radius: 8px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  pointer-events: none;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  z-index: 9999;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 添加小箭头 */
.tooltip::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid rgba(0, 0, 0, 0.85);
}

.tool-button:hover .tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(-4px);
}

/* 动画效果 */
.panel-expand-enter-active,
.panel-expand-leave-active {
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.27, 1.55);
}

.panel-expand-enter-from {
  opacity: 0;
  transform: translateX(-20px) scale(0.8);
}

.panel-expand-leave-to {
  opacity: 0;
  transform: translateX(-20px) scale(0.8);
}

@keyframes toolButtonSlideIn {
  from {
    opacity: 0;
    transform: translateX(-20px) scale(0.5);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}
</style> 