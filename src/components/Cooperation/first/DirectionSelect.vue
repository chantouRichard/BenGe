<template>
  <div class="direction-select">
    <h2 class="title">剧本方向选择</h2>

    <div class="selection-container">
      <!-- 目标区域 (已选择) -->
      <div class="target-area">
        <h3>已选择的方向主题 (最多3个)</h3>
        <div
            class="card-container"
            @drop="onDrop($event, 'selected')"
            @dragover.prevent
            @dragenter.prevent
        >
          <div
              v-for="(item, index) in selectedDirections"
              :key="'selected-'+index"
              class="direction-card"
              draggable="true"
              @dragstart="startDrag($event, item, 'selected')"
              :title="item"
          >
            <span class="card-content">{{ item }}</span>
            <span class="remove-btn" @click="removeDirection(index, 'selected')">×</span>
          </div>
          <div v-if="selectedDirections.length === 0" class="empty-placeholder">
            请从右侧拖入方向主题
          </div>
        </div>
      </div>

      <!-- 存放区域 (未选择) -->
      <div class="storage-area">
        <h3>可选的方向主题</h3>
        <div
            class="card-container"
            @drop="onDrop($event, 'unselected')"
            @dragover.prevent
            @dragenter.prevent
        >
          <div
              v-for="(item, index) in unselectedDirections"
              :key="'unselected-'+index"
              class="direction-card"
              draggable="true"
              @dragstart="startDrag($event, item, 'unselected')"
              :title="item"
          >
            <span class="card-content">{{ item }}</span>
            <span class="remove-btn" @click="deleteDirection(index)">×</span>
          </div>
          <div v-if="unselectedDirections.length === 0" class="empty-placeholder">
            暂无更多可选主题
          </div>
        </div>
      </div>
    </div>

    <div class="add-direction">
      <input
          v-model="newDirection"
          type="text"
          placeholder="输入新的方向主题"
          @keyup.enter="addNewDirection"
      >
      <button @click="addNewDirection">添加</button>
    </div>

    <button
        class="confirm-btn"
        :disabled="selectedDirections.length === 0"
        @click="confirmSelection"
    >
      确认选择
    </button>
  </div>
</template>

<script>
export default {
  name: 'DirectionSelect',
  data() {
    return {
      selectedDirections: [],  // 已选择的方向
      unselectedDirections: [  // 未选择的方向
        '悬疑推理',
        '浪漫爱情',
        '科幻未来',
        '历史传记',
        '青春校园',
        '恐怖惊悚'
      ],
      newDirection: '',        // 新方向输入
      dragItem: null,          // 当前拖拽的项
      dragSource: null         // 拖拽来源区域
    }
  },
  methods: {
    // 开始拖拽
    startDrag(event, item, source) {
      this.dragItem = item
      this.dragSource = source
      event.dataTransfer.effectAllowed = 'move'
    },

    // 放置处理
    onDrop(event, targetArea) {
      if (!this.dragItem) return

      // 如果拖到相同区域，不做处理
      if (this.dragSource === targetArea) return

      // 从源区域移除
      if (this.dragSource === 'selected') {
        const index = this.selectedDirections.indexOf(this.dragItem)
        if (index !== -1) {
          this.selectedDirections.splice(index, 1)
        }
      } else {
        const index = this.unselectedDirections.indexOf(this.dragItem)
        if (index !== -1) {
          this.unselectedDirections.splice(index, 1)
        }
      }

      // 添加到目标区域
      if (targetArea === 'selected') {
        // 检查是否已达到最大数量
        if (this.selectedDirections.length >= 3) {
          this.$message.warning('最多只能选择3个方向主题')
          // 把项目放回原区域
          if (this.dragSource === 'selected') {
            this.selectedDirections.push(this.dragItem)
          } else {
            this.unselectedDirections.push(this.dragItem)
          }
          return
        }
        this.selectedDirections.push(this.dragItem)
      } else {
        this.unselectedDirections.push(this.dragItem)
      }

      this.dragItem = null
      this.dragSource = null
    },

    // 移除方向（从已选择区域）
    removeDirection(index, area) {
      if (area === 'selected') {
        const [removed] = this.selectedDirections.splice(index, 1)
        this.unselectedDirections.push(removed)
      }
    },

    // 删除方向（从可选区域）
    deleteDirection(index) {
      this.unselectedDirections.splice(index, 1)
    },

    // 添加新方向
    addNewDirection() {
      if (!this.newDirection.trim()) {
        this.$message.warning('请输入有效方向主题')
        return
      }

      if (this.unselectedDirections.includes(this.newDirection)) {
        this.$message.warning('该方向主题已存在')
        return
      }

      this.unselectedDirections.push(this.newDirection)
      this.newDirection = ''
    },

    // 确认选择
    confirmSelection() {
      if (this.selectedDirections.length === 0) {
        this.$message.warning('请至少选择一个方向主题')
        return
      }

      this.$emit('confirm', this.selectedDirections)
    }
  }
}
</script>

<style scoped>
.direction-select {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Arial', sans-serif;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.title {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.selection-container {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.target-area, .storage-area {
  width: 48%; /* 固定宽度 */
  height: 100%;
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 15px;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.target-area {
  border-color: #67c23a;
  background-color: rgba(103, 194, 58, 0.05);
}

.storage-area {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.05);
}

h3 {
  margin-top: 0;
  color: #555;
  text-align: center;
  flex-shrink: 0;
}

.card-container {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.direction-card {
  padding: 12px 15px;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  cursor: move;
  position: relative;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  min-height: 40px;
}

.direction-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.card-content {
  flex: 1;
  word-wrap: break-word; /* 允许单词内换行 */
  white-space: normal; /* 允许换行 */
  overflow: hidden;
  padding-right: 20px;
}

.remove-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #f56c6c;
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.empty-placeholder {
  text-align: center;
  color: #999;
  padding: 20px;
  font-style: italic;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-direction {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.add-direction input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.add-direction button {
  padding: 10px 20px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.add-direction button:hover {
  background-color: #66b1ff;
}

.confirm-btn {
  display: block;
  width: 100%;
  padding: 12px;
  background-color: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

.confirm-btn:hover {
  background-color: #85ce61;
}

.confirm-btn:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}
</style>