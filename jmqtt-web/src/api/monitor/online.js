import request from '@/utils/request'

// 在线broker列表
export function brokerList(query) {
  return request({
    url: '/broker/list',
    method: 'get',
    params: query
  })
}

// 查询在线用户列表
export function list(query) {
  return request({
    url: '/session',
    method: 'get',
    params: query
  })
}

// 强退用户
export function forceLogout(clientId) {
  return request({
    url: '/session/kick/' + clientId,
    method: 'delete'
  })
}
