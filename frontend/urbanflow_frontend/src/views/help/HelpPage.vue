<template>
  <div class="help-page" :class="{ 'nav-expanded': isNavVisible }">
    <ControlHeader
      @toggle-nav="toggleNav"
      @toggle-record="toggleRecord"
      @toggle-emergency="toggleEmergency"
      @toggle-priority="togglePriority"
      @mode-changed="handleModeChange"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area" :class="{ 'nav-expanded': isNavVisible }">
      <div class="help-container">
        <div class="page-header">
          <h1>{{ pageTitle }}</h1>
        </div>

        <div class="help-content">
          <div v-if="loading" class="loading">
            <p>Loading help documentation...</p>
          </div>
          <div v-else-if="error" class="error">
            <p>Loading failed: {{ error }}</p>
            <button @click="loadMarkdown">Retry</button>
          </div>
          <div v-else class="markdown-body" v-html="renderedMarkdown"></div>
        </div>
      </div>
    </div>


    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)


const renderedMarkdown = ref('')
const loading = ref(true)
const error = ref('')

const pageTitle = computed(() => {
  const role = authStore.user?.role
  switch (role) {
    case 'ADMIN':
    case 'Admin':
    case 'admin':
      return 'Help & Documentation - Administrator'
    case 'Traffic Manager':
      return 'Help & Documentation - Traffic Operator'
    case 'Traffic Planner':
      return 'Help & Documentation - Urban Planner'
    default:
      return 'Help & Documentation'
  }
})



const loadMarkdown = async () => {
  loading.value = true
  error.value = ''

  try {
    const { marked } = await import('marked')

    // 简化配置，只保留必要的
    marked.setOptions({
      gfm: true,
      breaks: false,  // 改为false，避免干扰
      sanitize: false
    })

    const role = authStore.user?.role

    let helpContent = ''

    if (role === 'ADMIN' || role === 'Admin' || role === 'admin') {
      const helpModule = await import('@/assets/help_Admin.md?raw')
      helpContent = helpModule.default
    } else if (role === 'Traffic Manager') {
      const helpModule = await import('@/assets/help_Manager.md?raw')
      helpContent = helpModule.default
    } else if (role === 'Traffic Planner') {
      const helpModule = await import('@/assets/help_Planner.md?raw')
      helpContent = helpModule.default
    } else {
      const helpModule = await import('@/assets/help_Admin.md?raw')
      helpContent = helpModule.default
    }

    renderedMarkdown.value = marked(helpContent)

    // 调试：检查是否包含strong标签
    console.log('渲染的HTML是否包含strong标签:', renderedMarkdown.value.includes('<strong>'))
    if (renderedMarkdown.value.includes('<strong>')) {
      const strongMatches = renderedMarkdown.value.match(/<strong>(.*?)<\/strong>/g)
      console.log('找到的strong标签:', strongMatches?.slice(0, 5)) // 只显示前5个
    }

    loading.value = false
  } catch (err) {
    console.error('Failed to load Markdown:', err)
    error.value = `Loading failed: ${err}`
    loading.value = false

  }
}

onMounted(() => {
  loadMarkdown()
})

watch(
  () => authStore.user?.role,
  (newRole, oldRole) => {
    if (newRole !== oldRole && newRole) {
      loadMarkdown()
    }
  }
)

const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
  if (isRecordVisible.value) {
    isEmergencyVisible.value = false
    isPriorityVisible.value = false
  }
}

const toggleEmergency = () => {
  isEmergencyVisible.value = !isEmergencyVisible.value
  if (isEmergencyVisible.value) {
    isRecordVisible.value = false
    isPriorityVisible.value = false
  }
}

const togglePriority = () => {
  isPriorityVisible.value = !isPriorityVisible.value
  if (isPriorityVisible.value) {
    isRecordVisible.value = false
    isEmergencyVisible.value = false
  }
}

const handleModeChange = (isAI: boolean) => {
  console.log('Mode changed to:', isAI ? 'AI Mode' : 'Manual Mode')
}

const handleSignOut = () => {
  localStorage.removeItem('authToken')
  router.push({ name: 'Login' })
}
</script>

<style scoped lang="scss">
.help-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #FFFFFF;
  background-color: #1E1E2F;
  z-index: 1;

  * {
    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #252A3F;
      border-radius: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #00e3ff;
      border-radius: 4px;
      transition: all 0.3s ease;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #00b8d4;
      transform: scale(1.1);
    }

    &::-webkit-scrollbar-thumb:active {
      background: #0097a7;
    }

    &::-webkit-scrollbar-corner {
      background: #252A3F;
    }
  }
}

.main-area {
  height: calc(100% - 0.64rem);
  display: flex;
  overflow: hidden;
  transition: all 0.3s ease;
  margin: 0;
  padding: 0;
  width: 100%;

  &.nav-expanded {
    margin-left: 1.2rem;
  }
}

.help-container {
  width: 100%;
  padding: .2rem;
  color: #FFFFFF;
  background-color: #1E1E2F;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #252A3F;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #00e3ff;
    border-radius: 4px;
    transition: background 0.3s ease;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #00b8d4;
  }

  &::-webkit-scrollbar-thumb:active {
    background: #0097a7;
  }

  // Firefox 滚动条样式
  scrollbar-width: thin;
  scrollbar-color: #00e3ff #252A3F;
}

.page-header {
  margin-bottom: .2rem;
  width: 100%;
  max-width: 1200px;
  text-align: center;
  padding: 0 .2rem;
}

.page-header h1 {
  color: #FFFFFF;
  margin-bottom: 0.05rem;
  font-size: .35rem;
}

.page-header p {
  color: #FFFFFF;
  font-size: .2rem;
}

.help-content {
  width: 100%;
  max-width: 1200px;
  padding: .1rem;
  display: flex;
  justify-content: center;
}

.loading, .error {
  text-align: center;
  padding: .2rem;
  width: 100%;
  max-width: 1000px;
}

.loading {
  color: #00e3ff;
}

.error {
  color: #ff6b6b;
}

.error button {
  background: #00e3ff;
  color: #1E1E2F;
  border: none;
  padding: .08rem .16rem;
  border-radius: 4px;
  cursor: pointer;
  margin-top: .08rem;
}

.error button:hover {
  background: #00b8d4;
}

.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: .15rem;
  line-height: 1.6;
  color: #FFFFFF;
  background-color: #252A3F;
  padding: .2rem;
  border-radius: 8px;
  width: 100%;
  max-width: 1000px;
  text-align: left;
  word-wrap: break-word;
  overflow-wrap: break-word;
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #1a1a2e;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #00e3ff;
    border-radius: 3px;
    transition: background 0.3s ease;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #00b8d4;
  }

  scrollbar-width: thin;
  scrollbar-color: #00e3ff #1a1a2e;
}

.markdown-body h1 {
  color: #00e3ff;
  font-size: .2rem;
  font-weight: 600;
  padding-bottom: .08rem;
  border-bottom: 2px solid #00e3ff;
  margin-bottom: .15rem;
  margin-top: 0;
}

.markdown-body h2 {
  color: #FFFFFF;
  font-size: .2rem;
  font-weight: 600;
  margin-top: .2rem;
  margin-bottom: .1rem;
  padding-left: .08rem;
  border-left: 3px solid #00e3ff;
}

.markdown-body h3 {
  color: #FFFFFF;
  font-size: .18rem;
  font-weight: 600;
  margin-top: .15rem;
  margin-bottom: .08rem;
}

.markdown-body p {
  margin-bottom: .12rem;
  text-align: left;
  color: #FFFFFF;
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-body ul, .markdown-body ol {
  margin-bottom: .12rem;
  padding-left: .15rem;
  color: #FFFFFF;
}

.markdown-body li {
  margin-bottom: .04rem;
  color: #FFFFFF;
}

.markdown-body strong {
  color: #00e3ff;
  font-weight: bold;
}

.markdown-body b {
  color: #00e3ff;
  font-weight: bold;
}

.markdown-body code {
  background-color: #1a1a2e;
  color: #00e3ff;
  padding: .02rem .04rem;
  border-radius: 3px;
  font-size: .15rem;
  font-family: 'Courier New', monospace;
}

.markdown-body pre {
  background-color: #1a1a2e;
  padding: .12rem;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: .12rem;
  border: 1px solid #3a3a5c;
  white-space: pre;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.5;
}

.markdown-body pre code {
  background: none;
  padding: 0;
  color: #FFFFFF;
}

.markdown-body blockquote {
  border-left: 3px solid #00e3ff;
  padding: 0 .12rem;
  color: #b0b0b0;
  margin: .12rem 0;
  background-color: #1e1e2f;
  border-radius: 0 4px 4px 0;
}

.markdown-body hr {
  border: none;
  height: 1px;
  background-color: #3a3a5c;
  margin: .15rem 0;
}

.markdown-body a {
  color: #00e3ff;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: .12rem 0;
}

.markdown-body th, .markdown-body td {
  border: 1px solid #3a3a5c;
  padding: .06rem .08rem;
  text-align: left;
}

.markdown-body th {
  background-color: #1a1a2e;
  color: #00e3ff;
  font-weight: 600;
}

.markdown-body td {
  color: #FFFFFF;
}

.markdown-body img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: .08rem 0;
}

.markdown-body ul ul, .markdown-body ol ol, .markdown-body ul ol, .markdown-body ol ul {
  margin-top: .04rem;
  margin-bottom: .04rem;
}

.markdown-body li > p {
  margin-bottom: .04rem;
}

.markdown-body li > *:last-child {
  margin-bottom: 0;
}

.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4, .markdown-body h5, .markdown-body h6 {
  line-height: 1.25;
  margin-top: .2rem;
  margin-bottom: .1rem;
}

.markdown-body h1:first-child, .markdown-body h2:first-child, .markdown-body h3:first-child {
  margin-top: 0;
}

.markdown-body kbd {
  background-color: #1a1a2e;
  border: 1px solid #3a3a5c;
  border-bottom-color: #4a4a6c;
  border-radius: 3px;
  box-shadow: inset 0 -1px 0 #4a4a6c;
  color: #00e3ff;
  display: inline-block;
  font-size: .15rem;
  font-family: 'Courier New', monospace;
  line-height: 1;
  padding: .02rem .04rem;
  vertical-align: middle;
}

.markdown-body mark {
  background-color: rgba(0, 227, 255, 0.2);
  color: #FFFFFF;
  padding: .02rem;
}


.markdown-body pre code[class*="language-"] {
  color: #FFFFFF;
}

.markdown-body code[class*="language-json"] {
  color: #98c379;
}

.markdown-body code[class*="language-javascript"],
.markdown-body code[class*="language-js"] {
  color: #e5c07b;
}

.markdown-body code[class*="language-bash"],
.markdown-body code[class*="language-shell"] {
  color: #61afef;
}

.markdown-body code[class*="language-html"] {
  color: #e06c75;
}

.markdown-body code[class*="language-css"] {
  color: #56b6c2;
}


.markdown-body table {
  border-spacing: 0;
  border-collapse: separate;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #3a3a5c;
}

.markdown-body table tr:nth-child(even) {
  background-color: rgba(26, 26, 46, 0.5);
}

.markdown-body table tr:hover {
  background-color: rgba(0, 227, 255, 0.1);
}

.markdown-body blockquote p:last-child {
  margin-bottom: 0;
}

.markdown-body blockquote::before {
  content: '\201C';
  color: #00e3ff;
  font-size: .3rem;
  position: absolute;
  margin-left: -.15rem;
  margin-top: -.05rem;
}

.markdown-body blockquote {
  position: relative;
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0;
  }

  .help-container {
    padding: .1rem;
  }

  .page-header {
    padding: 0 .1rem;
  }

  .help-content {
    padding: .05rem;
  }

  .markdown-body {
    padding: .15rem;
    font-size: .2rem;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: .25rem;
  }

  .page-header p {
    font-size: .13rem;
  }

  .markdown-body {
    font-size: .1rem;
    padding: .1rem;
  }

  .markdown-body h1 {
    font-size: .2rem;
  }

  .markdown-body h2 {
    font-size: .18rem;
  }

  .markdown-body h3 {
    font-size: .16rem;
  }
}
</style>
