import type {
  MQTTUser,
  MQTTClient,
  TopicNode,
  RetainedMessage,
  OfflineMessage,
  BrokerStats,
  DashboardStats,
  Subscription,
  ClientDetail,
  ClusterNode,
  SubscriptionDetail,
  DelayedMessage,
  BlacklistItem
} from '../types/mqtt';

export const mockUsers: MQTTUser[] = [
  {
    id: '1',
    username: 'admin',
    isSuperuser: true,
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-01-15T08:00:00Z',
    aclRules: [
      { id: '1', userId: '1', topic: '#', permission: 'allow', action: 'pubsub', qos: [0, 1, 2] }
    ]
  },
  {
    id: '2',
    username: 'sensor_device',
    isSuperuser: false,
    createdAt: '2024-01-16T10:30:00Z',
    updatedAt: '2024-01-16T10:30:00Z',
    aclRules: [
      { id: '2', userId: '2', topic: 'sensors/+/data', permission: 'allow', action: 'publish', qos: [0, 1] },
      { id: '3', userId: '2', topic: 'sensors/+/config', permission: 'allow', action: 'subscribe', qos: [0, 1] }
    ]
  },
  {
    id: '3',
    username: 'mobile_app',
    isSuperuser: false,
    createdAt: '2024-01-17T14:20:00Z',
    updatedAt: '2024-01-17T14:20:00Z',
    aclRules: [
      { id: '4', userId: '3', topic: 'app/+/notifications', permission: 'allow', action: 'subscribe', qos: [0, 1, 2] },
      { id: '5', userId: '3', topic: 'app/+/commands', permission: 'allow', action: 'publish', qos: [0, 1] }
    ]
  },
  {
    id: '4',
    username: 'gateway_001',
    isSuperuser: false,
    createdAt: '2024-01-18T09:15:00Z',
    updatedAt: '2024-01-18T09:15:00Z',
    aclRules: [
      { id: '6', userId: '4', topic: 'gateway/001/+', permission: 'allow', action: 'pubsub', qos: [0, 1, 2] }
    ]
  },
  {
    id: '5',
    username: 'analytics_service',
    isSuperuser: false,
    createdAt: '2024-01-19T16:45:00Z',
    updatedAt: '2024-01-19T16:45:00Z',
    aclRules: [
      { id: '7', userId: '5', topic: 'analytics/+', permission: 'allow', action: 'subscribe', qos: [0, 1] }
    ]
  }
];

export const mockSubscriptions: Subscription[] = [
  { id: '1', topic: 'sensors/temperature/data', qos: 1, clientId: 'sensor_001' },
  { id: '2', topic: 'sensors/humidity/data', qos: 1, clientId: 'sensor_002' },
  { id: '3', topic: 'app/user123/notifications', qos: 2, clientId: 'mobile_client_001' },
  { id: '4', topic: 'gateway/001/status', qos: 0, clientId: 'gateway_001' },
  { id: '5', topic: 'analytics/events', qos: 1, clientId: 'analytics_001' },
  { id: '6', topic: 'system/heartbeat', qos: 0, clientId: 'monitor_001' },
  { id: '7', topic: 'devices/+/status', qos: 1, clientId: 'device_manager' },
  { id: '8', topic: 'config/updates', qos: 2, clientId: 'config_client' }
];

export const mockClients: MQTTClient[] = [
  {
    id: '1',
    clientId: 'sensor_001',
    username: 'sensor_device',
    peername: '192.168.1.101:45678',
    connectedAt: '2024-01-20T08:30:00Z',
    keepalive: 60,
    protocolVersion: '3.1.1',
    cleanSession: true,
    isBridge: false,
    subscriptions: [mockSubscriptions[0]],
    messagesReceived: 15420,
    messagesSent: 120,
    bytesReceived: 1024000,
    bytesSent: 8500
  },
  {
    id: '2',
    clientId: 'sensor_002',
    username: 'sensor_device',
    peername: '192.168.1.102:45679',
    connectedAt: '2024-01-20T08:35:00Z',
    keepalive: 60,
    protocolVersion: '3.1.1',
    cleanSession: true,
    isBridge: false,
    subscriptions: [mockSubscriptions[1]],
    messagesReceived: 12350,
    messagesSent: 80,
    bytesReceived: 850000,
    bytesSent: 5200
  },
  {
    id: '3',
    clientId: 'mobile_client_001',
    username: 'mobile_app',
    peername: '192.168.1.50:51234',
    connectedAt: '2024-01-20T09:00:00Z',
    keepalive: 300,
    protocolVersion: '5.0',
    cleanSession: false,
    isBridge: false,
    subscriptions: [mockSubscriptions[2]],
    messagesReceived: 450,
    messagesSent: 890,
    bytesReceived: 45000,
    bytesSent: 120000
  },
  {
    id: '4',
    clientId: 'gateway_001',
    username: 'gateway_001',
    peername: '192.168.1.200:1883',
    connectedAt: '2024-01-20T07:00:00Z',
    keepalive: 120,
    protocolVersion: '3.1.1',
    cleanSession: false,
    isBridge: true,
    subscriptions: [mockSubscriptions[3]],
    messagesReceived: 50000,
    messagesSent: 45000,
    bytesReceived: 5000000,
    bytesSent: 4200000
  },
  {
    id: '5',
    clientId: 'analytics_001',
    username: 'analytics_service',
    peername: '192.168.1.30:42345',
    connectedAt: '2024-01-20T06:00:00Z',
    keepalive: 60,
    protocolVersion: '3.0',
    cleanSession: true,
    isBridge: false,
    subscriptions: [mockSubscriptions[4]],
    messagesReceived: 100000,
    messagesSent: 50,
    bytesReceived: 10000000,
    bytesSent: 3000
  },
  {
    id: '6',
    clientId: 'device_manager',
    username: 'admin',
    peername: '192.168.1.10:45680',
    connectedAt: '2024-01-20T10:00:00Z',
    keepalive: 60,
    protocolVersion: '5.0',
    cleanSession: false,
    isBridge: false,
    subscriptions: [mockSubscriptions[6]],
    messagesReceived: 5000,
    messagesSent: 2000,
    bytesReceived: 500000,
    bytesSent: 200000
  }
];

export const mockTopicTree: TopicNode = {
  name: '',
  path: '',
  children: [
    {
      name: 'sensors',
      path: 'sensors',
      children: [
        {
          name: 'temperature',
          path: 'sensors/temperature',
          children: [
            { name: 'data', path: 'sensors/temperature/data', children: [], subscriberCount: 3, messageCount: 15420 },
            { name: 'config', path: 'sensors/temperature/config', children: [], subscriberCount: 1, messageCount: 50 }
          ],
          subscriberCount: 4,
          messageCount: 15470
        },
        {
          name: 'humidity',
          path: 'sensors/humidity',
          children: [
            { name: 'data', path: 'sensors/humidity/data', children: [], subscriberCount: 2, messageCount: 12350 },
            { name: 'config', path: 'sensors/humidity/config', children: [], subscriberCount: 1, messageCount: 30 }
          ],
          subscriberCount: 3,
          messageCount: 12380
        }
      ],
      subscriberCount: 7,
      messageCount: 27850
    },
    {
      name: 'app',
      path: 'app',
      children: [
        {
          name: 'user123',
          path: 'app/user123',
          children: [
            { name: 'notifications', path: 'app/user123/notifications', children: [], subscriberCount: 1, messageCount: 450 },
            { name: 'commands', path: 'app/user123/commands', children: [], subscriberCount: 0, messageCount: 890 }
          ],
          subscriberCount: 1,
          messageCount: 1340
        }
      ],
      subscriberCount: 1,
      messageCount: 1340
    },
    {
      name: 'gateway',
      path: 'gateway',
      children: [
        {
          name: '001',
          path: 'gateway/001',
          children: [
            { name: 'status', path: 'gateway/001/status', children: [], subscriberCount: 2, messageCount: 5000 },
            { name: 'control', path: 'gateway/001/control', children: [], subscriberCount: 1, messageCount: 200 }
          ],
          subscriberCount: 3,
          messageCount: 5200
        }
      ],
      subscriberCount: 3,
      messageCount: 5200
    },
    {
      name: 'analytics',
      path: 'analytics',
      children: [
        { name: 'events', path: 'analytics/events', children: [], subscriberCount: 1, messageCount: 100000 },
        { name: 'metrics', path: 'analytics/metrics', children: [], subscriberCount: 0, messageCount: 5000 }
      ],
      subscriberCount: 1,
      messageCount: 105000
    },
    {
      name: 'system',
      path: 'system',
      children: [
        { name: 'heartbeat', path: 'system/heartbeat', children: [], subscriberCount: 2, messageCount: 86400 },
        { name: 'logs', path: 'system/logs', children: [], subscriberCount: 0, messageCount: 10000 }
      ],
      subscriberCount: 2,
      messageCount: 96400
    },
    {
      name: 'devices',
      path: 'devices',
      children: [
        { name: '+',
          path: 'devices/+',
          children: [
            { name: 'status', path: 'devices/+/status', children: [], subscriberCount: 1, messageCount: 5000 }
          ],
          subscriberCount: 1,
          messageCount: 5000
        }
      ],
      subscriberCount: 1,
      messageCount: 5000
    },
    {
      name: 'config',
      path: 'config',
      children: [
        { name: 'updates', path: 'config/updates', children: [], subscriberCount: 1, messageCount: 100 }
      ],
      subscriberCount: 1,
      messageCount: 100
    }
  ],
  subscriberCount: 16,
  messageCount: 241890
};

export const mockRetainedMessages: RetainedMessage[] = [
  {
    id: '1',
    topic: 'sensors/temperature/config',
    payload: '{"interval": 30, "unit": "celsius"}',
    qos: 1,
    timestamp: '2024-01-20T08:00:00Z',
    encoding: 'plain',
    retain: true,
    payloadFormatIndicator: true,
    messageExpiryInterval: 86400,
    contentType: 'application/json',
    userProperties: [{ key: 'sensor_type', value: 'temperature' }]
  },
  {
    id: '2',
    topic: 'gateway/001/status',
    payload: '{"online": true, "version": "2.1.0"}',
    qos: 1,
    timestamp: '2024-01-20T07:00:00Z',
    encoding: 'plain',
    retain: true,
    payloadFormatIndicator: true,
    contentType: 'application/json',
    responseTopic: 'gateway/001/response',
    correlationData: 'gateway_001_status_req',
    userProperties: [{ key: 'gateway_id', value: '001' }]
  },
  {
    id: '3',
    topic: 'system/heartbeat',
    payload: 'alive',
    qos: 0,
    timestamp: '2024-01-20T00:00:00Z',
    encoding: 'plain',
    retain: true
  },
  {
    id: '4',
    topic: 'config/updates',
    payload: 'ewogICJ2ZXJzaW9uIjogIjEuMi4zIiwKICAiZmVhdHVyZXMiOiBbImVuY3J5cHRpb24iLCAiY29tcHJlc3Npb24iXQp9',
    qos: 2,
    timestamp: '2024-01-19T12:00:00Z',
    encoding: 'base64',
    retain: true,
    payloadFormatIndicator: true,
    messageExpiryInterval: 604800,
    contentType: 'application/json'
  },
  {
    id: '5',
    topic: 'devices/last_will',
    payload: '7b226d657373616765223a2022646576696365206f66666c696e65227d',
    qos: 1,
    timestamp: '2024-01-18T15:30:00Z',
    encoding: 'hex',
    retain: true,
    payloadFormatIndicator: false
  }
];

export const mockOfflineMessages: OfflineMessage[] = [
  {
    id: '1',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    payload: '{"type": "alert", "message": "Temperature threshold exceeded"}',
    qos: 2,
    timestamp: '2024-01-20T09:30:00Z',
    expiryTime: '2024-01-21T09:30:00Z',
    retain: false,
    payloadFormatIndicator: true,
    messageExpiryInterval: 86400,
    contentType: 'application/json',
    userProperties: [{ key: 'priority', value: 'high' }]
  },
  {
    id: '2',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    payload: '{"type": "info", "message": "Device connected"}',
    qos: 1,
    timestamp: '2024-01-20T09:25:00Z',
    expiryTime: '2024-01-21T09:25:00Z',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  },
  {
    id: '3',
    clientId: 'config_client',
    topic: 'config/updates',
    payload: '{"version": "1.2.4", "changes": ["bug fixes"]}',
    qos: 2,
    timestamp: '2024-01-20T08:00:00Z',
    expiryTime: '2024-01-22T08:00:00Z',
    retain: true,
    payloadFormatIndicator: true,
    messageExpiryInterval: 172800,
    contentType: 'application/json',
    responseTopic: 'config/response',
    correlationData: 'config_update_001'
  },
  {
    id: '4',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    payload: '{"type": "warning", "message": "Low battery"}',
    qos: 1,
    timestamp: '2024-01-20T07:45:00Z',
    expiryTime: '2024-01-21T07:45:00Z',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  },
  {
    id: '5',
    clientId: 'analytics_001',
    topic: 'analytics/events',
    payload: '{"event": "user_login", "user_id": "12345"}',
    qos: 1,
    timestamp: '2024-01-20T06:30:00Z',
    expiryTime: '2024-01-21T06:30:00Z',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json',
    userProperties: [{ key: 'event_type', value: 'user_activity' }]
  },
  {
    id: '6',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    payload: '{"type": "success", "message": "Update completed"}',
    qos: 2,
    timestamp: '2024-01-20T05:00:00Z',
    expiryTime: '2024-01-21T05:00:00Z',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  }
];

export const mockBrokerStats: BrokerStats = {
  version: '5.0.26',
  uptime: 172800,
  clientsConnected: 6,
  clientsMaximum: 1000,
  messagesReceived: 150000,
  messagesSent: 120000,
  bytesReceived: 15000000,
  bytesSent: 12000000,
  retainedMessages: 5,
  subscriptionsCount: 16,
  bytesPerSecond: 12500
};

export const mockDashboardStats: DashboardStats = {
  totalUsers: 5,
  totalClients: 6,
  totalSubscriptions: 16,
  totalRetainedMessages: 5,
  totalOfflineMessages: 6,
  messagesPerSecond: 45.5,
  bytesPerSecond: 12500
};

export const mockDelayedMessages: DelayedMessage[] = [
  {
    id: '1',
    clientId: 'sensor_001',
    topic: 'sensors/temperature/data',
    payload: '{"temperature": 25.5, "unit": "celsius"}',
    qos: 1,
    publishTime: '2024-01-20T08:00:00Z',
    delaySeconds: 300,
    scheduledTime: new Date(Date.now() + 180000).toISOString(),
    encoding: 'plain',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json',
    userProperties: [{ key: 'sensor_type', value: 'temperature' }]
  },
  {
    id: '2',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    payload: '{"type": "reminder", "message": "Meeting in 5 minutes"}',
    qos: 2,
    publishTime: '2024-01-20T09:00:00Z',
    delaySeconds: 600,
    scheduledTime: new Date(Date.now() + 420000).toISOString(),
    encoding: 'plain',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json',
    responseTopic: 'app/user123/response',
    correlationData: 'reminder_001'
  },
  {
    id: '3',
    clientId: 'gateway_001',
    topic: 'gateway/001/heartbeat',
    payload: '{"status": "alive", "timestamp": 1705737600}',
    qos: 0,
    publishTime: '2024-01-20T07:30:00Z',
    delaySeconds: 60,
    scheduledTime: new Date(Date.now() + 45000).toISOString(),
    encoding: 'plain',
    retain: true,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  },
  {
    id: '4',
    clientId: 'analytics_001',
    topic: 'analytics/events',
    payload: '{"event": "batch_process", "data": "base64encoded"}',
    qos: 1,
    publishTime: '2024-01-20T06:00:00Z',
    delaySeconds: 3600,
    scheduledTime: new Date(Date.now() + 900000).toISOString(),
    encoding: 'base64',
    retain: false,
    payloadFormatIndicator: true,
    messageExpiryInterval: 7200,
    contentType: 'application/json'
  },
  {
    id: '5',
    clientId: 'device_manager',
    topic: 'devices/command',
    payload: '{"command": "restart", "device_id": "dev_001"}',
    qos: 2,
    publishTime: '2024-01-20T10:00:00Z',
    delaySeconds: 1800,
    scheduledTime: new Date(Date.now() + 600000).toISOString(),
    encoding: 'plain',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json',
    userProperties: [
      { key: 'priority', value: 'high' },
      { key: 'source', value: 'management_console' }
    ]
  },
  {
    id: '6',
    clientId: 'sensor_002',
    topic: 'sensors/humidity/data',
    payload: '{"humidity": 65, "unit": "percent"}',
    qos: 1,
    publishTime: '2024-01-20T08:15:00Z',
    delaySeconds: 120,
    scheduledTime: new Date(Date.now() + 30000).toISOString(),
    encoding: 'plain',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  },
  {
    id: '7',
    clientId: 'mobile_client_001',
    topic: 'app/user123/alerts',
    payload: '{"type": "warning", "message": "Low battery"}',
    qos: 1,
    publishTime: '2024-01-20T09:30:00Z',
    delaySeconds: 900,
    scheduledTime: new Date(Date.now() + 720000).toISOString(),
    encoding: 'plain',
    retain: false,
    payloadFormatIndicator: true,
    contentType: 'application/json'
  },
  {
    id: '8',
    clientId: 'config_client',
    topic: 'config/updates',
    payload: '{"version": "1.2.5", "changes": ["security_patch"]}',
    qos: 2,
    publishTime: '2024-01-20T11:00:00Z',
    delaySeconds: 7200,
    scheduledTime: new Date(Date.now() + 3600000).toISOString(),
    encoding: 'plain',
    retain: true,
    payloadFormatIndicator: true,
    messageExpiryInterval: 86400,
    contentType: 'application/json',
    responseTopic: 'config/response',
    correlationData: 'config_update_002'
  }
];

export const mqttVersions = ['3.0', '3.1.1', '5.0'] as const;

export const qosLevels = [
  { value: 0, label: 'QoS 0 - 最多一次', description: '消息可能丢失' },
  { value: 1, label: 'QoS 1 - 至少一次', description: '消息可能重复' },
  { value: 2, label: 'QoS 2 - 恰好一次', description: '消息保证送达' }
];


export const mockClusterNodes: ClusterNode[] = [
  {
    id: '1',
    nodeId: 'node-keeper-001',
    ip: '192.168.1.10',
    port: 1883,
    role: 'keeper',
    status: 'online',
    joinedAt: '2024-01-15T08:00:00Z',
    lastHeartbeat: '2024-01-20T10:30:00Z',
    cpuUsage: 45.2,
    memoryUsage: 62.8,
    connections: 1250,
    messagesPerSecond: 1250.5
  },
  {
    id: '2',
    nodeId: 'node-keeper-002',
    ip: '192.168.1.11',
    port: 1883,
    role: 'keeper',
    status: 'online',
    joinedAt: '2024-01-15T08:05:00Z',
    lastHeartbeat: '2024-01-20T10:30:05Z',
    cpuUsage: 38.5,
    memoryUsage: 55.3,
    connections: 1180,
    messagesPerSecond: 1180.2
  },
  {
    id: '3',
    nodeId: 'node-worker-001',
    ip: '192.168.1.20',
    port: 1884,
    role: 'worker',
    status: 'online',
    joinedAt: '2024-01-16T09:00:00Z',
    lastHeartbeat: '2024-01-20T10:29:55Z',
    cpuUsage: 52.1,
    memoryUsage: 68.5,
    connections: 890,
    messagesPerSecond: 890.3
  },
  {
    id: '4',
    nodeId: 'node-worker-002',
    ip: '192.168.1.21',
    port: 1884,
    role: 'worker',
    status: 'online',
    joinedAt: '2024-01-16T09:10:00Z',
    lastHeartbeat: '2024-01-20T10:30:02Z',
    cpuUsage: 41.8,
    memoryUsage: 59.2,
    connections: 756,
    messagesPerSecond: 756.8
  },
  {
    id: '5',
    nodeId: 'node-worker-003',
    ip: '192.168.1.22',
    port: 1884,
    role: 'worker',
    status: 'offline',
    joinedAt: '2024-01-17T10:00:00Z',
    lastHeartbeat: '2024-01-20T09:45:00Z',
    cpuUsage: 0,
    memoryUsage: 0,
    connections: 0,
    messagesPerSecond: 0
  }
];

export const mockClientDetails: ClientDetail[] = [
  {
    id: '1',
    clientId: 'sensor_001',
    node: 'node-keeper-001',
    version: 'JMqtt-Client/1.2.0',
    status: 'online',
    cleanStart: true,
    keepalive: 60,
    connectedAt: '2024-01-20T08:30:00Z',
    lastSeen: '2024-01-20T10:30:00Z',
    connectionDuration: 7200,
    messagesPerSecond: 2.1,
    connectionProps: {
      sessionExpiryInterval: 0,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: false,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true,
      userProperties: [
        { key: 'client_type', value: 'sensor' },
        { key: 'firmware_version', value: '2.1.0' }
      ]
    },
    willMessage: {
      topic: 'sensors/sensor_001/status',
      payload: '{"status": "offline", "reason": "unexpected_disconnect"}',
      qos: 1,
      retain: true,
      payloadFormatIndicator: true,
      willDelayInterval: 0,
      messageExpiryInterval: 3600,
      contentType: 'application/json',
      responseTopic: 'sensors/sensor_001/response',
      correlationData: 'sensor_001_correlation_001',
      userProperties: [
        { key: 'device_id', value: 'sensor_001' },
        { key: 'location', value: 'building_a_floor_2' }
      ]
    }
  },
  {
    id: '2',
    clientId: 'sensor_002',
    node: 'node-keeper-001',
    version: 'JMqtt-Client/1.2.0',
    status: 'online',
    cleanStart: true,
    keepalive: 60,
    connectedAt: '2024-01-20T08:35:00Z',
    lastSeen: '2024-01-20T10:29:55Z',
    connectionDuration: 6900,
    messagesPerSecond: 1.8,
    connectionProps: {
      sessionExpiryInterval: 0,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: false,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true
    },
    willMessage: {
      topic: 'sensors/sensor_002/status',
      payload: '{"status": "offline"}',
      qos: 1,
      retain: true,
      payloadFormatIndicator: true,
      willDelayInterval: 0,
      messageExpiryInterval: 3600,
      contentType: 'application/json'
    }
  },
  {
    id: '3',
    clientId: 'mobile_client_001',
    node: 'node-worker-001',
    version: 'JMqtt-Client/2.0.0',
    status: 'online',
    cleanStart: false,
    keepalive: 300,
    connectedAt: '2024-01-20T09:00:00Z',
    lastSeen: '2024-01-20T10:30:00Z',
    connectionDuration: 5400,
    messagesPerSecond: 0.5,
    connectionProps: {
      sessionExpiryInterval: 86400,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: true,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true,
      userProperties: [
        { key: 'client_type', value: 'mobile_app' },
        { key: 'platform', value: 'iOS' },
        { key: 'app_version', value: '3.2.1' }
      ]
    },
    willMessage: {
      topic: 'app/user123/last_will',
      payload: '{"user_id": "user123", "status": "disconnected", "timestamp": "2024-01-20T10:30:00Z"}',
      qos: 2,
      retain: false,
      payloadFormatIndicator: true,
      willDelayInterval: 60,
      messageExpiryInterval: 0,
      contentType: 'application/json',
      responseTopic: 'app/user123/response',
      correlationData: 'mobile_client_001_will',
      userProperties: [
        { key: 'user_id', value: 'user123' },
        { key: 'device_type', value: 'iPhone' }
      ]
    }
  },
  {
    id: '4',
    clientId: 'gateway_001',
    node: 'node-keeper-002',
    version: 'JMqtt-Gateway/2.1.0',
    status: 'online',
    cleanStart: false,
    keepalive: 120,
    connectedAt: '2024-01-20T07:00:00Z',
    lastSeen: '2024-01-20T10:30:02Z',
    connectionDuration: 12600,
    messagesPerSecond: 8.5,
    connectionProps: {
      sessionExpiryInterval: 604800,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: true,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true,
      userProperties: [
        { key: 'client_type', value: 'gateway' },
        { key: 'gateway_version', value: '2.1.0' },
        { key: 'connected_devices', value: '25' }
      ]
    },
    willMessage: {
      topic: 'gateway/001/last_will',
      payload: '{"gateway_id": "001", "status": "offline", "connected_devices": 0}',
      qos: 1,
      retain: true,
      payloadFormatIndicator: true,
      willDelayInterval: 30,
      messageExpiryInterval: 86400,
      contentType: 'application/json',
      userProperties: [
        { key: 'gateway_id', value: '001' },
        { key: 'alert_level', value: 'critical' }
      ]
    }
  },
  {
    id: '5',
    clientId: 'analytics_001',
    node: 'node-worker-002',
    version: 'JMqtt-Client/1.5.0',
    status: 'online',
    cleanStart: true,
    keepalive: 60,
    connectedAt: '2024-01-20T06:00:00Z',
    lastSeen: '2024-01-20T10:29:58Z',
    connectionDuration: 16200,
    messagesPerSecond: 15.2,
    connectionProps: {
      sessionExpiryInterval: 0,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: false,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true,
      userProperties: [
        { key: 'client_type', value: 'analytics' },
        { key: 'service_name', value: 'analytics_service' }
      ]
    }
  },
  {
    id: '6',
    clientId: 'device_manager',
    node: 'node-keeper-001',
    version: 'JMqtt-Client/2.0.0',
    status: 'online',
    cleanStart: false,
    keepalive: 60,
    connectedAt: '2024-01-20T10:00:00Z',
    lastSeen: '2024-01-20T10:30:00Z',
    connectionDuration: 1800,
    messagesPerSecond: 3.2,
    connectionProps: {
      sessionExpiryInterval: 3600,
      receiveMaximum: 65535,
      maximumPacketSize: 268435456,
      topicAliasMaximum: 65535,
      requestResponseInfo: true,
      requestProblemInfo: true,
      retainAvailable: true,
      wildcardSubscriptionAvailable: true,
      sharedSubscriptionAvailable: true,
      subscriptionIdentifierAvailable: true,
      userProperties: [
        { key: 'client_type', value: 'management' },
        { key: 'role', value: 'admin' }
      ]
    },
    willMessage: {
      topic: 'system/device_manager/status',
      payload: '{"service": "device_manager", "status": "offline"}',
      qos: 1,
      retain: true,
      payloadFormatIndicator: true,
      willDelayInterval: 0,
      messageExpiryInterval: 3600,
      contentType: 'application/json'
    }
  }
];

export const mockBlacklist: BlacklistItem[] = [
  {
    id: '1',
    type: 'clientId',
    value: 'malicious_client_001',
    reason: '频繁发送异常消息',
    duration: 86400,
    isPermanent: false,
    createdAt: '2024-01-19T10:00:00Z',
    expiresAt: '2024-01-20T10:00:00Z',
    createdBy: 'admin'
  },
  {
    id: '2',
    type: 'ip',
    value: '192.168.1.99',
    reason: '暴力破解攻击',
    duration: 0,
    isPermanent: true,
    createdAt: '2024-01-18T08:00:00Z',
    createdBy: 'admin'
  },
  {
    id: '3',
    type: 'clientId',
    value: 'spam_client_001',
    reason: '消息 spam',
    duration: 3600,
    isPermanent: false,
    createdAt: '2024-01-20T09:00:00Z',
    expiresAt: '2024-01-20T10:00:00Z',
    createdBy: 'admin'
  }
];

export const mockSubscriptionDetails: SubscriptionDetail[] = [
  {
    id: 'sub-001',
    clientId: 'sensor_001',
    topic: 'sensors/temperature/data',
    qos: 1,
    options: { noLocal: false, retainAsPublished: false, retainHandling: 0 },
    subscribedAt: '2024-01-20T08:30:00Z'
  },
  {
    id: 'sub-002',
    clientId: 'sensor_002',
    topic: 'sensors/humidity/data',
    qos: 1,
    options: { noLocal: false, retainAsPublished: false, retainHandling: 0 },
    subscribedAt: '2024-01-20T08:35:00Z'
  },
  {
    id: 'sub-003',
    clientId: 'mobile_client_001',
    topic: 'app/user123/notifications',
    qos: 2,
    options: { noLocal: true, retainAsPublished: true, retainHandling: 1 },
    subscribedAt: '2024-01-20T09:00:00Z'
  },
  {
    id: 'sub-004',
    clientId: 'gateway_001',
    topic: 'gateway/001/status',
    qos: 0,
    options: { noLocal: false, retainAsPublished: false, retainHandling: 0 },
    subscribedAt: '2024-01-20T07:00:00Z'
  },
  {
    id: 'sub-005',
    clientId: 'analytics_001',
    topic: 'analytics/events',
    qos: 1,
    options: { noLocal: false, retainAsPublished: false, retainHandling: 0 },
    subscribedAt: '2024-01-20T06:00:00Z'
  }
];
