import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import '@/style/iconfont.css'
import '@/utils/rem.js'
import '@/style/index.scss'
import '@/style/button-hover.scss'
// import CommonInput from './components/common/CommonInput.vue'
// import CommonSelect from './components/common/CommonSelect.vue'

// dashboard页面分区模拟测试
// if (process.env.NODE_ENV === 'development') {
//   import('@/mocks/mockDashboardUserAreaApi');
//   console.log('API mocks have been activated for development.');
// }


const app = createApp(App)

// app.component('CommonSelect', CommonSelect)
// app.component('CommonInput', CommonInput)
app.use(createPinia())
app.use(router)

app.mount('#app')
console.log('App mounted: ', document.getElementById('app')?.getBoundingClientRect())
