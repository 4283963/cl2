<template>
  <transition name="modal">
    <div v-if="visible" class="modal-mask" @click.self="$emit('close')">
      <div class="modal-box">
        <header class="modal-header">
          <h3>
            <span class="header-icon">⏱</span>
            {{ machineName }} - 今日工时分布
          </h3>
          <button class="close-btn" @click="$emit('close')">×</button>
        </header>

        <div class="modal-body">
          <div class="legend">
            <span class="legend-item">
              <span class="legend-color work"></span>
              作业中 ({{ totalWork }} 分钟)
            </span>
            <span class="legend-item">
              <span class="legend-color idle"></span>
              怠速/休息 ({{ totalIdle }} 分钟)
            </span>
            <span class="legend-item idle-rate">
              偷懒率: <strong>{{ idleRate }}%</strong>
            </span>
          </div>

          <div v-if="loading" class="loading">
            <div class="spinner"></div>
            <span>加载中...</span>
          </div>

          <div v-else-if="!stats.length" class="empty">
            今日暂无工时数据
          </div>

          <div v-else class="chart-wrap">
            <svg :width="chartWidth" :height="chartHeight" class="chart">
              <defs>
                <linearGradient id="workGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#00e676"/>
                  <stop offset="100%" stop-color="#00a86b"/>
                </linearGradient>
                <linearGradient id="idleGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#ff9800"/>
                  <stop offset="100%" stop-color="#e65100"/>
                </linearGradient>
              </defs>

              <g class="grid">
                <line v-for="g in gridLines" :key="g.y"
                      :x1="chartPaddingLeft" :x2="chartWidth - chartPaddingRight"
                      :y1="g.y" :y2="g.y"
                      stroke="rgba(255,255,255,0.06)" stroke-dasharray="3 3"/>
                <text :x="chartPaddingLeft - 6" :y="g.y + 3"
                      text-anchor="end" fill="rgba(255,255,255,0.4)" font-size="10">
                  {{ g.label }}分
                </text>
              </g>

              <g v-for="(s, i) in stats" :key="s.hour" class="bar-group">
                <rect :x="barX(i) + barGap / 2"
                      :y="barY(s.workMinutes + s.idleMinutes, s.workMinutes)"
                      :width="barWidth - barGap"
                      :height="barHeight(s.workMinutes)"
                      fill="url(#workGrad)" rx="2" ry="2">
                  <title>{{ s.hour }}:00 - 作业 {{ s.workMinutes }} 分钟</title>
                </rect>

                <rect v-if="s.idleMinutes > 0"
                      :x="barX(i) + barGap / 2"
                      :y="barY(s.workMinutes + s.idleMinutes, s.idleMinutes)"
                      :width="barWidth - barGap"
                      :height="barHeight(s.idleMinutes)"
                      fill="url(#idleGrad)" rx="2" ry="2">
                  <title>{{ s.hour }}:00 - 怠速 {{ s.idleMinutes }} 分钟</title>
                </rect>

                <text :x="barX(i) + barWidth / 2"
                      :y="chartHeight - 10"
                      text-anchor="middle"
                      fill="rgba(255,255,255,0.5)"
                      font-size="10">
                  {{ s.hour }}时
                </text>
              </g>
            </svg>
          </div>

          <div v-if="!loading && stats.length" class="summary">
            <div class="summary-item">
              <span class="s-label">今日累计作业</span>
              <span class="s-value work">{{ totalWork }} 分钟</span>
            </div>
            <div class="summary-item">
              <span class="s-label">今日累计怠速</span>
              <span class="s-value idle">{{ totalIdle }} 分钟</span>
            </div>
            <div class="summary-item">
              <span class="s-label">作业时长</span>
              <span class="s-value">{{ workHours }} 小时</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { fetchWorkStats } from '../api/machine.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  machineId: { type: String, default: '' },
  machineName: { type: String, default: '' }
})

defineEmits(['close'])

const stats = ref([])
const loading = ref(false)

const chartWidth = 600
const chartHeight = 280
const chartPaddingLeft = 40
const chartPaddingRight = 20
const chartPaddingBottom = 28

const barWidth = computed(() => {
  const count = Math.max(stats.value.length, 1)
  return (chartWidth - chartPaddingLeft - chartPaddingRight) / count
})
const barGap = 6

const maxMinutes = computed(() => {
  const max = Math.max(...stats.value.map(s => s.workMinutes + s.idleMinutes), 60)
  return Math.ceil(max / 10) * 10
})

const gridLines = computed(() => {
  const lines = []
  const count = 4
  const innerHeight = chartHeight - chartPaddingBottom - 20
  for (let i = 0; i <= count; i++) {
    const v = Math.round((maxMinutes.value / count) * i)
    lines.push({
      y: chartHeight - chartPaddingBottom - (innerHeight / count) * i,
      label: v
    })
  }
  return lines
})

function barX(i) {
  return chartPaddingLeft + i * barWidth.value
}

function barHeight(minutes) {
  const innerHeight = chartHeight - chartPaddingBottom - 20
  return (minutes / maxMinutes.value) * innerHeight
}

function barY(totalMin, heightMin) {
  const innerHeight = chartHeight - chartPaddingBottom - 20
  const totalY = chartHeight - chartPaddingBottom - (totalMin / maxMinutes.value) * innerHeight
  return totalY
}

const totalWork = computed(() => stats.value.reduce((s, x) => s + x.workMinutes, 0))
const totalIdle = computed(() => stats.value.reduce((s, x) => s + x.idleMinutes, 0))
const idleRate = computed(() => {
  const total = totalWork.value + totalIdle.value
  if (!total) return 0
  return Math.round((totalIdle.value / total) * 100)
})
const workHours = computed(() => {
  const mins = totalWork.value
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h + (m ? '.' + Math.round(m / 60 * 10) : '')
})

watch(() => [props.visible, props.machineId], async ([vis, id]) => {
  if (vis && id) {
    loading.value = true
    try {
      const data = await fetchWorkStats(id)
      stats.value = data || []
    } catch (e) {
      stats.value = []
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-box {
  width: 660px;
  max-width: 90vw;
  background: linear-gradient(135deg, rgba(6, 30, 60, 0.98), rgba(10, 40, 80, 0.95));
  border: 1px solid rgba(0, 230, 118, 0.25);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 230, 118, 0.1);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 230, 118, 0.15);
  background: rgba(0, 230, 118, 0.04);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #00e676;
  letter-spacing: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
  font-size: 20px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 100, 100, 0.2);
  color: #ff5252;
}

.modal-body {
  padding: 20px;
}

.legend {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-item.idle-rate {
  margin-left: auto;
  color: #ff9800;
  font-size: 13px;
}

.legend-item.idle-rate strong {
  color: #ff9800;
  font-weight: 700;
}

.legend-color {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
}

.legend-color.work {
  background: linear-gradient(180deg, #00e676, #00a86b);
}

.legend-color.idle {
  background: linear-gradient(180deg, #ff9800, #e65100);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 2px solid rgba(0, 230, 118, 0.2);
  border-top-color: #00e676;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
}

.chart-wrap {
  display: flex;
  justify-content: center;
  padding: 0 10px;
}

.chart {
  display: block;
}

.summary {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 230, 118, 0.1);
}

.summary-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.s-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.s-value {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.s-value.work {
  color: #00e676;
}

.s-value.idle {
  color: #ff9800;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-box,
.modal-leave-to .modal-box {
  transform: scale(0.9) translateY(-20px);
  opacity: 0;
}
</style>
