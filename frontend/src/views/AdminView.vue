<template>
  <div class="app-shell">
    <div class="app-topbar">
      <div class="app-brand" @click="router.push('/sms')" style="cursor: pointer">
        <img class="app-brand-logo" src="/logo.png" width="28" height="28" alt="SyncSMS" />
        <div>SyncSMS 管理</div>
      </div>
      <div class="top-actions">
        <el-button size="default" @click="router.push('/sms')">返回短信</el-button>
      </div>
    </div>

    <div class="app-content">
      <el-card class="glass-card" shadow="never">
        <template #header>
          <div class="header">
            <div class="h-title">
              <div class="h-main">后台管理</div>
              <div class="h-sub">管理用户账号与设备密钥（给 APP 使用）</div>
            </div>
            <div class="h-right">
              <el-tooltip placement="bottom-end" effect="dark">
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

                <div class="battery-pill" role="button" tabindex="0" @click="tab = 'devices'">
                  <div class="battery-pill-main">
                    <div class="battery-pill-top">
                      <span class="battery-pill-label">最低电量</span>
                      <el-tag size="small" :type="lowestDeviceOnlineStatus.type" effect="plain">
                        {{ lowestDeviceOnlineStatus.text }}
                      </el-tag>
                    </div>
                    <div class="battery-pill-sub">
                      <span class="battery-pill-name">
                        {{ lowestBatteryDevice?.deviceName || (lowestBatteryDevice ? `设备#${lowestBatteryDevice.id}` : '--') }}
                      </span>
                      <span class="battery-pill-value" :class="batteryBadgeClass">
                        <template v-if="lowestBatteryDevice">
                          {{ lowestBatteryDevice.batteryPercent ?? '--' }}%
                          <span v-if="lowestBatteryDevice.isCharging === 1"> 充电中</span>
                        </template>
                        <template v-else>--</template>
                      </span>
                    </div>
                  </div>
                </div>
              </el-tooltip>
            </div>
          </div>
        </template>

        <el-tabs v-model="tab" type="card" class="tabs">
        <el-tab-pane label="用户管理" name="users">
          <div class="panel-head">
            <div class="panel-title">用户管理</div>
            <el-button type="primary" @click="openUserCreate">新增用户</el-button>
          </div>

            <el-table :data="users" v-loading="loadingUsers">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="160" />
              <el-table-column prop="nickname" label="昵称" />
              <el-table-column prop="role" label="角色" width="120" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template #default="{ row }">
                  <el-button size="small" @click="openUserEdit(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="removeUser(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
        </el-tab-pane>

        <el-tab-pane label="设备管理" name="devices">
          <div class="panel-head">
            <div class="panel-title">设备管理</div>
            <div class="panel-actions">
              <el-button @click="loadDevices">刷新</el-button>
              <el-button type="primary" @click="openDeviceCreate">新增设备</el-button>
            </div>
          </div>

            <el-table :data="devices" v-loading="loadingDevices">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="deviceName" label="设备名称" min-width="180" />
              <el-table-column prop="deviceKey" label="设备密钥" min-width="260" show-overflow-tooltip />
              <el-table-column label="电量" width="140">
                <template #default="{ row }">
                  <span v-if="row.batteryPercent === null || row.batteryPercent === undefined">--</span>
                  <el-tag v-else :type="row.batteryPercent <= 20 && row.isCharging !== 1 ? 'danger' : 'info'">
                    {{ row.batteryPercent }}%
                    <span v-if="row.isCharging === 1"> 充电中</span>
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lastSeenAt" label="最后在线" width="180" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="260">
                <template #default="{ row }">
                  <el-button size="small" @click="toggle(row)">切换状态</el-button>
                  <el-button size="small" type="danger" @click="removeDevice(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="audit">
          <div class="panel-head">
            <div class="panel-title">登录/注销日志</div>
            <div class="audit-filters">
              <el-input v-model="auditQuery.username" placeholder="用户名" style="width: 160px" clearable />
              <el-select v-model="auditQuery.action" placeholder="动作" style="width: 180px" clearable>
                <el-option label="登录成功" value="login_success" />
                <el-option label="登录失败" value="login_fail" />
                <el-option label="注销" value="logout" />
              </el-select>
              <el-button type="primary" @click="loadAudit">查询</el-button>
            </div>
          </div>

          <el-table :data="auditRows" v-loading="auditLoading">
            <el-table-column prop="createdAt" label="时间" width="180" />
            <el-table-column prop="username" label="用户名" width="160" />
            <el-table-column prop="action" label="动作" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.action === 'login_success'" type="success">登录成功</el-tag>
                <el-tag v-else-if="row.action === 'login_fail'" type="danger">登录失败</el-tag>
                <el-tag v-else type="info">注销</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="160" />
            <el-table-column prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
          </el-table>

          <div class="pager">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :total="auditTotal"
              :page-size="auditQuery.size"
              :current-page="auditQuery.page"
              :page-sizes="[10, 20, 50, 100]"
              @update:page-size="(s) => { auditQuery.size = s; auditQuery.page = 1; loadAudit() }"
              @update:current-page="(p) => { auditQuery.page = p; loadAudit() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
      </el-card>
    </div>

    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="520px">
      <el-form :model="userForm" label-width="90px">
        <el-form-item label="用户名" v-if="userDialogMode === 'create'">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.password" show-password placeholder="不修改可留空" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="userDialogMode === 'edit'">
          <el-select v-model="userForm.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deviceDialogVisible" title="新增设备" width="520px">
      <el-form :model="deviceForm" label-width="90px">
        <el-form-item label="设备名称">
          <el-input v-model="deviceForm.deviceName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDevice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createDevice,
  createUser,
  deleteDevice,
  deleteUser,
  getDevices,
  getUsers,
  toggleDevice,
  updateUser
} from '../api/admin'
import { getAuthAuditLogs } from '../api/audit'

const router = useRouter()
const tab = ref('users')

const users = ref([])
const devices = ref([])
const auditRows = ref([])
const auditTotal = ref(0)
const auditLoading = ref(false)
const auditQuery = reactive({ page: 1, size: 20, username: '', action: '' })
const loadingUsers = ref(false)
const loadingDevices = ref(false)

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

const onlineWindowMs = 2 * 60 * 1000

function parseLastSeenMs(lastSeenAt) {
  if (!lastSeenAt) return null
  // 后端通常返回 "YYYY-MM-DD HH:mm:ss"；Safari 对该格式兼容性差，所以手动转 ISO
  if (typeof lastSeenAt === 'string') {
    const iso = lastSeenAt.includes('T') ? lastSeenAt : lastSeenAt.replace(' ', 'T')
    const t = Date.parse(iso)
    return Number.isFinite(t) ? t : null
  }
  // 兜底：若后端返回时间戳
  if (typeof lastSeenAt === 'number') return lastSeenAt
  return null
}

const lowestDeviceOnlineStatus = computed(() => {
  const d = lowestBatteryDevice.value
  if (!d) return { text: '未知', type: 'info', isOnline: null }
  const ms = parseLastSeenMs(d.lastSeenAt)
  if (ms == null) return { text: '未知', type: 'info', isOnline: null }
  const delta = Date.now() - ms
  if (delta <= onlineWindowMs) return { text: '在线', type: 'success', isOnline: true }
  return { text: '离线', type: 'danger', isOnline: false }
})

async function loadUsers() {
  loadingUsers.value = true
  try {
    const res = await getUsers()
    users.value = res.data || []
  } finally {
    loadingUsers.value = false
  }
}

async function loadDevices() {
  loadingDevices.value = true
  try {
    const res = await getDevices()
    devices.value = res.data || []
  } finally {
    loadingDevices.value = false
  }
}

async function loadAudit() {
  auditLoading.value = true
  try {
    const res = await getAuthAuditLogs({ ...auditQuery })
    const page = res.data || {}
    auditRows.value = page.records || []
    auditTotal.value = page.total || 0
  } finally {
    auditLoading.value = false
  }
}

// 用户弹窗
const userDialogVisible = ref(false)
const userDialogMode = ref('create') // create | edit
const editingUserId = ref(null)
const userForm = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'viewer',
  status: 1
})

const userDialogTitle = computed(() => (userDialogMode.value === 'create' ? '新增用户' : '编辑用户'))

function openUserCreate() {
  userDialogMode.value = 'create'
  editingUserId.value = null
  userForm.username = ''
  userForm.password = ''
  userForm.nickname = ''
  userForm.role = 'viewer'
  userForm.status = 1
  userDialogVisible.value = true
}

function openUserEdit(row) {
  userDialogMode.value = 'edit'
  editingUserId.value = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.nickname = row.nickname || ''
  userForm.role = row.role || 'viewer'
  userForm.status = row.status ?? 1
  userDialogVisible.value = true
}

async function submitUser() {
  if (userDialogMode.value === 'create') {
    if (!userForm.username || !userForm.password) {
      ElMessage.warning('用户名和密码不能为空')
      return
    }
    await createUser({
      username: userForm.username,
      password: userForm.password,
      nickname: userForm.nickname,
      role: userForm.role
    })
    ElMessage.success('创建成功')
  } else {
    await updateUser(editingUserId.value, {
      password: userForm.password || undefined,
      nickname: userForm.nickname,
      role: userForm.role,
      status: userForm.status
    })
    ElMessage.success('更新成功')
  }
  userDialogVisible.value = false
  await loadUsers()
}

async function removeUser(row) {
  await ElMessageBox.confirm(`确认删除用户：${row.username}？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  await loadUsers()
}

// 设备弹窗
const deviceDialogVisible = ref(false)
const deviceForm = reactive({ deviceName: '' })

function openDeviceCreate() {
  deviceForm.deviceName = ''
  deviceDialogVisible.value = true
}

async function submitDevice() {
  if (!deviceForm.deviceName) {
    ElMessage.warning('设备名称不能为空')
    return
  }
  const res = await createDevice({ deviceName: deviceForm.deviceName })
  deviceDialogVisible.value = false
  ElMessage.success(`创建成功，密钥：${res.data.deviceKey}`)
  await loadDevices()
}

async function toggle(row) {
  await toggleDevice(row.id)
  ElMessage.success('已切换')
  await loadDevices()
}

async function removeDevice(row) {
  await ElMessageBox.confirm(`确认删除设备：${row.deviceName}？`, '提示', { type: 'warning' })
  await deleteDevice(row.id)
  ElMessage.success('已删除')
  await loadDevices()
}

onMounted(() => {
  loadUsers()
  loadDevices()
})

/**
 * @description 仅在切换到「登录日志」时再请求审计接口，避免进入后台即因该接口失败而弹出全局错误条（例如未执行 Flyway V3、auth_audit_log 表不存在）
 */
watch(tab, (name) => {
  if (name === 'audit') {
    loadAudit()
  }
  if (name === 'devices') {
    loadDevices()
  }
})
</script>

<style scoped>
.top-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.top-actions :deep(.el-button) {
  height: 32px;
  padding: 0 12px;
  border-radius: 10px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.h-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.h-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.h-main {
  font-weight: 900;
  color: rgba(15, 23, 42, 0.95);
}
.h-sub {
  font-size: 12px;
  color: rgba(100, 116, 139, 0.95);
}
.tabs {
  margin-top: 8px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 12px;
}
.panel-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.battery-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.65);
  cursor: pointer;
  user-select: none;
}
.battery-pill:hover {
  border-color: rgba(148, 163, 184, 0.6);
}
.battery-pill-label {
  font-size: 12px;
  font-weight: 700;
  color: rgba(71, 85, 105, 0.95);
}
.battery-pill-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
}
.battery-pill-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.battery-pill-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.battery-pill-name {
  font-size: 12px;
  font-weight: 900;
  color: rgba(15, 23, 42, 0.95);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.battery-pill-value {
  font-size: 12px;
  font-weight: 900;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(241, 245, 249, 0.85);
  color: rgba(15, 23, 42, 0.95);
}
.battery-pill-value.is-danger {
  background: rgba(254, 226, 226, 0.85);
  border-color: rgba(239, 68, 68, 0.35);
  color: rgba(153, 27, 27, 0.95);
}
.battery-pill-value.is-warn {
  background: rgba(254, 243, 199, 0.85);
  border-color: rgba(245, 158, 11, 0.35);
  color: rgba(146, 64, 14, 0.95);
}
.battery-pill-value.is-ok {
  background: rgba(220, 252, 231, 0.85);
  border-color: rgba(34, 197, 94, 0.35);
  color: rgba(20, 83, 45, 0.95);
}
.battery-pill-value.is-unknown {
  background: rgba(226, 232, 240, 0.75);
  border-color: rgba(148, 163, 184, 0.35);
  color: rgba(71, 85, 105, 0.95);
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
.panel-title {
  font-weight: 800;
  color: rgba(15, 23, 42, 0.95);
}
.audit-filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

