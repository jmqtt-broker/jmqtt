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

export interface NodeStats {
  nodeId: string;
  timestamp: string;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkIn: number;
  networkOut: number;
}

export interface SubscriptionDetail {
  id: string;
  clientId: string;
  topic: string;
  qos: number;
  options: SubscriptionOptions;
  subscribedAt: string;
}

export interface SubscriptionOptions {
  noLocal?: boolean;
  retainAsPublished?: boolean;
  retainHandling?: number;
}

export interface TopicSubscription {
  topic: string;
  subscriberCount: number;
  subscriptions: SubscriptionDetail[];
}
