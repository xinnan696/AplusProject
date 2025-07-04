<template>
  <div class="control-header">
    <div class="header_left">
      <div
        class="iconfont nav"
        @click="emit('toggle-nav')"
        title="Navigation"
      >&#xe660;</div>
      <div class="logo_box">
        <div class="logo">
<!--          <img src="C:\Users\x2357\Desktop\urbanflow_frontend\src\assets\images\LOGO.png" alt="">-->
          <img src="@/assets/images/logo.png" alt="">
        </div>
      </div>
    </div>
    <div class="header_right">
      <label class="switch toggle-absolute">
        <input type="checkbox">
        <span class="slider"></span>
      </label>
      <div class="iconfont record">&#xe683;</div>

      <div class="personal" @click="togglePanel">G</div>

      <div v-if="showPanel" class="user-panel">
  <div class="user-info">
    <div class="user-avatar">G</div>
    <div class="user-name">Yibing Guo</div>
    <div class="user-phone">1234567890</div>

  <div class="divider"></div>
  <button class="sign-out-btn">Sign Out</button>
  </div>
</div>
  </div>
    </div>
</template>

<script setup lang="ts">
const emit = defineEmits(['toggle-nav'])
import { ref, onMounted, onBeforeUnmount } from 'vue'

const showPanel = ref(false)

function togglePanel() {
  showPanel.value = !showPanel.value
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.personal') && !target.closest('.user-panel')) {
    showPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.control-header {
  position: relative;
  width: 100%;
  height: 0.64rem;
  background-color: #1E1E2F;
  border-bottom: 0.01rem solid #3A3A4C;
  z-index: 1000;
  display: flex;


  .header_left,
  .header_right {
    position: relative;
    height: 100%;
  }

  .header_left {
    width: 13.59rem;
  }

  .header_right {
    width: 5.61rem;
  }

  .iconfont {
    position: absolute;
    font-family: 'iconfont';
    font-size: 0.25rem;
    width: 0.33rem;
    height: 0.4rem;
    line-height: 0.4rem;
    color: #CFCFCF;
    text-align: center;
    cursor: pointer;
  }

  .nav {
    top: 0.13rem;
    left: 0.26rem;
  }


.switch {
  position: relative;
  display: inline-block;
  width: 0.5rem;
  height: 0.27rem;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #1E1E2F;
  border-radius: 0.68rem;
  transition: 0.4s;
  border: 1px solid #CFCFCF;
}

.slider::before {
  position: absolute;
  content: "";
  height: 0.27rem;
  width: 0.31rem;
  left: 0;
  top: 0;
  border: 1px solid #CFCFCF;
  background-color: #CFCFCF;
  border-radius: 50%;
  transition: 0.4s;
}

.switch input:checked + .slider {
  background-color: #00B4D8;
}

.switch input:checked + .slider::before {
  transform: translateX(0.185rem);
  background-color: #CFCFCF;
}

.toggle-absolute {
  position: absolute;
  top: 0.21rem;
  right: 2.58rem;
  z-index: 999;
}


  .record {
    top: 0.12rem;
    right: 1.45rem;
    font-size: 0.33rem;
  }


  .personal {
    position: absolute;
    width: 0.4rem;
    height: 0.4rem;
    background-color: #00B4D8;
    border-radius: 50%;
    top: 0.12rem;
    right: 0.25rem;
    font-size: 0.2rem;
    color: #FFFFFF;
    font-weight: bold;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    user-select: none;
}


  .user-panel {
  position: absolute;
  top: 0.64rem;
  right: 0.25rem;
  width: 2.8rem;
  height: 1.9rem;
  background-color: #2B2B3C;
  border: 1px solid #3A3A4C;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-info {
  margin-top: 0.16rem;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.user-avatar {
  width: 0.4rem;
  height: 0.4rem;
  background-color: #00bfff;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 0.2rem;
  font-weight: bold;
  color: #FFFFFF;
  margin-bottom: 0.16rem;
}


.user-name {
  height: 0.18rem;
  font-size: 0.16rem;
  font-weight: bold;
  color: #FFFFFF;
  margin-bottom: 0.16rem;
}


.user-phone {
  height: 0.16rem;
  font-size: 0.14rem;
  color: #F59A23;
  margin-bottom: 0.16rem;
}

.divider {
  width: 100%;
  height: 1px;
  background-color: #3a3a4c;
  margin-bottom: 0.13rem;
}


.sign-out-btn {
  width: 2.3rem;
  height: 0.25rem;
  background-color: #00B4D8;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s;
}

.sign-out-btn:hover {
  background-color: #00B4D8;
}
  .logo_box {
  position: absolute;
  top: 0.13rem;
  left: 0.8rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
}

.logo img {
  height: 0.9rem;
  width: auto;
  display: block;
}
}
</style>
