<template>
  <div class="dashboard">
    <header class="dash-header">
      <div class="header-left">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#00e676" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 2 L12 6 M12 18 L12 22 M2 12 L6 12 M18 12 L22 12"/>
          </svg>
        </div>
        <h1 class="header-title">智慧农业 · 北斗农机管理系统</h1>
      </div>
      <div class="header-right">
        <div class="stat-item">
          <span class="stat-value">{{ onlineCount }}</span>
          <span class="stat-label">在线农机</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ totalArea }}</span>
          <span class="stat-label">今日总作业(亩)</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value time-value">{{ currentTime }}</span>
          <span class="stat-label">系统时间</span>
        </div>
      </div>
    </header>

    <main class="dash-body">
      <section class="map-section">
        <div class="section-title">
          <span class="title-bar"></span>
          实时位置监控
        </div>
        <div class="map-wrapper">
          <MapView :machines="machines" />
        </div>
      </section>

      <aside class="table-section">
        <MachineTable :machines="machines" />
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import MapView from './components/MapView.vue'
import MachineTable from './components/MachineTable.vue'
import { fetchMachines } from './api/machine.js'

const machines = ref([])
const currentTime = ref('')
let refreshTimer = null
let clockTimer = null

const onlineCount = computed(() => machines.value.filter(m => m.status === 'ONLINE').length)
const totalArea = computed(() => {
  const total = machines.value.reduce((sum, m) => sum + (m.todayArea || 0), 0)
  return Math.round(total * 100) / 100
})

async function loadData() {
  try {
    machines.value = await fetchMachines()
  } catch (e) {
    console.error('加载农机数据失败:', e)
  }
}

function updateTime() {
  const now = new Date()
  const y = now.getFullYear()
  const mo = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${mo}-${d} ${h}:${mi}:${s}`
}

onMounted(() => {
  loadData()
  updateTime()
  refreshTimer = setInterval(loadData, 5000)
  clockTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #0a1628;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>

<style scoped>
.dashboard {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(0, 230, 118, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(0, 150, 255, 0.04) 0%, transparent 60%),
    linear-gradient(180deg, #0a1628 0%, #0d1f3c 50%, #0a1628 100%);
}

.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  height: 64px;
  background: rgba(6, 30, 60, 0.9);
  border-bottom: 1px solid rgba(0, 230, 118, 0.2);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 230, 118, 0.1);
  border-radius: 10px;
  border: 1px solid rgba(0, 230, 118, 0.3);
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(90deg, #00e676, #69f0ae);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #00e676;
  line-height: 1.2;
}

.time-value {
  font-size: 16px;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 1px;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(0, 230, 118, 0.2);
}

.dash-body {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  min-height: 0;
}

.map-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.title-bar {
  width: 3px;
  height: 16px;
  background: #00e676;
  border-radius: 2px;
}

.map-wrapper {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(0, 230, 118, 0.15);
  box-shadow: 0 0 30px rgba(0, 230, 118, 0.05);
}

.table-section {
  width: 520px;
  flex-shrink: 0;
}
</style>
