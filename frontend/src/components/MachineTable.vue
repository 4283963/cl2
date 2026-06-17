<template>
  <div class="table-wrapper">
    <div class="table-header">
      <span class="table-title">农机作业统计</span>
      <span class="table-count">共 {{ machines.length }} 台</span>
    </div>
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>排名</th>
            <th>农机名称</th>
            <th>农机ID</th>
            <th>作业类型</th>
            <th>今日面积(亩)</th>
            <th>状态</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(m, index) in machines" :key="m.machineId"
              :class="{ 'row-highlight': index < 3 }">
            <td>
              <span :class="['rank-badge', `rank-${index + 1}`]">{{ index + 1 }}</span>
            </td>
            <td class="name-cell">{{ m.machineName }}</td>
            <td class="id-cell">{{ m.machineId }}</td>
            <td>
              <span :class="['work-type-tag', `type-${(m.currentWorkType || '').toLowerCase()}`]">
                {{ workTypeLabels[m.currentWorkType] || m.currentWorkType || '--' }}
              </span>
            </td>
            <td class="area-cell">{{ m.todayArea }}</td>
            <td>
              <span :class="['status-dot', m.status === 'ONLINE' ? 'online' : 'offline']"></span>
              {{ m.status === 'ONLINE' ? '在线' : '离线' }}
            </td>
            <td class="time-cell">{{ m.updatedAt || '--' }}</td>
          </tr>
          <tr v-if="machines.length === 0">
            <td colspan="7" class="empty-row">暂无农机数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
defineProps({
  machines: { type: Array, default: () => [] }
})

const workTypeLabels = {
  SOWING: '播种',
  HARVESTING: '收割',
  PLOWING: '犁地',
  SPRAYING: '喷洒',
  TRANSPORTING: '转运'
}
</script>

<style scoped>
.table-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(6, 30, 60, 0.85);
  border-radius: 8px;
  border: 1px solid rgba(0, 230, 118, 0.15);
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(0, 230, 118, 0.15);
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #00e676;
  letter-spacing: 2px;
}

.table-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.table-scroll {
  flex: 1;
  overflow-y: auto;
}

.table-scroll::-webkit-scrollbar {
  width: 4px;
}

.table-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 230, 118, 0.3);
  border-radius: 2px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead tr {
  background: rgba(0, 230, 118, 0.08);
}

th {
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  text-align: left;
  white-space: nowrap;
  letter-spacing: 1px;
}

td {
  padding: 10px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

tbody tr:hover {
  background: rgba(0, 230, 118, 0.06);
}

.row-highlight {
  background: rgba(0, 230, 118, 0.03);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.1);
}

.rank-1 { background: linear-gradient(135deg, #ffd700, #ffb300); color: #1a1a2e; }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #9e9e9e); color: #1a1a2e; }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #a0522d); color: #fff; }

.name-cell {
  font-weight: 600;
  color: #fff;
}

.id-cell {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.area-cell {
  font-weight: 700;
  color: #00e676;
  font-size: 14px;
}

.work-type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.type-sowing { background: rgba(76, 175, 80, 0.2); color: #66bb6a; }
.type-harvesting { background: rgba(255, 152, 0, 0.2); color: #ffa726; }
.type-plowing { background: rgba(121, 85, 72, 0.2); color: #a1887f; }
.type-spraying { background: rgba(33, 150, 243, 0.2); color: #42a5f5; }
.type-transporting { background: rgba(156, 39, 176, 0.2); color: #ab47bc; }

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.status-dot.online {
  background: #00e676;
  box-shadow: 0 0 6px rgba(0, 230, 118, 0.6);
}

.status-dot.offline {
  background: #666;
}

.time-cell {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  white-space: nowrap;
}

.empty-row {
  text-align: center;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.3);
  font-size: 14px;
}
</style>
