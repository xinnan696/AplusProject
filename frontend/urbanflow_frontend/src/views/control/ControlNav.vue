<template>
  <div class="control-nav" :class="{ show: isVisible }">
    <div class="menu">

      <NavItem
        v-if="canAccess('Control')"
        label="Control"
        :active="$route.name === 'Control'"
        @click="navigateTo('Control')"
      />

      <NavItem
        v-if="canAccess('Dashboard')"
        label="Dashboard"
        :active="$route.name === 'Dashboard'"
        @click="navigateTo('Dashboard')"
      />

    
      <NavGroup
        v-if="canAccess('Administration')"
        title="Administration"
        :items="getAdminItems()"
        :currentRoute="$route.name as string"
        @sub-click="handleAdminClick"
      />

      <!-- Help - 所有角色都可见 -->
      <NavItem
        v-if="canAccess('Help')"
        label="Help"
        :active="$route.name === 'Help'"
        @click="navigateTo('Help')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NavItem from '@/components/navCom/NavItem.vue'
import NavGroup from '@/components/navCom/NavGroup.vue'

defineProps<{ isVisible: boolean }>()

const router = useRouter()
const authStore = useAuthStore()

const userRole = computed(() => authStore.userRole)

const rolePermissions = {
  'ADMIN': ['Control', 'Dashboard', 'Administration', 'Help'],
  'Traffic Manager': ['Control', 'Dashboard', 'Help'],
  'Traffic Planner': ['Dashboard', 'Help']
}

const canAccess = (item: string): boolean => {
  const role = userRole.value
  const permissions = rolePermissions[role as keyof typeof rolePermissions] || []

  console.log(`Checking nav access: ${item} for role: ${role}`, permissions.includes(item))
  return permissions.includes(item)
}


const getAdminItems = (): string[] => {
  if (!authStore.isAdmin()) {
    return []
  }

  return ['Users', 'User Logs']
}

const navigateTo = (routeName: string) => {

  if (authStore.hasPageAccess(routeName)) {
    router.push({ name: routeName })
  } else {
    console.warn(`Access denied to ${routeName} for role ${userRole.value}`)
  }
}

const handleAdminClick = (item: string) => {

  if (!authStore.isAdmin()) {
    console.warn('Access denied: Not an admin')
    return
  }

  if (item === 'Users') {
    router.push({ name: 'UserList' })
  } else if (item === 'User Logs') {
    router.push({ name: 'UserLog' })
  }
}
</script>

<style scoped lang="scss">
.control-nav {
  position: absolute;
  top: 0.64rem;  
  left: 0;
  width: 2.4rem;
  height: calc(100vh - 0.64rem);
  background-color: #1e1e2f;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  z-index: 1001;

  &.show {
    transform: translateX(0);
  }
}

.menu {
  display: flex;
  flex-direction: column;
  color: #FFFFFF;
  font-size: 0.16rem;
  padding-top: 0.16rem;
}
</style>
