import type {
  MQTTUser,
  MQTTClient,
  TopicNode,
  RetainedMessage,
  OfflineMessage,
  BrokerStats,
  DashboardStats,
  ClusterNode,
  SubscriptionDetail,
  ClientDetail,
  DelayedMessage,
  BlacklistItem,
  BlacklistType
} from '../types/mqtt';
import {
  mockUsers,
  mockClients,
  mockTopicTree,
  mockRetainedMessages,
  mockOfflineMessages,
  mockBrokerStats,
  mockDashboardStats,
  mockClusterNodes,
  mockSubscriptionDetails,
  mockClientDetails,
  mockDelayedMessages,
  mockBlacklist
} from '../mock/mqttData';

let users = [...mockUsers];
let clients = [...mockClients];
let retainedMessages = [...mockRetainedMessages];
let offlineMessages = [...mockOfflineMessages];
let delayedMessages = [...mockDelayedMessages];
let blacklist = [...mockBlacklist];

export const mockApi = {
  getDashboardStats(): Promise<DashboardStats> {
    return Promise.resolve({ ...mockDashboardStats });
  },

  getBrokerStats(): Promise<BrokerStats> {
    return Promise.resolve({ ...mockBrokerStats });
  },

  getUsers(): Promise<MQTTUser[]> {
    return Promise.resolve([...users]);
  },

  getUserById(id: string): Promise<MQTTUser | null> {
    const user = users.find(u => u.id === id);
    return Promise.resolve(user ? { ...user } : null);
  },

  createUser(user: Omit<MQTTUser, 'id' | 'createdAt' | 'updatedAt'>): Promise<MQTTUser> {
    const newUser: MQTTUser = {
      ...user,
      id: Math.random().toString(36).substr(2, 9),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    users.push(newUser);
    return Promise.resolve({ ...newUser });
  },

  updateUser(id: string, updates: Partial<MQTTUser>): Promise<MQTTUser | null> {
    const index = users.findIndex(u => u.id === id);
    if (index === -1) return Promise.resolve(null);
    users[index] = { ...users[index], ...updates, updatedAt: new Date().toISOString() };
    return Promise.resolve({ ...users[index] });
  },

  deleteUser(id: string): Promise<boolean> {
    const index = users.findIndex(u => u.id === id);
    if (index === -1) return Promise.resolve(false);
    users.splice(index, 1);
    return Promise.resolve(true);
  },

  getClients(): Promise<MQTTClient[]> {
    return Promise.resolve([...clients]);
  },

  getClientById(id: string): Promise<MQTTClient | null> {
    const client = clients.find(c => c.id === id);
    return Promise.resolve(client ? { ...client } : null);
  },

  disconnectClient(id: string): Promise<boolean> {
    const index = clients.findIndex(c => c.id === id);
    if (index === -1) return Promise.resolve(false);
    clients.splice(index, 1);
    return Promise.resolve(true);
  },

  getTopicTree(): Promise<TopicNode> {
    return Promise.resolve({ ...mockTopicTree });
  },

  getRetainedMessages(): Promise<RetainedMessage[]> {
    return Promise.resolve([...retainedMessages]);
  },

  getRetainedMessageById(id: string): Promise<RetainedMessage | null> {
    const msg = retainedMessages.find(m => m.id === id);
    return Promise.resolve(msg ? { ...msg } : null);
  },

  deleteRetainedMessage(id: string): Promise<boolean> {
    const index = retainedMessages.findIndex(m => m.id === id);
    if (index === -1) return Promise.resolve(false);
    retainedMessages.splice(index, 1);
    return Promise.resolve(true);
  },

  publishRetainedMessage(message: Omit<RetainedMessage, 'id' | 'timestamp'>): Promise<RetainedMessage> {
    const newMessage: RetainedMessage = {
      ...message,
      id: Math.random().toString(36).substr(2, 9),
      timestamp: new Date().toISOString()
    };
    const existingIndex = retainedMessages.findIndex(m => m.topic === message.topic);
    if (existingIndex !== -1) {
      retainedMessages[existingIndex] = newMessage;
    } else {
      retainedMessages.push(newMessage);
    }
    return Promise.resolve({ ...newMessage });
  },

  getOfflineMessages(): Promise<OfflineMessage[]> {
    return Promise.resolve([...offlineMessages]);
  },

  getOfflineMessagesByClientId(clientId: string): Promise<OfflineMessage[]> {
    return Promise.resolve(offlineMessages.filter(m => m.clientId === clientId));
  },

  deleteOfflineMessage(id: string): Promise<boolean> {
    const index = offlineMessages.findIndex(m => m.id === id);
    if (index === -1) return Promise.resolve(false);
    offlineMessages.splice(index, 1);
    return Promise.resolve(true);
  },

  clearOfflineMessages(clientId: string): Promise<number> {
    const initialLength = offlineMessages.length;
    offlineMessages = offlineMessages.filter(m => m.clientId !== clientId);
    return Promise.resolve(initialLength - offlineMessages.length);
  },

  getClusterNodes(): Promise<ClusterNode[]> {
    return Promise.resolve([...mockClusterNodes]);
  },

  getClusterNodeById(id: string): Promise<ClusterNode | null> {
    const node = mockClusterNodes.find(n => n.id === id);
    return Promise.resolve(node ? { ...node } : null);
  },

  getSubscriptionDetails(): Promise<SubscriptionDetail[]> {
    return Promise.resolve([...mockSubscriptionDetails]);
  },

  getSubscriptionDetailsByTopic(topic: string): Promise<SubscriptionDetail[]> {
    return Promise.resolve(mockSubscriptionDetails.filter(s => s.topic === topic));
  },

  getClientDetailById(id: string): Promise<ClientDetail | null> {
    const detail = mockClientDetails.find(c => c.id === id);
    return Promise.resolve(detail ? { ...detail } : null);
  },

  getDelayedMessages(): Promise<DelayedMessage[]> {
    return Promise.resolve([...delayedMessages]);
  },

  getDelayedMessageById(id: string): Promise<DelayedMessage | null> {
    const msg = delayedMessages.find(m => m.id === id);
    return Promise.resolve(msg ? { ...msg } : null);
  },

  deleteDelayedMessage(id: string): Promise<boolean> {
    const index = delayedMessages.findIndex(m => m.id === id);
    if (index === -1) return Promise.resolve(false);
    delayedMessages.splice(index, 1);
    return Promise.resolve(true);
  },

  getBlacklist(): Promise<BlacklistItem[]> {
    return Promise.resolve([...blacklist]);
  },

  getBlacklistById(id: string): Promise<BlacklistItem | null> {
    const item = blacklist.find(b => b.id === id);
    return Promise.resolve(item ? { ...item } : null);
  },

  addToBlacklist(item: { type: BlacklistType; value: string; reason?: string; isPermanent: boolean; duration: number }): Promise<BlacklistItem> {
    const now = new Date();
    const newItem: BlacklistItem = {
      id: Math.random().toString(36).substr(2, 9),
      type: item.type,
      value: item.value,
      reason: item.reason,
      duration: item.duration,
      isPermanent: item.isPermanent,
      createdAt: now.toISOString(),
      createdBy: 'admin',
      expiresAt: item.isPermanent ? undefined : new Date(now.getTime() + item.duration * 1000).toISOString()
    };
    blacklist.push(newItem);

    if (item.type === 'clientId') {
      const clientIndex = clients.findIndex(c => c.clientId === item.value);
      if (clientIndex !== -1) {
        clients.splice(clientIndex, 1);
      }
    }

    return Promise.resolve({ ...newItem });
  },

  removeFromBlacklist(id: string): Promise<boolean> {
    const index = blacklist.findIndex(b => b.id === id);
    if (index === -1) return Promise.resolve(false);
    blacklist.splice(index, 1);
    return Promise.resolve(true);
  },

  isBlacklisted(type: BlacklistType, value: string): Promise<boolean> {
    const now = new Date();
    const isBlocked = blacklist.some(b => {
      if (b.type !== type || b.value !== value) return false;
      if (b.isPermanent) return true;
      if (!b.expiresAt) return false;
      return new Date(b.expiresAt) > now;
    });
    return Promise.resolve(isBlocked);
  }
};
