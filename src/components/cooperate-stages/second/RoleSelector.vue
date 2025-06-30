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
          <!-- 显示已选择角色的用户名 -->
          <div v-if="roleSelections[index]" class="role-username">
            {{ roleSelections[index] }}
          </div>
          <img
            :src="getRoleImage(index)"
            alt="角色图片"
            class="role-image"
            @click="selectRole(index)"
          />
        </div>
        <div class="role-text">{{ role.name }}</div>
        <div class="role-description">{{ role.description }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { socketState } from '@/stores/socket';
export default {
  props: {
    roles: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
      selectedRole: null,  // 当前成员选择的角色
      roleSelections: {}   // 存储每个角色的选择状态，key是角色索引，value是已选择的用户名
    };
  },
  emits: ['selected', 'confirm'],
  methods: {
    getRoleImage(index) {
      return require(`@/assets/second/role${index + 1}.jpeg`)
    },
    // 选择角色
    selectRole(index) {
      // 如果该角色已被其他成员选择，则弹出提示
      if (this.roleSelections[index]) {
        this.$message({
          message: `${this.roleSelections[index]} 已选择该角色！`,
          type: 'warning',
        });
        return;
      }

      // 如果是当前用户选择角色
      this.selectedRole = this.roles[index];
      this.emitRoleSelection(index);  // 通过 WebSocket 发送角色选择
    },

    // 发送角色选择到服务器
    emitRoleSelection(index) {
      const roleName = this.roles[index].name;
      socketState.socket.send(
        JSON.stringify({
          type: "roleSelection",
          roleName,
          username: socketState.currentUsername,
        })
      );
    },

    // 确认角色选择
    confirmSelection() {
      this.$emit("confirm", this.selectedRole);
    },
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

  cursor: pointer;
}
.role-username {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 3px;
}

.disabled {
  pointer-events: none; /* 禁用已选择的角色 */
  opacity: 0.5; /* 让已选择的角色看起来不可选 */
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
/* 介绍文字默认透明 */
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

  position: absolute;
  top: 300px;
}

/* 悬浮时显示文字 */
.role-wrapper:hover .role-description {
  opacity: 1;
  color: white;
  font-size: 18px;
}

</style>
