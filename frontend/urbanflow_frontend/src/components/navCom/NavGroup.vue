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
        class="sub-menu-item"
        @click="$emit('sub-click', item)"
      >
        {{ item }}
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  title: string
  items: string[]
}>()

const expanded = ref(false)
const itemsToShow = ref<string[]>([])

function toggleMenu() {
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
  }

  .arrow-icon {
    font-family: 'iconfont';
    font-size: 0.16rem;
    position: absolute;
    right: 0.12rem;
    top: 50%;
    transform: translateY(-50%);
    color: white;
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

    &:hover {
    background-color: #2E2F41;
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
