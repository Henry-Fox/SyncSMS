import request from '../utils/request'

/**
 * @description 获取登录/注销审计日志（管理员）
 * @param {Object} params - { page, size, username, action }
 * @returns {Promise}
 */
export function getAuthAuditLogs(params) {
  return request.get('/admin/audit/auth-log', { params })
}

