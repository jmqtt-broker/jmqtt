import request from '@/utils/request'

// 客户端会话信息
export function brokerInfo(brokerId) {
  return request({
    url: '/broker/' + brokerId,
    method: 'get'
  })
}

// 在线broker列表
export function brokerList(query) {
  return request({
    url: '/broker/list',
    method: 'get',
    params: query
  })
}

export function page(query) {
  return request({
    url: '/broker',
    method: 'get',
    params: query
  })
}

