import request from '../utils/request'

/**
 * @description 用户登录
 * @param {Object} data - { username, password }
 * @returns {Promise}
 */
export function login(data) {
  return request.post('/user/login', data)
}

/**
 * @description 用户注销（用于审计记录）
 * @returns {Promise}
 */
export function logout() {
  return request.post('/user/logout')
}

/**
 * @description 获取验证码（base64 png）
 * @returns {Promise}
 */
export function getCaptcha() {
  return request.get('/user/captcha')
}
