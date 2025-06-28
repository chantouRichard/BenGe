<template>
  <div class="choose-area">
    <div class="choose-header">
      <h2 style="color: white">请选择你的角色</h2>
      <button class="button" style="margin-left: auto" @click="$emit('confirm')">
        选择完毕
      </button>
    </div>
    <div class="role-list">
      <div v-for="(role, index) in roles" :key="index" class="role-wrapper">
        <div class="card-container">
          <img
            :src="getRoleImage(index)"
            alt="角色图片"
            class="role-image"
            @click="$emit('selected', index)"
          />
        </div>
        <div class="role-text">{{ role.name }}</div>
        <div class="role-description">{{ role.description }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    roles: {
      type: Array,
      required: true
    }
  },
  emits: ['selected', 'confirm'],
  methods: {
    getRoleImage(index) {
      return require(`@/assets/second/role${index + 1}.jpeg`)
    }
  }
}
</script>

<style scoped>
.choose-area{
    display: flex;
    flex-direction: column;
    /* align-items: center; */

    width: 100%;
}
.choose-header {
  display: flex;
  margin: 0px 20px 20px;
  justify-content: space-between;
  width: 100%;
}

.role-list {
  display: flex;
  width: 100%;
  margin: auto;
  min-width: 700px;
  justify-content: space-evenly;
  gap: 20px;
}

.role-wrapper {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  transition: transform 0.3s ease;
  position: relative; /* ✅ 卡片允许自身浮动 */
}

/* 悬浮时卡片整体上移 */
.role-wrapper:hover {
  transform: translateY(-100px); /* ✅ 只这个卡片上移 */
  z-index: 1; /* ✅ 避免 hover 卡片被其他卡片遮住 */
}

/* 整个卡片容器：木纹背景 + 图片 */
.card-container {
  width: 100%;
  height: 240px;
  padding: 10px;
  background-image: url("@/assets/second/wood.jpg");
  background-size: cover;
  background-repeat: no-repeat;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.3s ease;
}

/* 图片本身只是填充，无需动画 */
.role-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 5px;
}

/* 文字样式 + 动画 */
.role-text {
  font-size: 24px;
  color: white;
  transition: font-size 0.3s ease;
}

/* 悬浮：整个卡片旋转 + 缩放；文字变大 */
.role-wrapper:hover .card-container {
  transform: scale(1.1) rotate(4deg);
}

.role-wrapper:hover .role-text {
  font-size: 28px;
}
/* 介绍文字默认透明 + 占位 */
.role-description {
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #555;
  font-size: 14px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  overflow: hidden;
}

/* 悬浮时显示文字 */
.role-wrapper:hover .role-description {
  opacity: 1;
  color: white;
  font-size: 18px;
}
</style>
