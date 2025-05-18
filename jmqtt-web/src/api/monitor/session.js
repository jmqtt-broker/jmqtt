import request from '@/utils/request'

// 客户端会话信息
export function sessionInfo(clientId) {
  return request({
    url: '/session/' + clientId,
    method: 'get'
  })
}

// 查询订阅信息
export function getSubscriptions(clientId) {
  return request({
    url: '/session/subscriptions/' + clientId,
    method: 'get'
  })
}

// 查询遗嘱消息
export function getWill(clientId) {
  return request({
    url: '/session/will/' + clientId,
    method: 'get'
  })
}
