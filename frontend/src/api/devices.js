import request from '../utils/request'

/**
 * @description 获取设备状态列表（不包含密钥，给所有 Web 用户展示用）
 * @returns {Promise}
 */
export function getDeviceStatus() {
  return request.get('/devices/status')
}

