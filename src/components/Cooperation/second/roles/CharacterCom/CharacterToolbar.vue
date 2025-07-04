<template>
  <div
    class="character-toolball"
    :style="toolballStyle"
    @mouseenter="isExpanded = true"
    @mouseleave="isExpanded = false"
    @mousedown="startDrag"
  >
    <!-- 主按钮 -->
    <button class="main-button">
      <img src="../../../../../assets/second/role1.jpeg" alt="角色工具" class="icon-main" />
    </button>

    <!-- 展开面板 -->
    <transition name="panel-expand">
      <div v-if="isExpanded" class="tool-panel">
        <button
          v-for="(btn, i) in buttons"
          :key="btn.action"
          class="tool-button"
          :style="{ '--delay-index': i }"
          @click="$emit(btn.action)"
          @mouseenter="btn.hover = true"
          @mouseleave="btn.hover = false"
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

// 角色设计师专用按钮配置
const buttons = ref([
  {
    icon: require("@/assets/icons/plus-circle.svg"),
    action: "add-character",
    tooltip: "添加角色",
    color: "rgba(255, 140, 100, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/link.svg"),
    action: "add-relationship",
    tooltip: "建立关系",
    color: "rgba(255, 180, 120, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/second/role2.jpeg"),
    action: "character-template",
    tooltip: "角色模板",
    color: "rgba(180, 140, 255, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/file-text.svg"),
    action: "export-characters",
    tooltip: "导出角色表",
    color: "rgba(100, 220, 180, 0.7)",
    hover: false,
  },
  {
    icon: require("@/assets/icons/magic-wand.svg"),
    action: "ai-generate",
    tooltip: "AI生成角色",
    color: "rgba(255, 215, 0, 0.7)",
    hover: false,
  },
]);
// 判断是否房主
import { isOwner } from "@/api/room";
import { socketState } from "@/stores/socket";
const addAiIntegrateButton = () => {
  if (isOwner(socketState.roomId)) {
    buttons.value.push({
      icon: require("@/assets/icons/AI.svg"),
      action: "ai-integrate",
      tooltip: "AI整合",
      color: "rgba(255, 215, 0, 0.7)",
      hover: false,
    });
  }
};

addAiIntegrateButton();

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
  'add-character',
  'add-relationship',
  'character-template',
  'export-characters',
  'ai-generate',
  'ai-integrate'
]);
</script>

<style scoped>
.character-toolball {
  position: fixed;
  z-index: 1000;
  display: flex;
  align-items: center;
  cursor: grab;
  user-select: none;
  -webkit-user-drag: none;
}

.character-toolball:active {
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
              0 0 20px 2px rgba(255, 140, 100, 0.5);
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
              0 0 30px 4px rgba(255, 140, 100, 0.7);
  background-color: rgba(255, 180, 120, 0.9);
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
              inset 0 0 0 1px rgba(255, 255, 255, 0.3);
  margin-left: 12px;
}

.tool-button {
  position: relative;
  width: 44px;
  height: 44px;
  margin: 0 6px;
  background: rgba(255, 245, 235, 0.8);
  border-radius: 50%;
  border: 1px solid rgba(255, 140, 100, 0.5);
  transition: all 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28);
  cursor: pointer;
  align-self: center;
  opacity: 0;
  transform: scale(0.8);
  animation: button-appear 0.4s ease forwards;
  animation-delay: calc(var(--delay-index) * 0.1s);
  overflow: visible;
}

.tool-button:hover {
  background: rgba(255, 140, 100, 0.2);
  border-color: rgba(255, 140, 100, 0.8);
  transform: scale(1.1);
}

.tooltip {
  position: absolute;
  top: -35px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 9999;
}

.tool-button:hover .tooltip {
  opacity: 1;
}

/* 动画 */
@keyframes button-appear {
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.panel-expand-enter-active,
.panel-expand-leave-active {
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
}

.panel-expand-enter-from,
.panel-expand-leave-to {
  opacity: 0;
  transform: translateX(-20px) scale(0.8);
}
</style> 