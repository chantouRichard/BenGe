<template>
  <div class="edit-area">
    <!-- 顶部标题 + 按钮 -->
    <div class="edit-header">
      <h2>
        第二阶段:分工设计阶段<br />
        你的角色是：{{ roles[userRole].name }}
      </h2>
      <button class="button" @click="$emit('nextStage')">下一阶段</button>
    </div>

    <!-- 全屏编辑区域 -->
    <teleport to="body">
      <transition name="fullscreen-slide">
        <div class="canvas fullscreen" v-if="isFullScreen">
          <button
            class="canvasButton"
            @click="isFullScreen = false"
            :style="{ backgroundImage: `url(${reduceIcon})` }"
          ></button>
          <div style="padding: 20px">
            <NarrativeWorkspace
              @updateGraph="handleGraphUpdate"
            />
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 常规编辑区域 -->
    <transition name="canvas-fade">
      <div class="canvas" v-if="!isFullScreen">
        <button
          class="canvasButton"
          @click="isFullScreen = true"
          :style="{ backgroundImage: `url(${enlargeIcon})` }"
        ></button>
        <div style="padding: 20px; height: 100%">
          <NarrativeWorkspace
            v-if="userRole == 0"
            @updateGraph="handleGraphUpdate"
          />
          <CharacterDesign
            v-if="userRole == 1"
            @updateGraph="handleGraphUpdate"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import NarrativeWorkspace from "@/components/roles/NarrativeWorkspace.vue";
import CharacterDesign from "@/components/roles/CharacterDesign.vue";

export default {
  name: "EditArea",
  components: {
    NarrativeWorkspace,
    CharacterDesign,
  },
  props: {
    roles: {
      type: Array,
      required: true,
    },
    userRole: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      isFullScreen: false,
      enlargeIcon: require("@/assets/second/enlarge.png"),
      reduceIcon: require("@/assets/second/reduce.png"),

      sharedNodes: [],
      sharedEdges: [],
    };
  },
  methods: {
    handleGraphUpdate(newGraph) {
      this.sharedNodes = newGraph.nodes;
      this.sharedEdges = newGraph.edges;
    },
  },
};
</script>

<style scoped>
.edit-area {
  width: 66%;
  min-width: 442px;
  background: transparent;
  color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}
.edit-header {
  display: flex;
  margin: 0px 20px 20px;
  align-items: center;
  justify-content: space-between;
}

.canvasButton {
  width: 32px;
  height: 32px;

  border: none;
  position: absolute; /* 绝对定位 */
  top: 20px; /* 距离父容器顶部 10px */
  right: 20px; /* 距离父容器右侧 10px */
  object-fit: cover;

  background: transparent;

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  z-index: 900;
  transition: all 0.3s ease;
}

.canvasButton:hover {
  width: 40px;
  height: 40px;
}
.canvas {
  position: relative;
  width: 100%;
  height: calc(100% - 100px);

  background-image: url(~@/assets/second/canvas.png);

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  overflow: hidden;

  transition: all 0.5s ease;
}

.canvas.fullscreen {
  position: fixed; /* 使其浮动在最顶层 */
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999; /* 保证在最顶层 */
  background-color: white; /* 如需白底，或保持透明 */

  /* ✅ 关键调整项 */
  background-size: 120% auto; /* 放大宽度，高度自适应 */
  background-position: center center; /* 始终聚焦中心 */
  background-repeat: no-repeat;

  transition: all 0.5s ease;
}
.canvas-fade-enter-active {
  opacity: 1;
  transition: all 0.5s ease;
}
.canvas-fade-leave-active {
  opacity: 0;
  transition: all 0.5s ease;
}
/* 动画进入 */
.fullscreen-slide-enter-active {
  animation: slideDown 0.5s ease-out forwards;
}

/* 动画离开 */
.fullscreen-slide-leave-active {
  animation: slideUp 0.5s ease-in forwards;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(0);
    opacity: 1;
  }
  to {
    transform: translateY(100%);
    opacity: 0;
  }
}
</style>
