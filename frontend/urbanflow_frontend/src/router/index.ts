import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import ControlHome from '@/views/control/ControlHome.vue'
import MapPage from '@/views/Map/MapPage.vue'
import LoginLogin from '@/views/login/LoginLogin.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'LoginLogin',
    component: LoginLogin
  },
  {
    path: '/',
    redirect: '/control'
  },
  {
    path: '/control',
    name: 'ControlHome',
    component: ControlHome
  },
  {
  path: '/map',
  name: 'MapEdge',
  component: MapPage
}

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
