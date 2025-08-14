import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import '@/style/iconfont.css'
import '@/utils/rem.js'
import '@/style/index.scss'
import '@/style/button-hover.scss'
import { autoCleanupOnStart } from '@/utils/clearMockData'
// import CommonInput from './components/common/CommonInput.vue'
// import CommonSelect from './components/common/CommonSelect.vue'

// 清理开发环境遗留的Mock数据
autoCleanupOnStart()

const app = createApp(App)

// app.component('CommonSelect', CommonSelect)
// app.component('CommonInput', CommonInput)
app.use(createPinia())
app.use(router)

app.mount('#app')
console.log('✅ UrbanFlow App mounted successfully')
