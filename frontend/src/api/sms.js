import request from '../utils/request'

/**
 * @description 获取短信列表（分页）
 * @param {Object} params - { page, size, sender, keyword, deviceId, isRead }
 * @returns {Promise}
 */
export function getSmsList(params) {
  return request.get('/sms/list', { params })
}

/**
 * @description 获取短信详情
 * @param {number} id - 短信ID
 * @returns {Promise}
 */
export function getSmsDetail(id) {
  return request.get(`/sms/${id}`)
}

/**
 * @description 标记短信为已读
 * @param {number} id - 短信ID
 * @returns {Promise}
 */
export function markSmsRead(id) {
  return request.put(`/sms/${id}/read`)
}
