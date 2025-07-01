<template>
  <div class="menu-group">
    <!-- 菜单头 -->
    <div class="group-title" @click="toggleMenu">
      <span class="title-text">{{ title }}</span>
      <div class="iconfont arrow-icon">
        {{ expanded ? '\ue749' : '\ue735' }}
      </div>
    </div>

    <!-- 子菜单：逐项进入/退出动画 -->
    <TransitionGroup
      v-if="itemsToShow.length"
      name="submenu"
      tag="div"
      class="sub-menu"
    >
      <div
        v-for="item in itemsToShow"
        :key="item"
        :class="[
          'sub-menu-item',
          {
            'active': (item === 'User Management' && props.currentRoute === 'UserList') ||
                     (item === 'User Logs' && props.currentRoute === 'UserLog')
          }
        ]"
        @click="$emit('sub-click', item)"
      >
        {{ item }}
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  title: string
  items: string[]
  currentRoute?: string
}>()

defineEmits<{
  'sub-click': [item: string]
}>()

const expanded = ref(false)
const itemsToShow = ref<string[]>([])

// 检查是否有子菜单项处于激活状态
const hasActiveItem = computed(() => {
  return (props.currentRoute === 'UserList') || (props.currentRoute === 'UserLog')
})

// 监听路由变化，如果有激活的子菜单项，保持展开状态
watch(hasActiveItem, (newValue) => {
  if (newValue && !expanded.value) {
    expanded.value = true
    itemsToShow.value = [...props.items]
  }
}, { immediate: true })

function toggleMenu() {
  // 如果当前有激活的子菜单项，不允许收起
  if (hasActiveItem.value && expanded.value) {
    return // 不做任何操作，保持展开状态
  }
  
  expanded.value = !expanded.value

  if (expanded.value) {
    itemsToShow.value = []
    props.items.forEach((item, index) => {
      setTimeout(() => {
        itemsToShow.value.push(item)
      }, index * 80)
    })
  } else {
    props.items.forEach((item, index) => {
      setTimeout(() => {
        itemsToShow.value = itemsToShow.value.filter(i => i !== item)
      }, index * 80)
    })
  }
}
</script>

<style scoped lang="scss">
.menu-group {
  margin-bottom: 0.12rem;

  .group-title {
    width: 2.24rem;
    height: 0.48rem;
    background-color: #2B2C3D;
    color: #fff;
    font-size: 0.16rem;
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
    margin-left: 0.08rem;
    cursor: pointer;
    border-radius: 0.04rem;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
      transition: left 0.4s ease;
    }

    &:hover {
      background-color: #2E2F41;
      transform: translateY(-1px);
      box-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
      
      &::before {
        left: 100%;
      }
      
      .arrow-icon {
        transform: translateY(-50%) scale(1.1);
        color: #00E3FF;
      }
    }
  }

  .arrow-icon {
    font-family: 'iconfont';
    font-size: 0.16rem;
    position: absolute;
    right: 0.12rem;
    top: 50%;
    transform: translateY(-50%);
    color: white;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .sub-menu {
    margin-top: 0.12rem;
    display: flex;
    flex-direction: column;
    gap: 0.12rem;
  }

  .sub-menu-item {
    width: 2.12rem;
    height: 0.40rem;
    background-color: #2B2C3D;
    color: #ccc;
    font-size: 0.14rem;
    text-align: center;
    line-height: 0.40rem;
    cursor: pointer;
    margin-left: 0.14rem;
    border-radius: 0.04rem;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
      transition: left 0.4s ease;
    }

    &:hover {
      background-color: #2E2F41;
      transform: translateY(-1px) translateX(2px);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
      color: #fff;
      
      &::before {
        left: 100%;
      }
    }

    &.active {
      background: linear-gradient(135deg, #00B4D8, #0096C7);
      color: white;
      box-shadow: 0 3px 8px rgba(0, 180, 216, 0.25);
      border-left: 2px solid #00E3FF;
      
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        right: 6px;
        width: 3px;
        height: 3px;
        background-color: #00E3FF;
        border-radius: 50%;
        transform: translateY(-50%);
        animation: pulse-small 2s infinite;
      }
    }
  }

  @keyframes pulse-small {
    0%, 100% {
      opacity: 1;
      transform: translateY(-50%) scale(1);
    }
    50% {
      opacity: 0.6;
      transform: translateY(-50%) scale(1.3);
    }
  }

  /* 进入动画 */
  .submenu-enter-from {
    opacity: 0;
    transform: translateY(-6px);
  }
  .submenu-enter-active {
    transition: all 0.25s ease;
  }

  /* 离开动画 */
  .submenu-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }
  .submenu-leave-active {
    transition: all 0.25s ease;
  }
}
</style>