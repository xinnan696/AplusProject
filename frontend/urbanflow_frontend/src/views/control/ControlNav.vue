<template>
  <div class="control-nav" :class="{ show: isVisible }">
    <div class="menu">
      <NavItem
        label="Control"
        :active="$route.name === 'Control'"
        @click="navigateTo('Control')"
      />
      <NavItem
        label="Dashboard"
        :active="$route.name === 'Dashboard'"
        @click="navigateTo('Dashboard')"
      />
      <NavGroup
        title="Administration"
        :items="['User Management', 'User Logs']"
        :currentRoute="$route.name as string"
        @sub-click="handleAdminClick"
      />
      <NavItem
        label="Help"
        :active="$route.name === 'Help'"
        @click="navigateTo('Help')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import NavItem from '@/components/navCom/NavItem.vue'
import NavGroup from '@/components/navCom/NavGroup.vue'

defineProps<{ isVisible: boolean }>()

const router = useRouter()

// 导航到指定路由 - 不改变导航栏状态
const navigateTo = (routeName: string) => {
  router.push({ name: routeName })
  // 不调用任何关闭导航栏的函数，保持当前状态
}

// 处理管理菜单点击 - 不改变导航栏状态
const handleAdminClick = (item: string) => {
  if (item === 'User Management') {
    router.push({ name: 'UserList' })
  } else if (item === 'User Logs') {
    router.push({ name: 'UserLog' })
  }
  // 不调用任何关闭导航栏的函数，保持当前状态
}
</script>

<style scoped lang="scss">
.control-nav {
  position: absolute;
  top: 0.64rem;  // 与Header底部对齐
  left: 0;
  width: 2.4rem;
  height: calc(100vh - 0.64rem);
  background-color: #1B1C2D;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  z-index: 1001;
  // 移除 margin-top，让导航栏顶部与header直接相连

  &.show {
    transform: translateX(0);
  }
}

.menu {
  display: flex;
  flex-direction: column;
  color: #FFFFFF;
  font-size: 0.16rem;
  padding-top: 0.16rem;  // 给菜单内容添加顶部间距，保持Control项的原有位置
}
</style>