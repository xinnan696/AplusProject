<template>
  <div class="priority-vehicle-tracking" :class="{ 'visible': isVisible }">
    <div class="panel-header">
      <h3>Priority Vehicle Tracking</h3>
      <button class="close-btn" @click="$emit('close')">×</button>
    </div>

    <div class="panel-content">
      <div class="junction-info">
        <span class="junction-title">Junction</span>
        <span class="junction-name">Dame Street - Dublin Castle</span>
      </div>

      <div class="traffic-light-section">
        <div class="traffic-light-header">
          <span class="direction">Traffic Light - S <span class="current-direction">(Current Travel Direction)</span></span>
        </div>

        <div class="light-controls">
          <div class="light-state">
            <span class="label">Light State</span>
            <div class="light-options">
              <button
                class="light-btn red"
                :class="{ 'active': lightStates.S === 'red' }"
                @click="setLightState('S', 'red')"
              >RED</button>
              <button
                class="light-btn green"
                :class="{ 'active': lightStates.S === 'green' }"
                @click="setLightState('S', 'green')"
              >GREEN</button>
            </div>
          </div>

          <div class="duration-control">
            <span class="label">Duration</span>
            <input type="text" class="duration-input" v-model="durations.S" placeholder="">
          </div>
        </div>
      </div>

      <div class="traffic-light-section">
        <div class="traffic-light-header">
          <span class="direction">Traffic Light - N</span>
        </div>

        <div class="light-controls">
          <div class="light-state">
            <span class="label">Light State</span>
            <div class="light-options">
              <button
                class="light-btn red"
                :class="{ 'active': lightStates.N === 'red' }"
                @click="setLightState('N', 'red')"
              >RED</button>
              <button
                class="light-btn green"
                :class="{ 'active': lightStates.N === 'green' }"
                @click="setLightState('N', 'green')"
              >GREEN</button>
            </div>
          </div>

          <div class="duration-control">
            <span class="label">Duration</span>
            <input type="text" class="duration-input" v-model="durations.N" placeholder="">
          </div>
        </div>
      </div>

      <div class="traffic-light-section">
        <div class="traffic-light-header">
          <span class="direction">Traffic Light - W</span>
        </div>

        <div class="light-controls">
          <div class="light-state">
            <span class="label">Light State</span>
            <div class="light-options">
              <button
                class="light-btn red"
                :class="{ 'active': lightStates.W === 'red' }"
                @click="setLightState('W', 'red')"
              >RED</button>
              <button
                class="light-btn green"
                :class="{ 'active': lightStates.W === 'green' }"
                @click="setLightState('W', 'green')"
              >GREEN</button>
            </div>
          </div>

          <div class="duration-control">
            <span class="label">Duration</span>
            <input type="text" class="duration-input" v-model="durations.W" placeholder="">
          </div>
        </div>
      </div>

      <div class="traffic-light-section">
        <div class="traffic-light-header">
          <span class="direction">Traffic Light - E</span>
        </div>

        <div class="light-controls">
          <div class="light-state">
            <span class="label">Light State</span>
            <div class="light-options">
              <button
                class="light-btn red"
                :class="{ 'active': lightStates.E === 'red' }"
                @click="setLightState('E', 'red')"
              >RED</button>
              <button
                class="light-btn green"
                :class="{ 'active': lightStates.E === 'green' }"
                @click="setLightState('E', 'green')"
              >GREEN</button>
            </div>
          </div>

          <div class="duration-control">
            <span class="label">Duration</span>
            <input type="text" class="duration-input" v-model="durations.E" placeholder="">
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <button class="apply-btn" @click="applyChanges">APPLY</button>
        <button class="cancel-btn" @click="$emit('close')">CANCEL</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

defineProps<{
  isVisible: boolean
}>()

defineEmits<{
  close: []
}>()

// 信号灯状态
const lightStates = reactive({
  S: 'green',
  N: 'red',
  W: 'red',
  E: 'red'
})

// 持续时间
const durations = reactive({
  S: '',
  N: '',
  W: '',
  E: ''
})

// 设置信号灯状态
const setLightState = (direction: string, state: string) => {
  lightStates[direction as keyof typeof lightStates] = state
}

// 应用更改
const applyChanges = () => {
  console.log('Applying changes:', {
    lightStates: { ...lightStates },
    durations: { ...durations }
  })
  // 这里可以添加实际的应用逻辑
}
</script>

<style scoped lang="scss">
.priority-vehicle-tracking {
  position: fixed;
  top: 0.64rem; // 在header下方
  right: -5.6rem; // 初始隐藏在右侧
  width: 5.6rem;
  height: calc(100vh - 0.64rem);
  background-color: #2B2B3C;
  border-left: 1px solid #3A3A4C;
  box-shadow: -0.02rem 0 0.1rem rgba(0, 0, 0, 0.3);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); // 更丝滑的缓动函数
  z-index: 1000;
  display: flex;
  flex-direction: column;
  transform: translateX(0); // 添加transform用于更流畅的动画

  &.visible {
    right: 0;
    box-shadow: -0.05rem 0 0.3rem rgba(0, 0, 0, 0.4); // 显示时增强阴影
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 0.64rem; // 固定高度为0.64rem
  padding: 0 0.2rem; // 只保持左右内边距
  background-color: #1E1E2F;
  border-bottom: 1px solid #3A3A4C;

  h3 {
    margin: 0;
    color: #FF4757;
    font-size: 0.2rem; // 20px 主标题
    font-weight: bold;
  }

  .close-btn {
    background: none;
    border: none;
    color: #FFFFFF;
    font-size: 0.24rem;
    cursor: pointer;
    padding: 0;
    width: 0.24rem;
    height: 0.24rem;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      color: #FF4757;
    }
  }
}

.panel-content {
  flex: 1;
  height: calc(100vh - 0.64rem - 0.64rem);
  padding: 0.12rem;
  overflow: hidden;
  opacity: 0;
  transform: translateY(0.2rem);
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
  transition-delay: 0.1s;
  display: flex;
  flex-direction: column;
}

.priority-vehicle-tracking.visible .panel-content {
  opacity: 1;
  transform: translateY(0); // 显示时移动到正常位置
}

.junction-info {
  margin-bottom: 0.1rem;
  display: flex;
  align-items: center;
  gap: 0.12rem;
  flex-shrink: 0;

  .junction-title {
    font-size: 0.14rem;
    color: #999;
    flex-shrink: 0;
  }

  .junction-name {
    font-size: 0.14rem;
    color: #FFFFFF;
    font-weight: bold;
    flex: 1;
  }
}

.traffic-light-section {
  margin-bottom: 0.12rem;
  padding: 0.08rem;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 0.06rem;
  flex-shrink: 0;

  .traffic-light-header {
    margin-bottom: 0.12rem;

    .direction {
      font-size: 0.14rem; // 14px 正文
      color: #FFFFFF;
      font-weight: 500;

      .current-direction {
        color: #00b4d8;
      }
    }
  }

  .light-controls {
    .light-state {
      margin-bottom: 0.1rem; // 缩小间距

      .label {
        display: block;
        font-size: 0.12rem; // 12px 小字体（保持不变）
        color: #999;
        margin-bottom: 0.06rem;
      }

      .light-options {
        display: flex;
        gap: 0.08rem;

        .light-btn {
          width: 1rem;
          height: 0.4rem;
          border: none;
          border-radius: 0.08rem;
          font-weight: bold;
          font-size: 0.2rem;
          color: #FFFFFF;
          cursor: pointer;
          background: linear-gradient(135deg, #2B2C3D 0%, #34354A 100%);
          transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
          position: relative;
          overflow: hidden;
          display: flex;
          align-items: center;
          justify-content: center;

          &::before {
            content: '';
            position: absolute;
            top: 0;
            left: -100%;
            width: 100%;
            height: 100%;
            background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
            transition: left 0.5s;
          }

          &:hover::before {
            left: 100%;
          }

          &.red {
            color: #E63946;

            &:hover:not(.active) {
              background: linear-gradient(135deg, #E63946 20%, #34354A 100%);
              color: #FFFFFF;
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(230, 57, 70, 0.3);
            }

            &.active {
              background: linear-gradient(135deg, #E63946 0%, #D62C37 100%);
              color: #FFFFFF;
              border: none;
              box-shadow: 0 4px 12px rgba(230, 57, 70, 0.4);
              transform: translateY(-1px);
            }
          }

          &.green {
            color: #2A9D8F;

            &:hover:not(.active) {
              background: linear-gradient(135deg, #2A9D8F 20%, #34354A 100%);
              color: #FFFFFF;
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(42, 157, 143, 0.3);
            }

            &.active {
              background: linear-gradient(135deg, #2A9D8F 0%, #238A7A 100%);
              color: #FFFFFF;
              border: none;
              box-shadow: 0 4px 12px rgba(42, 157, 143, 0.4);
              transform: translateY(-1px);
            }
          }
        }
      }
    }

    .duration-control {
      .label {
        display: block;
        font-size: 0.12rem; // 12px 小字体（保持不变）
        color: #999;
        margin-bottom: 0.06rem;
      }

      .duration-input {
        width: 100%;
        padding: 0.04rem;
        background-color: #1E1E2F;
        border: 1px solid #3A3A4C;
        border-radius: 0.04rem;
        color: #FFFFFF;
        font-size: 0.12rem;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        height: 0.24rem;
        box-sizing: border-box;

        &::placeholder {
          color: #666;
          transition: color 0.25s ease;
        }

        &:focus {
          outline: none;
          border-color: #00B4D8;
          box-shadow: 0 0 0 0.02rem rgba(0, 180, 216, 0.2);

          &::placeholder {
            color: #888;
          }
        }

        &:hover:not(:focus) {
          border-color: #555;
        }
      }
    }
  }
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  width: 4.2rem;
  margin: 0 auto;
  margin-top: auto;
  padding: 0.1rem 0;
  flex-shrink: 0;

  .apply-btn, .cancel-btn {
    width: 1.4rem;
    height: 0.4rem;
    font-size: 0.14rem;
    font-weight: bold;
    border-radius: 0.2rem;
    border: none;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 0;
      height: 0;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      transform: translate(-50%, -50%);
      transition: all 0.3s ease;
    }

    &:active::before {
      width: 200%;
      height: 200%;
    }
  }

  .apply-btn {
    background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%);
    color: #FFFFFF;

    &:hover {
      background: linear-gradient(135deg, #00D4F8 0%, #00a8cc 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 180, 216, 0.4);
    }
  }

  .cancel-btn {
    background: linear-gradient(135deg, #999999 0%, #777777 100%);
    color: #FFFFFF;

    &:hover {
      background: linear-gradient(135deg, #AAAAAA 0%, #888888 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(150, 150, 150, 0.3);
    }
  }
}
</style>
