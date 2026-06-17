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
        <div :class="['conn-badge', connectionOk ? 'conn-ok' : 'conn-bad']">
          <span class="conn-dot"></span>
          {{ connectionOk ? '连接正常' : '服务异常(显示缓存)' }}
        </div>
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

    <transition name="fade">
      <div v-if="errorBanner" class="error-banner">
        <span>⚠ {{ errorBanner }}</span>
        <button @click="errorBanner = ''">×</button>
      </div>
    </transition>

    <main class="dash-body">
      <section class="map-section">
        <div class="section-title">
          <span class="title-bar"></span>
          实时位置监控
          <span v-if="!connectionOk" class="stale-tag">数据可能延迟</span>
        </div>
        <div class="map-wrapper">
          <MapView :machines="machines" />
        </div>
      </section>

      <aside class="table-section">
        <MachineTable :machines="machines" @view-stats="openStats" />
      </aside>
    </main>

    <WorkStatsModal
      :visible="statsModal.visible"
      :machine-id="statsModal.machineId"
      :machine-name="statsModal.machineName"
      @close="statsModal.visible = false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import MapView from './components/MapView.vue'
import MachineTable from './components/MachineTable.vue'
import WorkStatsModal from './components/WorkStatsModal.vue'
import { fetchMachines, fetchHealth } from './api/machine.js'

const machines = ref([])
const currentTime = ref('')
const connectionOk = ref(true)
const errorBanner = ref('')
const consecutiveFails = ref(0)

const statsModal = reactive({
  visible: false,
  machineId: '',
  machineName: ''
})

function openStats(machine) {
  statsModal.machineId = machine.machineId
  statsModal.machineName = machine.machineName
  statsModal.visible = true
}
let refreshTimer = null
let clockTimer = null

const onlineCount = computed(() => machines.value.filter(m => m.status === 'ONLINE').length)
const totalArea = computed(() => {
  const total = machines.value.reduce((sum, m) => sum + (m.todayArea || 0), 0)
  return Math.round(total * 100) / 100
})

async function loadData() {
  try {
    const fresh = await fetchMachines()
    if (Array.isArray(fresh) && fresh.length > 0) {
      machines.value = fresh
    }
    consecutiveFails.value = 0
    connectionOk.value = true
  } catch (e) {
    consecutiveFails.value++
    if (consecutiveFails.value >= 2) {
      connectionOk.value = false
      errorBanner.value = `后端请求连续失败 ${consecutiveFails.value} 次, 地图保留最后一次位置 (${e.message})`
    }
  }
}

async function checkHealth() {
  try {
    await fetchHealth()
    if (!connectionOk.value) {
      connectionOk.value = true
      errorBanner.value = ''
      consecutiveFails.value = 0
    }
  } catch {}
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
  setInterval(checkHealth, 3000)
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

.conn-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  border: 1px solid;
}

.conn-badge.conn-ok {
  color: #00e676;
  border-color: rgba(0, 230, 118, 0.4);
  background: rgba(0, 230, 118, 0.08);
}

.conn-badge.conn-bad {
  color: #ff9800;
  border-color: rgba(255, 152, 0, 0.4);
  background: rgba(255, 152, 0, 0.08);
  animation: warn-blink 1.5s ease-in-out infinite;
}

@keyframes warn-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.conn-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.conn-ok .conn-dot {
  background: #00e676;
  box-shadow: 0 0 8px rgba(0, 230, 118, 0.8);
}

.conn-bad .conn-dot {
  background: #ff9800;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.8);
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

.error-banner {
  background: linear-gradient(90deg, rgba(255, 87, 34, 0.2), rgba(255, 152, 0, 0.1));
  border-bottom: 1px solid rgba(255, 87, 34, 0.4);
  padding: 8px 28px;
  font-size: 13px;
  color: #ffab91;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.error-banner button {
  background: none;
  border: none;
  color: #ffab91;
  font-size: 18px;
  cursor: pointer;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity .3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
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

.stale-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
  letter-spacing: 0;
  margin-left: 6px;
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
