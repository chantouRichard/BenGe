<template>
  <div :class="['task-card-wrapper', { 'slide-in': isSliding }]">
    <!-- Task Card -->
    <div class="task-card">
      <div class="task-card-top">
        <!-- 这是上面的矩形 -->
        <div class="task-card-head">
          <!-- 控制按钮 -->
           <img style="object-fit: cover;width: 30px;height: 30px;" src="../../../assets/second/wink.png"/>
           <h3 style="margin-left: 4px;">任务卡片</h3>
          <div
            v-if="isSliding"
            class="task-card-toggle"
            @click="toggleDrawer"
          >
            <i class="fas fa-caret-left"></i>
            <!-- Font Awesome 左箭头图标，代表折叠 -->
          </div>
          <div v-else class="task-card-toggle" @click="toggleDrawer">
            <i class="fas fa-caret-right"></i>
            <!-- Font Awesome 右箭头图标，代表展开 -->
          </div>
        </div>
        <div class="task-card-content">
          <!-- 任务卡片内容 -->
          <p style="width: 250px;">{{ content }}</p>
        </div>
      </div>

      <!-- 底部矩形，稍微旋转 -->
      <div class="task-card-bottom">
        <div class="task-card-content">
          <p>More details</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps } from "vue";

const props = defineProps({
  content: {
    type: String,
    required: true
  }
})

const isDrawerOpen = ref(true);
const isSliding = ref(true);

const toggleDrawer = () => {
  // 先触发旋转
  isDrawerOpen.value = !isDrawerOpen.value;

  // 延迟触发滑动动画
  setTimeout(() => {
    isSliding.value = isDrawerOpen.value;
  }, 100); // 300ms 与旋转动画时长一致
};
</script>

<style scoped>
.task-card-wrapper {
  position: fixed;
  top: 100px;
  left: -250px;
  z-index: 9000;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.5s ease; /* 添加平滑过渡 */
}

.task-card-wrapper.slide-in {
  transform: translateX(100%); /* 向左滑动 */
}

.task-card {
  position: relative;
  width: 300px;
  height: 200px;
  transform-origin: center;
  transition: transform 0.3s ease;
}

.task-card-top {
  position: absolute;
  width: 100%;
  height: 94%;
  border-radius: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1); /* 微微的下方阴影 */
  /* background-color: white; */
  background-image: url('../../../assets/second/cardback.png');
  background-size: cover;

  display: flex;
  flex-direction: column;
}

.task-card-head {
  margin: 10px;
  width: 100%;
  height: 30px;

  display: flex;
  align-items: center;
}

.task-card-bottom {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08); /* 更轻微的阴影 */
  background-color: white;
  background-image: url('../../../assets/second/cardback.png');


  transition: transform 0.5s ease;
}
.slide-in .task-card-bottom {
  top: 4%;
  left: 8%;
  transform: rotate(8deg); /* 回正 */
}

.task-card-top {
  background-color: white;
  top: 0;
  z-index: 2;
  display: flex;
}

.task-card-bottom {
  background-color: #f2f3f6;
  top: 2%;
  left: 2%;
  transform: rotate(0deg); /* 微微旋转 */
  z-index: 1;
  display: flex;
}

.task-card-content {
  text-align: center;
  font-size: 18px;
  display: flex;
  justify-content: center;
}

.task-card-toggle {
  position: absolute;
  right: 10px;
  font-size: 24px;
  cursor: pointer;
  z-index: 3;
}
</style>
