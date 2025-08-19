<template>
  <div id="app">
    <!-- Loading Transition Overlay -->
    <LoadingTransition 
      v-if="authStore.showTransition" 
      :user="authStore.user"
      @complete="authStore.completeTransition"
    />
    
    <!-- Main Application -->
    <router-view v-show="!authStore.showTransition"></router-view>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import LoadingTransition from '@/components/LoadingTransition.vue'

const authStore = useAuthStore()
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}


#app {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 100vh;
}
</style>
