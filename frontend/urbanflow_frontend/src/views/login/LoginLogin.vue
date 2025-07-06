<template>
  <div class="login-page">
    <div class="login-box">
      <div class="logo">logo</div>
      <div class="system-title">URBANFLOW SYSTEM</div>

      <div class="input-group">
        <input
          v-model="loginForm.userId"
          type="text"
          placeholder="User ID"
        />
      </div>

      <div class="input-group">
        <input
          v-model="loginForm.password"
          type="password"
          placeholder="Password"
        />
      </div>

      <button
        class="login-button"
        @click="handleLogin"
        :disabled="!loginForm.userId || !loginForm.password"
      >
        LOGIN
      </button>

      <div class="forgot" @click="goToForgot">Forgot password?</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loginForm = ref({
  userId: '',
  password: ''
})


const handleLogin = () => {

  if (loginForm.value.userId && loginForm.value.password) {



    localStorage.setItem('authToken', 'valid-user-token')

    router.push({ name: 'Control' })
  }
}

const goToForgot = () => {
  router.push({ name: 'ForgetPassword' })
}
</script>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/images/LoginBg.png');
  background-size: cover;
  background-repeat: no-repeat;
}


.login-box {
  width: 4.99rem;
  height: 5.70rem;
  padding: .5rem 0.73rem;
  background-color: #1E1E2F;
  border-radius: 0.24rem;
  box-shadow: 0 0.1rem 0.2rem rgba(0, 0, 0, 0.2);
  color: #fff;
  text-align: center;
}


.logo {
  font-size: 0.3rem;
  margin-bottom: 0.2rem;
}


.system-title {
  font-size: 0.3rem;
  font-weight: bold;
  color: #00e3ff;
  margin-bottom: 0.4rem;
}


.input-group {
  position: relative;
  margin-bottom: 0.3rem;
  border: 0.01rem solid #00e3ff;
}


.input-group input {
  width: 100%;
  padding: 0.12rem 0.1rem 0.12rem 0.35rem;
  border: none;
  background-color: #404060;
  color: #fff;
  font-size: 0.2rem;
  outline: none;
}


.login-button {
  width: 100%;
  padding: 0.14rem;
  background-color: #00e3ff;
  color: #FFFFFF;
  font-weight: bold;
  border: none;
  margin-top: 0.2rem;
  cursor: pointer;
  transition: background-color 0.3s;
  font-size: 0.18rem;
}

.login-button:disabled {
  background-color: #666;
  cursor: not-allowed;
}


.login-button:hover:not(:disabled) {
  background-color: #00bcd4;
}


.forgot {
  margin-top: 0.2rem;
  font-size: 0.2rem;
  color: #FFFFFF;
  cursor: pointer;
}
</style>
