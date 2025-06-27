import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import '@/style/iconfont.css'
import '@/utils/rem.js'
import '@/style/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
console.log('App mounted: ', document.getElementById('app')?.getBoundingClientRect())
