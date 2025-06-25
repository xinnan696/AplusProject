<template>
  <div class="control-nav" :class="{ show: isVisible }">
    <div class="menu">
      <NavItem label="Control" :active="true" />
      <NavItem label="Dashboard" />
      <NavGroup title="Administration" :items="['User Roles', 'New User', 'Logs']" />
      <NavItem label="Help" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ isVisible: boolean }>()

import NavItem from '@/components/navCom/NavItem.vue'
import NavGroup from '@/components/navCom/NavGroup.vue'
</script>

<style scoped lang="scss">
.control-nav {
  position: absolute;
  top: 0.64rem;
  left: 0;
  width: 2.4rem;
  height: calc(100vh - 0.64rem);
  background-color: #1B1C2D;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  z-index: 1001;
  margin-top: 0.16rem;

  &.show {
    transform: translateX(0);
  }
}

.menu {
  display: flex;
  flex-direction: column;
  color: #FFFFFF;
  font-size: 0.16rem;
}
</style>
