<template>
  <div class="loading-transition">
    <!-- Tech Background -->
    <div class="tech-background">
      <div class="grid-overlay"></div>
      <div class="floating-particles">
        <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
      <div class="circuit-lines">
        <div class="line line-1"></div>
        <div class="line line-2"></div>
        <div class="line line-3"></div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="loading-content">
      <!-- Logo Section -->
      <div class="logo-section">
        <div class="logo-circle" :class="{ 'animate': logoAnimated }">
          <div class="logo-inner">
            <div class="logo-rings">
              <div class="ring ring-1"></div>
              <div class="ring ring-2"></div>
              <div class="ring ring-3"></div>
            </div>
            <span class="logo-text">UF</span>
          </div>
        </div>
        <h1 class="system-title" :class="{ 'show': titleShow }">UrbanFlow</h1>
        <p class="system-subtitle" :class="{ 'show': subtitleShow }">Smart Traffic Management System</p>
      </div>

      <!-- User Info Card -->
      <Transition name="slide-up" appear>
        <div v-if="userCardShow" class="user-card">
          <div class="user-avatar">
            <span class="avatar-text">{{ userInitial }}</span>
            <div class="avatar-ring"></div>
          </div>
          <div class="user-info">
            <h3 class="user-name">Welcome back, {{ user?.userName || 'User' }}</h3>
            <p class="user-role">{{ user?.role || 'Guest' }}</p>
            <p class="user-account">Account: {{ user?.accountNumber || 'N/A' }}</p>
            <div v-if="user?.managedAreas?.length" class="managed-areas">
              <span class="areas-label">Managed Areas:</span>
              <div class="areas-container">
                <span
                  v-for="(area, index) in user.managedAreas"
                  :key="index"
                  class="area-tag"
                  :style="{ animationDelay: `${index * 200}ms` }"
                >
                  {{ area }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Progress Section -->
      <div class="progress-section" :class="{ 'show': progressShow }">
        <div class="loading-text">
          <span class="stage-text">{{ currentStageText }}</span>
          <div class="dots">
            <span v-for="i in 3" :key="i" class="dot" :style="{ animationDelay: `${i * 200}ms` }"></span>
          </div>
        </div>

        <div class="progress-bar">
          <div class="progress-track">
            <div
              class="progress-fill"
              :style="{ width: `${progress}%` }"
            ></div>
            <div class="progress-glow" :style="{ left: `${progress}%` }"></div>
          </div>
          <div class="progress-percentage">{{ Math.round(progress) }}%</div>
        </div>

        <!-- Stage Indicators -->
        <div class="stage-indicators">
          <div
            v-for="(stage, index) in loadingStages"
            :key="index"
            class="stage-indicator"
            :class="{
              'completed': index < currentStage,
              'active': index === currentStage,
              'pending': index > currentStage
            }"
          >
            <div class="stage-icon">
              <div v-if="index < currentStage" class="check-icon">✓</div>
              <div v-else-if="index === currentStage" class="loading-spinner"></div>
              <div v-else class="pending-dot"></div>
            </div>
            <span class="stage-label">{{ stage.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Success Animation -->
    <Transition name="success-fade" appear>
      <div v-if="showSuccess" class="success-overlay">
        <div class="success-circle">
          <div class="check-mark">
            <div class="check-line check-line-1"></div>
            <div class="check-line check-line-2"></div>
          </div>
        </div>
        <h2 class="success-text">Access Granted</h2>
        <p class="success-subtitle">Redirecting to your dashboard...</p>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineProps, defineEmits } from 'vue'

interface User {
  id: number
  accountNumber: string
  userName: string
  email: string
  role: string
  enabled: boolean
  managedAreas?: string[]
}

interface Props {
  user: User | null
  duration?: number
}

const props = withDefaults(defineProps<Props>(), {
  duration: 5000
})

const emit = defineEmits<{
  (e: 'complete'): void
}>()

// Reactive data
const progress = ref(0)
const currentStage = ref(0)
const logoAnimated = ref(false)
const titleShow = ref(false)
const subtitleShow = ref(false)
const userCardShow = ref(false)
const progressShow = ref(false)
const showSuccess = ref(false)

// Loading stages
const loadingStages = [
  { text: 'Verifying Identity', duration: 1000 },
  { text: 'Loading Permissions', duration: 1500 },
  { text: 'Initializing System', duration: 1200 },
  { text: 'Preparing Dashboard', duration: 1300 }
]

// Computed
const userInitial = computed(() => {
  return props.user?.userName?.charAt(0).toUpperCase() || 'U'
})

const currentStageText = computed(() => {
  return loadingStages[currentStage.value]?.text || 'Complete'
})

// Methods
const getParticleStyle = (index: number) => {
  const angle = (index * 18) % 360
  const distance = 100 + (index * 20) % 200
  const duration = 10 + (index * 2) % 20

  return {
    '--angle': `${angle}deg`,
    '--distance': `${distance}px`,
    '--duration': `${duration}s`,
    '--delay': `${index * 0.5}s`
  }
}

const startAnimation = () => {
  // Logo animation
  setTimeout(() => {
    logoAnimated.value = true
  }, 300)

  // Title animations
  setTimeout(() => {
    titleShow.value = true
  }, 800)

  setTimeout(() => {
    subtitleShow.value = true
  }, 1200)

  // User card
  setTimeout(() => {
    userCardShow.value = true
  }, 1600)

  // Progress section
  setTimeout(() => {
    progressShow.value = true
  }, 2000)

  // Start progress and stages
  setTimeout(() => {
    startProgress()
  }, 2400)
}

const startProgress = () => {
  let totalDuration = 0
  let currentProgress = 0

  // Calculate total duration
  loadingStages.forEach(stage => {
    totalDuration += stage.duration
  })

  // Start stage progression
  loadingStages.forEach((stage, index) => {
    setTimeout(() => {
      currentStage.value = index
    }, currentProgress)
    currentProgress += stage.duration
  })

  // Progress bar animation
  const progressInterval = setInterval(() => {
    progress.value += (100 / totalDuration) * 50 // Update every 50ms

    if (progress.value >= 100) {
      clearInterval(progressInterval)
      progress.value = 100

      // Show success
      setTimeout(() => {
        showSuccess.value = true

        // Complete after success animation
        setTimeout(() => {
          emit('complete')
        }, 2000)
      }, 500)
    }
  }, 50)
}

// Lifecycle
onMounted(() => {
  startAnimation()
})
</script>

<style scoped lang="scss">
.loading-transition {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #0a0e1a 0%, #1a1f2e 25%, #2a2f3e 50%, #1a1f2e 75%, #0a0e1a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  z-index: 9999;
}

// Tech Background
.tech-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(0, 180, 216, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 180, 216, 0.1) 1px, transparent 1px);
  background-size: .06rem .06rem;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(60px, 60px); }
}

.floating-particles {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 4px;
  background: radial-gradient(circle, #00E5FF 0%, transparent 70%);
  border-radius: 50%;
  animation: particleFloat var(--duration, 15s) linear infinite;
  animation-delay: var(--delay, 0s);
  opacity: 0.7;
}

@keyframes particleFloat {
  0% {
    transform: translate(-50%, -50%) rotate(0deg) translateX(var(--distance, 100px)) rotate(0deg);
    opacity: 0;
  }
  10% { opacity: 0.7; }
  90% { opacity: 0.7; }
  100% {
    transform: translate(-50%, -50%) rotate(var(--angle, 360deg)) translateX(var(--distance, 100px)) rotate(calc(-1 * var(--angle, 360deg)));
    opacity: 0;
  }
}

.circuit-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.line {
  position: absolute;
  background: linear-gradient(90deg, transparent 0%, #00B4D8 50%, transparent 100%);
  opacity: 0.3;
  animation: linePulse 4s ease-in-out infinite;
}

.line-1 {
  top: 20%;
  left: 0;
  width: 100%;
  height: 2px;
  animation-delay: 0s;
}

.line-2 {
  top: 0;
  right: 30%;
  width: 2px;
  height: 100%;
  background: linear-gradient(0deg, transparent 0%, #00B4D8 50%, transparent 100%);
  animation-delay: 1.5s;
}

.line-3 {
  bottom: 25%;
  left: 0;
  width: 100%;
  height: 2px;
  animation-delay: 3s;
}

@keyframes linePulse {
  0%, 100% { opacity: 0.1; transform: scaleX(0.5); }
  50% { opacity: 0.6; transform: scaleX(1); }
}

.loading-content {
  position: relative;
  z-index: 10;
  text-align: center;
  max-width: 320px;
  width: 90%;
  padding: 0 0.5rem;
}

.logo-section {
  margin-bottom: 1.5rem;
}

.logo-circle {
  width: 70px;
  height: 70px;
  margin: 0 auto 0.8rem;
  position: relative;
  transform: scale(0.8);
  opacity: 0;
  transition: all 1s cubic-bezier(0.4, 0, 0.2, 1);

  &.animate {
    transform: scale(1);
    opacity: 1;
  }
}

.logo-inner {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow:
    0 0 30px rgba(0, 229, 255, 0.5),
    inset 0 1px 6px rgba(255, 255, 255, 0.3);
}

.logo-rings {
  position: absolute;
  top: -20px;
  left: -20px;
  right: -20px;
  bottom: -20px;
}

.ring {
  position: absolute;
  border: 2px solid;
  border-radius: 50%;
  animation: ringRotate 6s linear infinite;
}

.ring-1 {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-color: rgba(0, 229, 255, 0.3);
  animation-duration: 8s;
}

.ring-2 {
  top: 10px;
  left: 10px;
  right: 10px;
  bottom: 10px;
  border-color: rgba(0, 180, 216, 0.4);
  animation-duration: 12s;
  animation-direction: reverse;
}

.ring-3 {
  top: 20px;
  left: 20px;
  right: 20px;
  bottom: 20px;
  border-color: rgba(0, 229, 255, 0.2);
  animation-duration: 16s;
}

@keyframes ringRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.logo-text {
  font-size: 1.3rem;
  font-weight: 900;
  color: #FFFFFF;
  text-shadow: 0 0 15px rgba(255, 255, 255, 0.8);
  position: relative;
  z-index: 5;
}

.system-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: #00E5FF;
  margin: 0.5rem 0 0.3rem;
  text-shadow: 0 0 20px rgba(0, 229, 255, 0.6);
  transform: translateY(15px);
  opacity: 0;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);

  &.show {
    transform: translateY(0);
    opacity: 1;
  }
}

.system-subtitle {
  font-size: 0.8rem;
  color: #B3E5FC;
  font-weight: 300;
  letter-spacing: 1px;
  transform: translateY(15px);
  opacity: 0;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.2s;

  &.show {
    transform: translateY(0);
    opacity: 1;
  }
}

.user-card {
  background: linear-gradient(135deg, rgba(30, 33, 47, 0.9) 0%, rgba(42, 45, 74, 0.9) 100%);
  border: 1px solid rgba(0, 180, 216, 0.3);
  border-radius: 12px;
  padding: 1rem;
  margin: 1rem 0;
  backdrop-filter: blur(8px);
  box-shadow:
    0 6px 24px rgba(0, 0, 0, 0.3),
    0 0 15px rgba(0, 180, 216, 0.1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent 0%, #00E5FF 50%, transparent 100%);
  }
}

.user-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
  border-radius: 50%;
  margin: 0 auto 0.6rem;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: 0 0 15px rgba(0, 229, 255, 0.4);
}

.avatar-text {
  font-size: 1rem;
  font-weight: 700;
  color: #FFFFFF;
}

.avatar-ring {
  position: absolute;
  top: -5px;
  left: -5px;
  right: -5px;
  bottom: -5px;
  border: 2px solid rgba(0, 229, 255, 0.3);
  border-radius: 50%;
  animation: ringPulse 3s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.1); opacity: 0.6; }
}

.user-name {
  font-size: 1rem;
  color: #FFFFFF;
  margin: 0.3rem 0;
  font-weight: 600;
}

.user-role {
  font-size: 0.8rem;
  color: #00E5FF;
  margin: 0.2rem 0;
  font-weight: 500;
}

.user-account {
  font-size: 0.7rem;
  color: #B3E5FC;
  margin: 0.2rem 0 0.6rem;
}

.managed-areas {
  text-align: left;
}

.areas-label {
  font-size: 0.9rem;
  color: #B3E5FC;
  display: block;
  margin-bottom: 0.5rem;
}

.areas-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.area-tag {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.2) 0%, rgba(0, 180, 216, 0.2) 100%);
  border: 1px solid rgba(0, 229, 255, 0.4);
  color: #00E5FF;
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 500;
  animation: tagFadeIn 0.6s ease-out forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes tagFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Progress Section
.progress-section {
  transform: translateY(30px);
  opacity: 0;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);

  &.show {
    transform: translateY(0);
    opacity: 1;
  }
}

.loading-text {
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.stage-text {
  font-size: 1.1rem;
  color: #E3F2FD;
  font-weight: 500;
}

.dots {
  display: flex;
  gap: 0.2rem;
}

.dot {
  width: 4px;
  height: 4px;
  background: #00E5FF;
  border-radius: 50%;
  animation: dotBounce 1.4s ease-in-out infinite;
}

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1.2); opacity: 1; }
}

.progress-bar {
  margin-bottom: 2rem;
  position: relative;
}

.progress-track {
  width: 100%;
  height: 6px;
  background: rgba(30, 33, 47, 0.8);
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(0, 180, 216, 0.2);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00E5FF 0%, #00B4D8 50%, #00E5FF 100%);
  border-radius: 10px;
  transition: width 0.3s ease;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.3) 50%, transparent 100%);
    animation: progressShine 2s ease-in-out infinite;
  }
}

@keyframes progressShine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-glow {
  position: absolute;
  top: -2px;
  width: 10px;
  height: 10px;
  background: #00FFFF;
  border-radius: 50%;
  box-shadow: 0 0 15px #00FFFF;
  transform: translateX(-50%);
  transition: left 0.3s ease;
}

.progress-percentage {
  text-align: center;
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #00E5FF;
  font-weight: 600;
}

// Stage Indicators
.stage-indicators {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
}

.stage-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;

  &.completed .stage-icon {
    background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%);
    color: #FFFFFF;
    transform: scale(1.1);
  }

  &.active .stage-icon {
    background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
    color: #FFFFFF;
    animation: pulse 2s ease-in-out infinite;
  }

  &.pending .stage-icon {
    background: rgba(113, 128, 150, 0.3);
    border: 2px solid rgba(113, 128, 150, 0.5);
  }
}

.stage-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.3rem;
  transition: all 0.3s ease;
  position: relative;
}

.check-icon {
  font-size: 1rem;
  font-weight: 700;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #FFFFFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pending-dot {
  width: 8px;
  height: 8px;
  background: rgba(113, 128, 150, 0.6);
  border-radius: 50%;
}

.stage-label {
  font-size: 0.8rem;
  color: #B3E5FC;
  text-align: center;
  max-width: 80px;
  line-height: 1.2;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

// Success Overlay
.success-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.success-circle {
  width: 100px;
  height: 100px;
  border: 3px solid #4CAF50;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
  animation: successGlow 2s ease-in-out infinite;
}

@keyframes successGlow {
  0%, 100% { box-shadow: 0 0 20px rgba(76, 175, 80, 0.5); }
  50% { box-shadow: 0 0 40px rgba(76, 175, 80, 0.8); }
}

.check-mark {
  position: relative;
  width: 40px;
  height: 40px;
}

.check-line {
  position: absolute;
  background: #4CAF50;
  border-radius: 2px;
  transform-origin: left center;
}

.check-line-1 {
  width: 15px;
  height: 3px;
  top: 20px;
  left: 8px;
  transform: rotate(45deg);
  animation: checkDraw1 0.6s ease-out 0.3s forwards;
  transform: rotate(45deg) scaleX(0);
}

.check-line-2 {
  width: 25px;
  height: 3px;
  top: 17px;
  left: 18px;
  transform: rotate(-45deg);
  animation: checkDraw2 0.6s ease-out 0.6s forwards;
  transform: rotate(-45deg) scaleX(0);
}

@keyframes checkDraw1 {
  to { transform: rotate(45deg) scaleX(1); }
}

@keyframes checkDraw2 {
  to { transform: rotate(-45deg) scaleX(1); }
}

.success-text {
  font-size: 2rem;
  color: #4CAF50;
  margin-bottom: 0.5rem;
  font-weight: 700;
  text-shadow: 0 0 20px rgba(76, 175, 80, 0.5);
}

.success-subtitle {
  font-size: 1rem;
  color: #B3E5FC;
  font-weight: 300;
}

// Transitions
.slide-up-enter-active {
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(50px);
}

.slide-up-enter-to {
  opacity: 1;
  transform: translateY(0);
}

.success-fade-enter-active {
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.success-fade-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.success-fade-enter-to {
  opacity: 1;
  transform: scale(1);
}

@media (max-width: 768px) {
  .loading-content {
    max-width: 280px;
    padding: 0 0.5rem;
  }

  .system-title {
    font-size: 1.3rem;
  }

  .system-subtitle {
    font-size: 0.7rem;
    letter-spacing: 0.5px;
  }

  .user-card {
    padding: 0.8rem;
  }

  .logo-circle {
    width: 60px;
    height: 60px;
  }

  .logo-text {
    font-size: 1.1rem;
  }
}

@media (max-width: 480px) {
  .loading-content {
    max-width: 240px;
    padding: 0 0.3rem;
  }

  .areas-container {
    justify-content: center;
  }

  .managed-areas {
    text-align: center;
  }

  .system-title {
    font-size: 1.1rem;
  }

  .user-name {
    font-size: 0.9rem;
  }

  .user-card {
    padding: 0.6rem;
  }

  .logo-circle {
    width: 50px;
    height: 50px;
  }

  .logo-text {
    font-size: 1rem;
  }
}
</style>
