import request from '../utils/request'

// ==================== 用户管理 ====================

/**
 * @description 获取所有用户列表
 * @returns {Promise}
 */
export function getUsers() {
  return request.get('/admin/users')
}

/**
 * @description 创建用户
 * @param {Object} data - { username, password, nickname, role }
 * @returns {Promise}
 */
export function createUser(data) {
  return request.post('/admin/users', data)
}

/**
 * @description 更新用户
 * @param {number} id - 用户ID
 * @param {Object} data - { password, nickname, role, status }
 * @returns {Promise}
 */
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

/**
 * @description 删除用户
 * @param {number} id - 用户ID
 * @returns {Promise}
 */
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

// ==================== 设备管理 ====================

/**
 * @description 获取所有设备列表
 * @returns {Promise}
 */
export function getDevices() {
  return request.get('/admin/devices')
}

/**
 * @description 创建设备
 * @param {Object} data - { deviceName }
 * @returns {Promise}
 */
export function createDevice(data) {
  return request.post('/admin/devices', data)
}

/**
 * @description 切换设备状态
 * @param {number} id - 设备ID
 * @returns {Promise}
 */
export function toggleDevice(id) {
  return request.put(`/admin/devices/${id}/toggle`)
}

/**
 * @description 删除设备
 * @param {number} id - 设备ID
 * @returns {Promise}
 */
export function deleteDevice(id) {
  return request.delete(`/admin/devices/${id}`)
}
