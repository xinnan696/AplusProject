<template>
  <div class="menu-group">
    <div class="group-title" @click="toggleMenu">
      <span class="title-text">{{ title }}</span>
      <div class="iconfont arrow-icon">
        {{ expanded ? '\ue749' : '\ue735' }}
      </div>
    </div>

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
            'active': (item === 'Users' && ['UserList', 'AddUser', 'UserDetails', 'EditUser'].includes(props.currentRoute || '')) ||
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

const hasActiveItem = computed(() => {
  const userRoutes = ['UserList', 'AddUser', 'UserDetails', 'EditUser']
  const userLogRoutes = ['UserLog']

  return userRoutes.includes(props.currentRoute || '') || userLogRoutes.includes(props.currentRoute || '')
})

watch(hasActiveItem, (newValue) => {
  if (newValue && !expanded.value) {
    expanded.value = true
    itemsToShow.value = [...props.items]
  }
}, { immediate: true })

function toggleMenu() {
  if (hasActiveItem.value && expanded.value) {
    return
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


    &.active {
      background: linear-gradient(135deg, #00B4D8, #0096C7);
      color: white;
      box-shadow: 0 3px 8px rgba(0, 180, 216, 0.25);
    }
  }

  .submenu-enter-from {
    opacity: 0;
    transform: translateY(-6px);
  }
  .submenu-enter-active {
    transition: all 0.25s ease;
  }

  .submenu-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }
  .submenu-leave-active {
    transition: all 0.25s ease;
  }
}
</style>
