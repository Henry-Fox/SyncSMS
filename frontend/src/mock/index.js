import Mock from 'mockjs'

/**
 * @description 本地开发 mock 开关：VITE_USE_MOCK=true 时启用
 */
export function setupMock() {
  if (import.meta.env.VITE_USE_MOCK !== 'true') return

  const Random = Mock.Random
  const token = Mock.mock('@guid')

  Mock.setup({ timeout: '200-600' })

  // 登录
  Mock.mock('/api/user/login', 'post', () => ({
    code: 200,
    message: 'success',
    data: {
      token,
      username: 'admin',
      nickname: '管理员',
      role: 'admin'
    }
  }))

  // 短信列表
  Mock.mock(/\/api\/sms\/list(\?.*)?$/, 'get', (options) => {
    const url = new URL(options.url, 'http://localhost')
    const page = Number(url.searchParams.get('page') || 1)
    const size = Number(url.searchParams.get('size') || 20)
    const total = 137

    const records = Array.from({ length: size }).map((_, i) => ({
      id: (page - 1) * size + i + 1,
      deviceId: 1,
      sender: Random.pick(['1069xxxx', '10086', '95588', '顺丰快递', '京东']),
      content: Random.pick([
        `验证码：${Random.integer(100000, 999999)}，5分钟内有效。`,
        '您的快递已到达驿站，请及时取件。',
        '尊敬的客户，您的话费余额不足。',
        '银行提醒：您的账户发生一笔交易。'
      ]),
      smsTime: Random.datetime('yyyy-MM-dd HH:mm:ss'),
      syncedAt: Random.datetime('yyyy-MM-dd HH:mm:ss'),
      isRead: Random.integer(0, 1)
    }))

    return {
      code: 200,
      message: 'success',
      data: { total, page, size, records }
    }
  })

  // 短信详情
  Mock.mock(/\/api\/sms\/\d+$/, 'get', () => ({
    code: 200,
    message: 'success',
    data: {
      id: 1,
      deviceId: 1,
      sender: '1069xxxx',
      content: `验证码：${Random.integer(100000, 999999)}，5分钟内有效。`,
      smsTime: Random.datetime('yyyy-MM-dd HH:mm:ss'),
      syncedAt: Random.datetime('yyyy-MM-dd HH:mm:ss'),
      isRead: 0
    }
  }))

  // 标记已读
  Mock.mock(/\/api\/sms\/\d+\/read$/, 'put', () => ({
    code: 200,
    message: 'success',
    data: null
  }))

  // 用户/设备管理
  Mock.mock('/api/admin/users', 'get', () => ({
    code: 200,
    message: 'success',
    data: [
      { id: 1, username: 'admin', nickname: '管理员', role: 'admin', status: 1 },
      { id: 2, username: 'viewer', nickname: '查看者', role: 'viewer', status: 1 }
    ]
  }))

  Mock.mock('/api/admin/devices', 'get', () => ({
    code: 200,
    message: 'success',
    data: [
      {
        id: 1,
        deviceName: '我的手机',
        deviceKey: Mock.mock('@string("hex", 32)'),
        status: 1,
        batteryPercent: Random.integer(15, 99),
        isCharging: Random.integer(0, 1),
        lastSeenAt: Random.datetime('yyyy-MM-dd HH:mm:ss'),
        lastSyncAt: Random.datetime('yyyy-MM-dd HH:mm:ss')
      }
    ]
  }))

  Mock.mock('/api/admin/users', 'post', () => ({ code: 200, message: 'success', data: null }))
  Mock.mock(/\/api\/admin\/users\/\d+$/, 'put', () => ({ code: 200, message: 'success', data: null }))
  Mock.mock(/\/api\/admin\/users\/\d+$/, 'delete', () => ({ code: 200, message: 'success', data: null }))
  Mock.mock('/api/admin/devices', 'post', () => ({
    code: 200,
    message: 'success',
    data: { id: 2, deviceName: '新设备', deviceKey: Mock.mock('@string("hex", 32)'), status: 1 }
  }))
  Mock.mock(/\/api\/admin\/devices\/\d+\/toggle$/, 'put', () => ({ code: 200, message: 'success', data: null }))
  Mock.mock(/\/api\/admin\/devices\/\d+$/, 'delete', () => ({ code: 200, message: 'success', data: null }))
}

