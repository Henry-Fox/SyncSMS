<template>
  <div class="app-shell">
    <div class="app-topbar">
      <div class="app-brand">
        <img class="app-brand-logo" src="/logo.png" width="28" height="28" alt="SyncSMS" />
        <div>SyncSMS</div>
      </div>
      <div class="top-actions">
        <el-tooltip v-if="auth.role" placement="bottom-end" effect="dark">
          <template #content>
            <div class="battery-pop">
              <div class="battery-pop-title">设备电量</div>
              <div v-if="batteryDevices.length === 0" class="battery-pop-empty">暂无设备</div>
              <div v-else class="battery-pop-list">
                <div v-for="d in batteryDevices" :key="d.id" class="battery-pop-item">
                  <div class="battery-pop-name">{{ d.deviceName || `设备#${d.id}` }}</div>
                  <div class="battery-pop-meta">
                    <span v-if="d.batteryPercent === null || d.batteryPercent === undefined">--</span>
                    <span v-else>{{ d.batteryPercent }}%</span>
                    <span v-if="d.isCharging === 1">（充电中）</span>
                    <span v-if="d.lastSeenAt" class="battery-pop-sep">·</span>
                    <span v-if="d.lastSeenAt">
                      {{ (Date.now() - (parseLastSeenMs(d.lastSeenAt) ?? 0) <= onlineWindowMs) ? '在线' : '离线' }}
                      <span class="battery-pop-sep">·</span>
                      {{ d.lastSeenAt }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <div
            class="battery-bar"
            role="button"
            tabindex="0"
            :class="{ clickable: auth.isAdmin }"
            @click="auth.isAdmin ? router.push('/admin') : null"
          >
            <span class="battery-bar-label">最低电量</span>
            <span class="battery-bar-name">
              {{ lowestBatteryDevice?.deviceName || (lowestBatteryDevice ? `设备#${lowestBatteryDevice.id}` : '--') }}
            </span>
            <span class="battery-bar-value" :class="batteryBadgeClass">
              <template v-if="lowestBatteryDevice">
                {{ lowestBatteryDevice.batteryPercent ?? '--' }}%
                <span v-if="lowestBatteryDevice.isCharging === 1"> 充电中</span>
              </template>
              <template v-else>--</template>
            </span>
            <el-tag size="small" :type="lowestDeviceOnlineStatus.type" effect="plain">
              {{ lowestDeviceOnlineStatus.text }}
            </el-tag>
          </div>
        </el-tooltip>
        <el-tag v-if="auth.role" type="info" effect="dark" size="small">{{ auth.role }}</el-tag>
        <el-button v-if="auth.isAdmin" size="default" @click="router.push('/admin')">后台管理</el-button>
        <el-button size="default" @click="onLogout">退出</el-button>
      </div>
    </div>

    <div class="app-content">
      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="header">
            <div class="h-title">
              <div class="h-main">短信列表</div>
              <div class="h-sub">按时间倒序展示，支持筛选与快速标记</div>
            </div>
            <div class="filters">
              <el-input v-model="query.sender" placeholder="发送人" style="width: 180px" clearable />
              <el-input v-model="query.keyword" placeholder="内容关键字" style="width: 220px" clearable />
              <el-select v-model="query.isRead" placeholder="已读状态" style="width: 140px" clearable>
                <el-option :value="0" label="未读" />
                <el-option :value="1" label="已读" />
              </el-select>
              <el-button type="primary" @click="reload">查询</el-button>
            </div>
          </div>
        </template>

        <el-table
          :data="rows"
          v-loading="loading"
          style="width: 100%"
          :header-cell-style="{ background: 'rgba(15, 23, 42, 0.04)' }"
          row-class-name="sms-row"
        >
          <el-table-column prop="smsTime" label="时间" width="170" />
          <el-table-column prop="sender" label="发送人" width="140" />
          <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
          <el-table-column prop="isRead" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.isRead === 1 ? 'success' : 'warning'">
                {{ row.isRead === 1 ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.isRead !== 1" size="small" type="primary" @click="markRead(row)">
                标记已读
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="prev, pager, next, sizes, total"
            :total="total"
            :page-size="query.size"
            :current-page="query.page"
            :page-sizes="[10, 20, 50, 100]"
            @update:page-size="onSize"
            @update:current-page="onPage"
          />
        </div>
      </el-card>
    </div>

    <el-dialog v-model="detailVisible" title="短信详情" width="640px">
      <div v-if="detail">
        <div class="d-row"><span class="k">时间</span><span class="v">{{ detail.smsTime }}</span></div>
        <div class="d-row"><span class="k">发送人</span><span class="v">{{ detail.sender }}</span></div>
        <div class="d-row"><span class="k">状态</span>
          <span class="v">
            <el-tag :type="detail.isRead === 1 ? 'success' : 'warning'" size="small">
              {{ detail.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
            <span v-if="detail.readBy" style="margin-left: 8px; color: #6b7280; font-size: 13px;">
              {{ detail.readBy }} · {{ detail.readAt }}
            </span>
          </span>
        </div>
        <div class="d-row"><span class="k">内容</span></div>
        <pre class="pre">{{ detail.content }}</pre>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDeviceStatus } from '../api/devices'
import { getSmsDetail, getSmsList, markSmsRead } from '../api/sms'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 20,
  sender: '',
  keyword: '',
  isRead: undefined
})

const detailVisible = ref(false)
const detail = ref(null)

const devices = ref([])
const loadingDevices = ref(false)

async function loadDevices() {
  // 电量/在线状态对所有能看短信列表的人可见；接口由后端保证权限
  loadingDevices.value = true
  try {
    const res = await getDeviceStatus()
    devices.value = res.data || []
  } finally {
    loadingDevices.value = false
  }
}

const onlineWindowMs = 2 * 60 * 1000

/**
 * @description 将后端常见的 "YYYY-MM-DD HH:mm:ss" 转为可解析时间戳（兼容 Safari）
 * @param {string|number|null|undefined} lastSeenAt
 * @returns {number|null}
 */
function parseLastSeenMs(lastSeenAt) {
  if (!lastSeenAt) return null
  if (typeof lastSeenAt === 'string') {
    const iso = lastSeenAt.includes('T') ? lastSeenAt : lastSeenAt.replace(' ', 'T')
    const t = Date.parse(iso)
    return Number.isFinite(t) ? t : null
  }
  if (typeof lastSeenAt === 'number') return lastSeenAt
  return null
}

const batteryDevices = computed(() => {
  const list = Array.isArray(devices.value) ? devices.value : []
  return [...list].sort((a, b) => {
    const ap = a?.batteryPercent
    const bp = b?.batteryPercent
    const aKnown = ap !== null && ap !== undefined
    const bKnown = bp !== null && bp !== undefined
    if (aKnown && bKnown) return ap - bp
    if (aKnown) return -1
    if (bKnown) return 1
    return (String(a?.deviceName || '')).localeCompare(String(b?.deviceName || ''))
  })
})

const lowestBatteryDevice = computed(() => {
  const known = batteryDevices.value.find(d => d?.batteryPercent !== null && d?.batteryPercent !== undefined)
  return known || batteryDevices.value[0] || null
})

const batteryBadgeClass = computed(() => {
  const d = lowestBatteryDevice.value
  const p = d?.batteryPercent
  if (p === null || p === undefined) return 'is-unknown'
  if (p <= 20 && d?.isCharging !== 1) return 'is-danger'
  if (p <= 50 && d?.isCharging !== 1) return 'is-warn'
  return 'is-ok'
})

const lowestDeviceOnlineStatus = computed(() => {
  const d = lowestBatteryDevice.value
  if (!d) return { text: '未知', type: 'info', isOnline: null }
  const ms = parseLastSeenMs(d.lastSeenAt)
  if (ms == null) return { text: '未知', type: 'info', isOnline: null }
  const delta = Date.now() - ms
  if (delta <= onlineWindowMs) return { text: '在线', type: 'success', isOnline: true }
  return { text: '离线', type: 'danger', isOnline: false }
})

async function reload() {
  loading.value = true
  try {
    const res = await getSmsList({ ...query })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  const res = await getSmsDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function markRead(row) {
  await markSmsRead(row.id)
  ElMessage.success('已标记')
  await reload()
}

function onPage(p) {
  query.page = p
  reload()
}

function onSize(s) {
  query.size = s
  query.page = 1
  reload()
}

async function onLogout() {
  await auth.logout()
  router.replace('/login')
}

let deviceTimer = null
let smsTimer = null

onMounted(() => {
  reload()
  loadDevices()
  deviceTimer = setInterval(() => loadDevices(), 30_000)
  smsTimer = setInterval(() => reload(), 15_000)
})

onBeforeUnmount(() => {
  if (deviceTimer) clearInterval(deviceTimer)
  deviceTimer = null
  if (smsTimer) clearInterval(smsTimer)
  smsTimer = null
})
</script>

<style scoped>
.top-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.top-actions :deep(.el-button) {
  height: 32px;
  padding: 0 12px;
  border-radius: 10px;
}
.top-actions :deep(.el-tag) {
  height: 32px;
  line-height: 32px;
  padding: 0 10px;
  border-radius: 10px;
}

.battery-bar {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  user-select: none;
  max-width: 380px;
}
.battery-bar.clickable {
  cursor: pointer;
}
.battery-bar.clickable:hover {
  border-color: rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.1);
}
.battery-bar-label {
  font-size: 12px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.76);
}
.battery-bar-name {
  font-size: 12px;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.92);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.battery-bar-value {
  font-size: 12px;
  font-weight: 900;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(0, 0, 0, 0.12);
  color: rgba(255, 255, 255, 0.92);
  white-space: nowrap;
}
.battery-bar-value.is-danger {
  background: rgba(127, 29, 29, 0.28);
  border-color: rgba(239, 68, 68, 0.32);
}
.battery-bar-value.is-warn {
  background: rgba(146, 64, 14, 0.26);
  border-color: rgba(245, 158, 11, 0.32);
}
.battery-bar-value.is-ok {
  background: rgba(20, 83, 45, 0.24);
  border-color: rgba(34, 197, 94, 0.3);
}
.battery-bar-value.is-unknown {
  background: rgba(15, 23, 42, 0.22);
  border-color: rgba(148, 163, 184, 0.22);
}

@media (max-width: 980px) {
  .battery-bar-label {
    display: none;
  }
  .battery-bar {
    max-width: 300px;
  }
}

.battery-pop {
  min-width: 320px;
  max-width: 420px;
}
.battery-pop-title {
  font-weight: 900;
  margin-bottom: 8px;
}
.battery-pop-empty {
  color: rgba(226, 232, 240, 0.95);
}
.battery-pop-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.battery-pop-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.battery-pop-name {
  font-weight: 800;
}
.battery-pop-meta {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.95);
}
.battery-pop-sep {
  margin: 0 6px;
  opacity: 0.9;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.h-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.h-main {
  font-weight: 900;
  color: rgba(15, 23, 42, 0.95);
  letter-spacing: 0.2px;
}
.h-sub {
  font-size: 12px;
  color: rgba(100, 116, 139, 0.95);
}
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.d-row {
  display: flex;
  gap: 10px;
  margin: 6px 0;
}
.k {
  width: 64px;
  color: #6b7280;
}
.v {
  color: #111827;
}
.pre {
  background: #0b1020;
  color: #e5e7eb;
  padding: 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>

