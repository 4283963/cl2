<template>
  <div ref="mapContainer" class="map-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'

const props = defineProps({
  machines: { type: Array, default: () => [] }
})

const mapContainer = ref(null)
let map = null
let markersLayer = null

const workTypeLabels = {
  SOWING: '播种',
  HARVESTING: '收割',
  PLOWING: '犁地',
  SPRAYING: '喷洒',
  TRANSPORTING: '转运'
}

onMounted(() => {
  map = L.map(mapContainer.value, {
    center: [34.2658, 108.9541],
    zoom: 14,
    zoomControl: false
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 18
  }).addTo(map)

  L.control.zoom({ position: 'topright' }).addTo(map)

  markersLayer = L.layerGroup().addTo(map)
  updateMarkers(props.machines)
})

watch(() => props.machines, (newMachines) => {
  updateMarkers(newMachines)
}, { deep: true })

function updateMarkers(machines) {
  if (!markersLayer) return
  markersLayer.clearLayers()

  machines.forEach(m => {
    if (m.currentLat && m.currentLng && m.status === 'ONLINE') {
      const pulseIcon = L.divIcon({
        className: 'machine-marker',
        html: `<div class="marker-pulse"></div><div class="marker-dot"></div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      })

      const marker = L.marker([m.currentLat, m.currentLng], { icon: pulseIcon })
      const workType = workTypeLabels[m.currentWorkType] || m.currentWorkType || '未知'
      marker.bindPopup(`
        <div style="font-size:13px;line-height:1.6;">
          <strong>${m.machineName}</strong><br/>
          ID: ${m.machineId}<br/>
          作业类型: ${workType}<br/>
          今日面积: ${m.todayArea} 亩<br/>
          状态: ${m.status === 'ONLINE' ? '🟢 在线' : '🔴 离线'}
        </div>
      `)
      markersLayer.addLayer(marker)
    }
  })
}

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}
</style>

<style>
.machine-marker {
  background: none !important;
  border: none !important;
}

.marker-dot {
  width: 14px;
  height: 14px;
  background: #00e676;
  border: 2px solid #fff;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 8px rgba(0, 230, 118, 0.8);
  z-index: 2;
}

.marker-pulse {
  width: 24px;
  height: 24px;
  background: rgba(0, 230, 118, 0.3);
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: pulse 2s ease-out infinite;
  z-index: 1;
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(2.5);
    opacity: 0;
  }
}
</style>
