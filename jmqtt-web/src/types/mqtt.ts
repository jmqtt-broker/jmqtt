export interface MQTTUser {
  id: string;
  username: string;
  password?: string;
  isSuperuser: boolean;
  createdAt: string;
  updatedAt: string;
  aclRules?: ACLRule[];
}

export interface ACLRule {
  id: string;
  userId: string;
  topic: string;
  permission: 'allow' | 'deny';
  action: 'publish' | 'subscribe' | 'pubsub';
  qos: number[];
}

export interface MQTTClient {
  id: string;
  clientId: string;
  username?: string;
  peername: string;
  connectedAt: string;
  keepalive: number;
  protocolVersion: string;
  cleanSession: boolean;
  isBridge: boolean;
  subscriptions: Subscription[];
  messagesReceived: number;
  messagesSent: number;
  bytesReceived: number;
  bytesSent: number;
}

export interface Subscription {
  id: string;
  topic: string;
  qos: number;
  clientId: string;
}

export interface TopicNode {
  name: string;
  path: string;
  children: TopicNode[];
  subscriberCount: number;
  messageCount: number;
}

export interface RetainedMessage {
  id: string;
  topic: string;
  payload: string;
  qos: number;
  timestamp: string;
  encoding: 'plain' | 'base64' | 'hex';
  retain: boolean;
  payloadFormatIndicator?: boolean;
  messageExpiryInterval?: number;
  contentType?: string;
  responseTopic?: string;
  correlationData?: string;
  userProperties?: { key: string; value: string }[];
}

export interface OfflineMessage {
  id: string;
  clientId: string;
  topic: string;
  payload: string;
  qos: number;
  timestamp: string;
  expiryTime?: string;
  retain?: boolean;
  payloadFormatIndicator?: boolean;
  messageExpiryInterval?: number;
  contentType?: string;
  responseTopic?: string;
  correlationData?: string;
  userProperties?: { key: string; value: string }[];
}

export interface BrokerStats {
  version: string;
  uptime: number;
  clientsConnected: number;
  clientsMaximum: number;
  messagesReceived: number;
  messagesSent: number;
  bytesReceived: number;
  bytesSent: number;
  retainedMessages: number;
  subscriptionsCount: number;
  bytesPerSecond: number;
}

export type MQTTVersion = '3.0' | '3.1.1' | '5.0';

export interface DashboardStats {
  totalUsers: number;
  totalClients: number;
  totalSubscriptions: number;
  totalRetainedMessages: number;
  totalOfflineMessages: number;
  messagesPerSecond: number;
  bytesPerSecond: number;
}

export interface ClusterNode {
  id: string;
  nodeId: string;
  ip: string;
  port: number;
  role: 'keeper' | 'worker';
  status: 'online' | 'offline' | 'joining' | 'leaving';
  joinedAt: string;
  lastHeartbeat: string;
  cpuUsage: number;
  memoryUsage: number;
  connections: number;
  messagesPerSecond: number;
}

export interface SubscriptionDetail {
  id: string;
  clientId: string;
  topic: string;
  qos: number;
  options: {
    noLocal?: boolean;
    retainAsPublished?: boolean;
    retainHandling?: number;
  };
  subscribedAt: string;
}

export interface ClientDetail {
  id: string;
  clientId: string;
  node: string;
  version: string;
  status: 'online' | 'offline';
  cleanStart: boolean;
  keepalive: number;
  connectedAt: string;
  disconnectedAt?: string;
  lastSeen?: string;
  connectionDuration: number;
  messagesPerSecond: number;
  connectionProps?: {
    sessionExpiryInterval: number;
    receiveMaximum: number;
    maximumPacketSize: number;
    topicAliasMaximum: number;
    requestResponseInfo: boolean;
    requestProblemInfo: boolean;
    retainAvailable: boolean;
    wildcardSubscriptionAvailable: boolean;
    sharedSubscriptionAvailable: boolean;
    subscriptionIdentifierAvailable: boolean;
    userProperties?: { key: string; value: string }[];
  };
  willMessage?: {
    topic: string;
    payload: string;
    qos: number;
    retain: boolean;
    payloadFormatIndicator: boolean;
    willDelayInterval: number;
    messageExpiryInterval: number;
    contentType?: string;
    responseTopic?: string;
    correlationData?: string;
    userProperties?: { key: string; value: string }[];
  };
}

export type BridgeType = 'mqtt' | 'kafka' | 'rocketmq' | 'mysql' | 'postgresql' | 'redis' | 'websocket';

export interface BridgeConfig {
  id: string;
  name: string;
  type: BridgeType;
  enabled: boolean;
  config: Record<string, string | number | boolean>;
  createdAt: string;
  updatedAt: string;
}

export interface BridgeRule {
  id: string;
  bridgeId: string;
  topic: string;
  enabled: boolean;
  createdAt: string;
  kafkaTopic?: string;
  targetTable?: string;
  targetMqttTopic?: string;
  rocketmqTopic?: string;
  redisKey?: string;
}

export interface BridgeStatus {
  id: string;
  status: 'connected' | 'disconnected' | 'error';
  lastError?: string;
  messagesForwarded: number;
  messagesFailed: number;
  lastConnectedAt?: string;
}

export interface DelayedMessage {
  id: string;
  clientId: string;
  topic: string;
  payload: string;
  qos: number;
  publishTime: string;
  delaySeconds: number;
  scheduledTime: string;
  encoding: 'plain' | 'base64' | 'hex';
  retain?: boolean;
  payloadFormatIndicator?: boolean;
  messageExpiryInterval?: number;
  contentType?: string;
  responseTopic?: string;
  correlationData?: string;
  userProperties?: { key: string; value: string }[];
}

export type BlacklistType = 'clientId' | 'ip';

export interface BlacklistItem {
  id: string;
  type: BlacklistType;
  value: string;
  reason?: string;
  duration: number;
  isPermanent: boolean;
  createdAt: string;
  expiresAt?: string;
  createdBy?: string;
}
