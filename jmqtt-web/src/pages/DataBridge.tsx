import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  Trash2,
  Edit3,
  Power,
  PowerOff,
  CheckCircle2,
  AlertCircle,
  Server,
  Database,
  MessageSquare,
  Settings,
  Link2,
  X,
  ChevronDown,
  ChevronRight,
  Activity,
  ArrowRight,
  Globe
} from 'lucide-react';
import type { BridgeConfig, BridgeRule, BridgeType, BridgeStatus } from '../types/mqtt';

const bridgeTypes: { type: BridgeType; label: string; icon: React.ElementType; description: string }[] = [
  { type: 'mqtt', label: 'MQTT', icon: MessageSquare, description: '转发到另一个 MQTT Broker' },
  { type: 'kafka', label: 'Kafka', icon: Server, description: '转发到 Kafka 消息队列' },
  { type: 'rocketmq', label: 'RocketMQ', icon: Server, description: '转发到 RocketMQ 消息队列' },
  { type: 'mysql', label: 'MySQL', icon: Database, description: '转发到 MySQL 数据库' },
  { type: 'postgresql', label: 'PostgreSQL', icon: Database, description: '转发到 PostgreSQL 数据库' },
  { type: 'redis', label: 'Redis', icon: Database, description: '转发到 Redis 缓存' },
  { type: 'websocket', label: 'WebSocket', icon: Globe, description: '转发到 WebSocket 服务' }
];

const defaultConfigs: Record<BridgeType, Record<string, string>> = {
  mqtt: {
    host: '',
    port: '1883',
    username: '',
    password: '',
    clientId: '',
    ssl: 'false'
  },
  kafka: {
    bootstrapServers: '',
    acks: '1',
    retries: '3'
  },
  rocketmq: {
    nameServer: '',
    group: '',
    accessKey: '',
    secretKey: ''
  },
  mysql: {
    host: '',
    port: '3306',
    database: '',
    username: '',
    password: ''
  },
  postgresql: {
    host: '',
    port: '5432',
    database: '',
    username: '',
    password: ''
  },
  redis: {
    host: '',
    port: '6379',
    password: '',
    database: '0'
  },
  websocket: {
    url: '',
    reconnect: 'true',
    reconnectInterval: '5000',
    heartbeat: 'true',
    heartbeatInterval: '30000'
  }
};

const demoConfigs: Record<BridgeType, { name: string; config: Record<string, string> }> = {
  mqtt: {
    name: 'MQTT 桥接 Demo',
    config: {
      host: 'remote-mqtt-broker.example.com',
      port: '1883',
      username: 'bridge_user',
      password: '',
      clientId: 'jmqtt-bridge-001',
      ssl: 'false'
    }
  },
  kafka: {
    name: 'Kafka 消息桥接 Demo',
    config: {
      bootstrapServers: 'localhost:9092',
      acks: '1',
      retries: '3'
    }
  },
  rocketmq: {
    name: 'RocketMQ 桥接 Demo',
    config: {
      nameServer: 'localhost:9876',
      group: 'mqtt-bridge-group',
      accessKey: 'rocketmq-access-key',
      secretKey: ''
    }
  },
  mysql: {
    name: 'MySQL 数据存储 Demo',
    config: {
      host: 'localhost',
      port: '3306',
      database: 'mqtt_data',
      username: 'root',
      password: ''
    }
  },
  postgresql: {
    name: 'PostgreSQL 存储 Demo',
    config: {
      host: 'localhost',
      port: '5432',
      database: 'mqtt_analytics',
      username: 'postgres',
      password: ''
    }
  },
  redis: {
    name: 'Redis 缓存 Demo',
    config: {
      host: 'localhost',
      port: '6379',
      password: '',
      database: '0'
    }
  },
  websocket: {
    name: 'WebSocket 实时推送 Demo',
    config: {
      url: 'wss://ws.example.com/mqtt-bridge',
      reconnect: 'true',
      reconnectInterval: '5000',
      heartbeat: 'true',
      heartbeatInterval: '30000'
    }
  }
};

const mockBridges: BridgeConfig[] = [
  {
    id: '1',
    name: 'Kafka 消息桥接 Demo',
    type: 'kafka',
    enabled: true,
    config: {
      bootstrapServers: 'localhost:9092',
      acks: '1',
      retries: '3'
    },
    createdAt: '2024-01-20T08:00:00Z',
    updatedAt: '2024-01-20T08:00:00Z'
  },
  {
    id: '2',
    name: 'MySQL 数据存储 Demo',
    type: 'mysql',
    enabled: true,
    config: {
      host: 'localhost',
      port: '3306',
      database: 'mqtt_data',
      username: 'root',
      password: ''
    },
    createdAt: '2024-01-20T09:00:00Z',
    updatedAt: '2024-01-20T09:00:00Z'
  },
  {
    id: '3',
    name: 'MQTT 桥接 Demo',
    type: 'mqtt',
    enabled: true,
    config: {
      host: 'remote-mqtt-broker.example.com',
      port: '1883',
      username: 'bridge_user',
      password: '',
      clientId: 'jmqtt-bridge-001',
      ssl: 'false'
    },
    createdAt: '2024-01-20T10:00:00Z',
    updatedAt: '2024-01-20T10:00:00Z'
  },
  {
    id: '4',
    name: 'RocketMQ 桥接 Demo',
    type: 'rocketmq',
    enabled: true,
    config: {
      nameServer: 'localhost:9876',
      group: 'mqtt-bridge-group',
      accessKey: 'rocketmq-access-key',
      secretKey: ''
    },
    createdAt: '2024-01-20T11:00:00Z',
    updatedAt: '2024-01-20T11:00:00Z'
  },
  {
    id: '5',
    name: 'PostgreSQL 存储 Demo',
    type: 'postgresql',
    enabled: true,
    config: {
      host: 'localhost',
      port: '5432',
      database: 'mqtt_analytics',
      username: 'postgres',
      password: ''
    },
    createdAt: '2024-01-20T12:00:00Z',
    updatedAt: '2024-01-20T12:00:00Z'
  },
  {
    id: '6',
    name: 'Redis 缓存 Demo',
    type: 'redis',
    enabled: true,
    config: {
      host: 'localhost',
      port: '6379',
      password: '',
      database: '0'
    },
    createdAt: '2024-01-20T13:00:00Z',
    updatedAt: '2024-01-20T13:00:00Z'
  },
  {
    id: '7',
    name: 'WebSocket 实时推送 Demo',
    type: 'websocket',
    enabled: true,
    config: {
      url: 'wss://ws.example.com/mqtt-bridge',
      reconnect: 'true',
      reconnectInterval: '5000',
      heartbeat: 'true',
      heartbeatInterval: '30000'
    },
    createdAt: '2024-01-20T14:00:00Z',
    updatedAt: '2024-01-20T14:00:00Z'
  }
];

const mockRules: BridgeRule[] = [
  { id: '1', bridgeId: '1', topic: 'sensors/+/data', kafkaTopic: 'mqtt-sensors-data', enabled: true, createdAt: '2024-01-20T08:00:00Z' },
  { id: '2', bridgeId: '1', topic: 'devices/+/status', kafkaTopic: 'mqtt-devices-status', enabled: true, createdAt: '2024-01-20T08:30:00Z' },
  { id: '3', bridgeId: '1', topic: 'alerts/+', kafkaTopic: 'mqtt-alerts', enabled: true, createdAt: '2024-01-20T08:45:00Z' },
  { id: '4', bridgeId: '2', topic: 'analytics/events', targetTable: 'events', enabled: true, createdAt: '2024-01-20T09:00:00Z' },
  { id: '5', bridgeId: '2', topic: 'sensors/+/data', targetTable: 'sensor_data', enabled: true, createdAt: '2024-01-20T09:15:00Z' },
  { id: '6', bridgeId: '3', topic: 'alerts/+', targetMqttTopic: 'forwarded/alerts', enabled: true, createdAt: '2024-01-20T10:00:00Z' },
  { id: '7', bridgeId: '3', topic: 'commands/+', targetMqttTopic: 'remote/commands', enabled: true, createdAt: '2024-01-20T10:30:00Z' },
  { id: '8', bridgeId: '4', topic: 'notifications/+', rocketmqTopic: 'mqtt-notifications', enabled: true, createdAt: '2024-01-20T11:00:00Z' },
  { id: '9', bridgeId: '4', topic: 'events/+', rocketmqTopic: 'mqtt-events', enabled: true, createdAt: '2024-01-20T11:30:00Z' },
  { id: '10', bridgeId: '5', topic: 'metrics/+', targetTable: 'metrics', enabled: true, createdAt: '2024-01-20T12:00:00Z' },
  { id: '11', bridgeId: '5', topic: 'logs/+', targetTable: 'system_logs', enabled: true, createdAt: '2024-01-20T12:30:00Z' },
  { id: '12', bridgeId: '6', topic: 'cache/updates', redisKey: 'mqtt:cache:updates', enabled: true, createdAt: '2024-01-20T13:00:00Z' },
  { id: '13', bridgeId: '6', topic: 'session/+', redisKey: 'mqtt:session:{topic}', enabled: true, createdAt: '2024-01-20T13:30:00Z' },
  { id: '14', bridgeId: '7', topic: 'realtime/data', enabled: true, createdAt: '2024-01-20T14:00:00Z' },
  { id: '15', bridgeId: '7', topic: 'live/updates', enabled: true, createdAt: '2024-01-20T14:30:00Z' }
];

const mockStatuses: Record<string, BridgeStatus> = {
  '1': {
    id: '1',
    status: 'connected',
    messagesForwarded: 15420,
    messagesFailed: 0,
    lastConnectedAt: '2024-01-20T08:00:00Z'
  },
  '2': {
    id: '2',
    status: 'connected',
    messagesForwarded: 8930,
    messagesFailed: 5,
    lastConnectedAt: '2024-01-20T09:00:00Z'
  },
  '3': {
    id: '3',
    status: 'connected',
    messagesForwarded: 8920,
    messagesFailed: 12,
    lastConnectedAt: '2024-01-20T10:00:00Z'
  },
  '4': {
    id: '4',
    status: 'connected',
    messagesForwarded: 5670,
    messagesFailed: 0,
    lastConnectedAt: '2024-01-20T11:00:00Z'
  },
  '5': {
    id: '5',
    status: 'connected',
    messagesForwarded: 4560,
    messagesFailed: 2,
    lastConnectedAt: '2024-01-20T12:00:00Z'
  },
  '6': {
    id: '6',
    status: 'connected',
    messagesForwarded: 23450,
    messagesFailed: 3,
    lastConnectedAt: '2024-01-20T13:00:00Z'
  },
  '7': {
    id: '7',
    status: 'connected',
    messagesForwarded: 12340,
    messagesFailed: 0,
    lastConnectedAt: '2024-01-20T14:00:00Z'
  }
};

export default function DataBridge() {
  const [bridges, setBridges] = useState<BridgeConfig[]>(mockBridges);
  const [rules, setRules] = useState<BridgeRule[]>(mockRules);
  const [statuses, setStatuses] = useState<Record<string, BridgeStatus>>(mockStatuses);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRuleModal, setShowRuleModal] = useState(false);
  const [editingBridge, setEditingBridge] = useState<BridgeConfig | null>(null);
  const [selectedBridge, setSelectedBridge] = useState<BridgeConfig | null>(null);
  const [expandedBridges, setExpandedBridges] = useState<string[]>(['1']);
  const [newBridge, setNewBridge] = useState<Partial<BridgeConfig>>({
    type: 'kafka',
    enabled: true,
    config: { ...defaultConfigs.kafka }
  });
  const [newRule, setNewRule] = useState<{ topic: string; enabled: boolean; kafkaTopic?: string; targetTable?: string; targetMqttTopic?: string; rocketmqTopic?: string; redisKey?: string }>({ topic: '', enabled: true });
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showRuleDeleteConfirm, setShowRuleDeleteConfirm] = useState(false);
  const [bridgeToDelete, setBridgeToDelete] = useState<string | null>(null);
  const [ruleToDelete, setRuleToDelete] = useState<string | null>(null);

  const toggleBridge = (id: string) => {
    setBridges(prev => prev.map(b => b.id === id ? { ...b, enabled: !b.enabled } : b));
  };

  const deleteBridge = (id: string) => {
    setBridgeToDelete(id);
    setShowDeleteConfirm(true);
  };

  const confirmDeleteBridge = () => {
    if (bridgeToDelete) {
      setBridges(prev => prev.filter(b => b.id !== bridgeToDelete));
      setRules(prev => prev.filter(r => r.bridgeId !== bridgeToDelete));
      setShowDeleteConfirm(false);
      setBridgeToDelete(null);
    }
  };

  const addBridge = () => {
    if (!newBridge.name || !newBridge.type) return;

    const bridge: BridgeConfig = {
      id: Math.random().toString(36).substr(2, 9),
      name: newBridge.name,
      type: newBridge.type as BridgeType,
      enabled: newBridge.enabled || false,
      config: newBridge.config || {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    setBridges(prev => [...prev, bridge]);
    setStatuses(prev => ({
      ...prev,
      [bridge.id]: {
        id: bridge.id,
        status: 'disconnected',
        messagesForwarded: 0,
        messagesFailed: 0
      }
    }));
    setShowAddModal(false);
    setNewBridge({ type: 'kafka', enabled: true, config: { ...defaultConfigs.kafka } });
  };

  const updateBridge = () => {
    if (!editingBridge) return;

    setBridges(prev => prev.map(b => b.id === editingBridge.id ? { ...editingBridge, updatedAt: new Date().toISOString() } : b));
    setEditingBridge(null);
  };

  const addRule = () => {
    if (!selectedBridge || !newRule.topic) return;

    const rule: BridgeRule = {
      id: Math.random().toString(36).substr(2, 9),
      bridgeId: selectedBridge.id,
      topic: newRule.topic,
      enabled: newRule.enabled,
      createdAt: new Date().toISOString(),
      kafkaTopic: newRule.kafkaTopic,
      targetTable: newRule.targetTable,
      targetMqttTopic: newRule.targetMqttTopic,
      rocketmqTopic: newRule.rocketmqTopic,
      redisKey: newRule.redisKey
    };

    setRules(prev => [...prev, rule]);
    setNewRule({ topic: '', enabled: true });
    setShowRuleModal(false);
  };

  const deleteRule = (ruleId: string) => {
    setRuleToDelete(ruleId);
    setShowRuleDeleteConfirm(true);
  };

  const confirmDeleteRule = () => {
    if (ruleToDelete) {
      setRules(prev => prev.filter(r => r.id !== ruleToDelete));
      setShowRuleDeleteConfirm(false);
      setRuleToDelete(null);
    }
  };

  const toggleRule = (ruleId: string) => {
    setRules(prev => prev.map(r => r.id === ruleId ? { ...r, enabled: !r.enabled } : r));
  };

  const toggleExpand = (bridgeId: string) => {
    const bridgeIndex = bridges.findIndex(b => b.id === bridgeId);
    const rowIndex = Math.floor(bridgeIndex / 2);
    const bridgeIdsInRow = bridges.slice(rowIndex * 2, rowIndex * 2 + 2).map(b => b.id);
    const isExpanding = !expandedBridges.includes(bridgeId);

    if (isExpanding) {
      setExpandedBridges(prev => [...new Set([...prev, ...bridgeIdsInRow])]);
    } else {
      setExpandedBridges(prev => prev.filter(id => !bridgeIdsInRow.includes(id)));
    }
  };

  const getBridgeRules = (bridgeId: string) => {
    return rules.filter(r => r.bridgeId === bridgeId);
  };

  const getBridgeTypeInfo = (type: BridgeType) => {
    return bridgeTypes.find(t => t.type === type) || bridgeTypes[0];
  };

  const getStatusBadge = (status: BridgeStatus) => {
    if (status.status === 'connected') {
      return <span className="inline-flex items-center gap-1.5 px-2 py-1 bg-emerald-500/20 text-emerald-400 rounded text-xs"><CheckCircle2 className="w-3 h-3" />已连接</span>;
    } else if (status.status === 'error') {
      return <span className="inline-flex items-center gap-1.5 px-2 py-1 bg-red-500/20 text-red-400 rounded text-xs"><AlertCircle className="w-3 h-3" />错误</span>;
    }
    return <span className="inline-flex items-center gap-1.5 px-2 py-1 bg-slate-600 text-slate-400 rounded text-xs"><PowerOff className="w-3 h-3" />未连接</span>;
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-3">
            <Link2 className="w-7 h-7 text-blue-400" />
            数据桥接
          </h1>
          <p className="text-slate-400 mt-1">将 Broker 内部消息转发到目标服务</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          添加桥接器
        </button>
      </div>

      <div className="grid grid-cols-2 gap-4">
        {bridges.map((bridge) => {
          const typeInfo = getBridgeTypeInfo(bridge.type);
          const status = statuses[bridge.id];
          const bridgeRules = getBridgeRules(bridge.id);
          const isExpanded = expandedBridges.includes(bridge.id);

          return (
            <motion.div
              key={bridge.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-slate-800/50 border border-slate-700/50 rounded-xl overflow-hidden"
            >
              <div className="p-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="p-3 bg-blue-500/20 rounded-lg">
                      <typeInfo.icon className="w-6 h-6 text-blue-400" />
                    </div>
                    <div>
                      <div className="flex items-center gap-3">
                        <h3 className="text-lg font-semibold text-white">{bridge.name}</h3>
                        <span className="px-2 py-0.5 bg-slate-700 text-slate-300 rounded text-xs">{typeInfo.label}</span>
                        {status && getStatusBadge(status)}
                      </div>
                      <p className="text-slate-400 text-sm mt-1">{typeInfo.description}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => toggleBridge(bridge.id)}
                      className={`p-2 rounded-lg transition-colors ${
                        bridge.enabled ? 'text-emerald-400 hover:bg-emerald-500/20' : 'text-slate-500 hover:bg-slate-700'
                      }`}
                      title={bridge.enabled ? '禁用' : '启用'}
                    >
                      <Power className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => setEditingBridge(bridge)}
                      className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                      title="编辑"
                    >
                      <Edit3 className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => deleteBridge(bridge.id)}
                      className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                      title="删除"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => toggleExpand(bridge.id)}
                      className="p-2 text-slate-400 hover:bg-slate-700 rounded-lg transition-colors"
                    >
                      {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                    </button>
                  </div>
                </div>

                {status && status.status === 'connected' && (
                  <div className="mt-4 grid grid-cols-3 gap-4">
                    <div className="bg-slate-900/50 rounded-lg p-3">
                      <p className="text-slate-400 text-xs">转发消息数</p>
                      <p className="text-emerald-400 text-lg font-semibold">{status.messagesForwarded.toLocaleString()}</p>
                    </div>
                    <div className="bg-slate-900/50 rounded-lg p-3">
                      <p className="text-slate-400 text-xs">失败消息数</p>
                      <p className="text-red-400 text-lg font-semibold">{status.messagesFailed.toLocaleString()}</p>
                    </div>
                    <div className="bg-slate-900/50 rounded-lg p-3">
                      <p className="text-slate-400 text-xs">规则数量</p>
                      <p className="text-blue-400 text-lg font-semibold">{bridgeRules.length}</p>
                    </div>
                  </div>
                )}

                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      exit={{ opacity: 0, height: 0 }}
                      className="mt-4 pt-4 border-t border-slate-700/50"
                    >
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="text-white font-medium flex items-center gap-2">
                          <Activity className="w-4 h-4 text-blue-400" />
                          转发规则
                        </h4>
                        <button
                          onClick={() => { setSelectedBridge(bridge); setShowRuleModal(true); }}
                          className="flex items-center gap-1 px-3 py-1.5 bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 rounded text-sm transition-colors"
                        >
                          <Plus className="w-3 h-3" />
                          添加规则
                        </button>
                      </div>

                      {bridgeRules.length > 0 ? (
                        <div className="space-y-2">
                          {bridgeRules.map((rule) => {
                            const bridge = bridges.find(b => b.id === rule.bridgeId);
                            let targetInfo = '';
                            if (bridge?.type === 'kafka' && rule.kafkaTopic) targetInfo = `→ Kafka: ${rule.kafkaTopic}`;
                            else if (bridge?.type === 'mysql' && rule.targetTable) targetInfo = `→ Table: ${rule.targetTable}`;
                            else if (bridge?.type === 'postgresql' && rule.targetTable) targetInfo = `→ Table: ${rule.targetTable}`;
                            else if (bridge?.type === 'mqtt' && rule.targetMqttTopic) targetInfo = `→ Topic: ${rule.targetMqttTopic}`;
                            else if (bridge?.type === 'rocketmq' && rule.rocketmqTopic) targetInfo = `→ Topic: ${rule.rocketmqTopic}`;
                            else if (bridge?.type === 'redis' && rule.redisKey) targetInfo = `→ Key: ${rule.redisKey}`;
                            else if (bridge?.type === 'websocket') targetInfo = '→ WebSocket 实时推送';

                            return (
                              <div
                                key={rule.id}
                                className="flex items-center justify-between p-3 bg-slate-900/50 rounded-lg"
                              >
                                <div className="flex items-center gap-3">
                                  <ArrowRight className="w-4 h-4 text-slate-500" />
                                  <div>
                                    <span className="text-slate-300 font-mono text-sm">{rule.topic}</span>
                                    {targetInfo && <span className="text-blue-400 text-sm ml-2">{targetInfo}</span>}
                                  </div>
                                  <span className={`px-2 py-0.5 rounded text-xs ${rule.enabled ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-600 text-slate-400'}`}>
                                    {rule.enabled ? '启用' : '禁用'}
                                  </span>
                                </div>
                                <div className="flex items-center gap-2">
                                  <button
                                    onClick={() => toggleRule(rule.id)}
                                    className={`p-1.5 rounded transition-colors ${rule.enabled ? 'text-emerald-400 hover:bg-emerald-500/20' : 'text-slate-500 hover:bg-slate-700'}`}
                                  >
                                    <Power className="w-4 h-4" />
                                  </button>
                                  <button
                                    onClick={() => deleteRule(rule.id)}
                                    className="p-1.5 text-red-400 hover:bg-red-500/20 rounded transition-colors"
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      ) : (
                        <p className="text-slate-500 text-sm">暂无转发规则</p>
                      )}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </motion.div>
          );
        })}

        {bridges.length === 0 && (
          <div className="text-center py-12">
            <Link2 className="w-12 h-12 mx-auto mb-3 text-slate-500" />
            <p className="text-slate-400">暂无桥接器</p>
            <p className="text-slate-500 text-sm mt-1">点击上方按钮添加第一个桥接器</p>
          </div>
        )}
      </div>

      <AnimatePresence>
        {showAddModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setShowAddModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-slate-800 rounded-xl border border-slate-700 max-w-2xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-slate-700 flex items-center justify-between">
                <h2 className="text-xl font-bold text-white">添加桥接器</h2>
                <button onClick={() => setShowAddModal(false)} className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-6">
                <div>
                  <label className="text-slate-200 font-medium mb-2 block">桥接器名称</label>
                  <input
                    type="text"
                    value={newBridge.name || ''}
                    onChange={(e) => setNewBridge(prev => ({ ...prev, name: e.target.value }))}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                    placeholder="输入桥接器名称"
                  />
                </div>

                <div>
                  <label className="text-slate-200 font-medium mb-2 block">桥接器类型</label>
                  <div className="grid grid-cols-3 gap-3">
                    {bridgeTypes.map((type) => (
                      <button
                        key={type.type}
                        onClick={() => setNewBridge(prev => ({ ...prev, type: type.type, config: { ...defaultConfigs[type.type] } }))}
                        className={`p-4 rounded-lg border transition-colors ${
                          newBridge.type === type.type
                            ? 'border-blue-500 bg-blue-500/20'
                            : 'border-slate-600 hover:border-slate-500'
                        }`}
                      >
                        <type.icon className={`w-6 h-6 mx-auto mb-2 ${newBridge.type === type.type ? 'text-blue-400' : 'text-slate-400'}`} />
                        <p className={`text-sm font-medium ${newBridge.type === type.type ? 'text-white' : 'text-slate-300'}`}>{type.label}</p>
                      </button>
                    ))}
                  </div>
                </div>

                {newBridge.type && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">连接配置</label>
                    <div className="space-y-3">
                      {Object.entries(defaultConfigs[newBridge.type as BridgeType]).map(([key, defaultValue]) => (
                        <div key={key}>
                          <label className="text-slate-400 text-sm mb-1 block">{key}</label>
                          <input
                            type={key.includes('password') ? 'password' : key.includes('port') || key.includes('database') || key.includes('retries') ? 'number' : 'text'}
                            value={newBridge.config?.[key] || ''}
                            onChange={(e) => setNewBridge(prev => ({
                              ...prev,
                              config: { ...prev.config, [key]: e.target.value }
                            }))}
                            className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                            placeholder={defaultValue}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex items-center gap-3">
                  <button
                    onClick={() => setNewBridge(prev => ({ ...prev, enabled: !prev.enabled }))}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      newBridge.enabled ? 'bg-blue-500' : 'bg-slate-600'
                    }`}
                  >
                    <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      newBridge.enabled ? 'translate-x-6' : 'translate-x-1'
                    }`} />
                  </button>
                  <span className="text-slate-200">立即启用</span>
                </div>
              </div>

              <div className="p-6 border-t border-slate-700 flex gap-3">
                <button
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={addBridge}
                  disabled={!newBridge.name}
                  className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-700 disabled:text-slate-500 text-white rounded-lg transition-colors"
                >
                  添加
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showRuleModal && selectedBridge && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setShowRuleModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-slate-800 rounded-xl border border-slate-700 max-w-md w-full"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-slate-700 flex items-center justify-between">
                <h2 className="text-xl font-bold text-white">添加转发规则</h2>
                <button onClick={() => setShowRuleModal(false)} className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-4">
                <div>
                  <label className="text-slate-200 font-medium mb-2 block">目标桥接器</label>
                  <p className="text-slate-300">{selectedBridge.name} ({bridgeTypes.find(t => t.type === selectedBridge.type)?.label})</p>
                </div>

                <div>
                  <label className="text-slate-200 font-medium mb-2 block">MQTT Topic 匹配规则</label>
                  <input
                    type="text"
                    value={newRule.topic}
                    onChange={(e) => setNewRule(prev => ({ ...prev, topic: e.target.value }))}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                    placeholder="例如: sensors/+/data"
                  />
                  <p className="text-slate-500 text-sm mt-1">支持通配符: + 匹配单层级, # 匹配多层级</p>
                </div>

                {selectedBridge.type === 'kafka' && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">Kafka Topic</label>
                    <input
                      type="text"
                      value={newRule.kafkaTopic || ''}
                      onChange={(e) => setNewRule(prev => ({ ...prev, kafkaTopic: e.target.value }))}
                      className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                      placeholder="例如: mqtt-sensors-data"
                    />
                  </div>
                )}

                {(selectedBridge.type === 'mysql' || selectedBridge.type === 'postgresql') && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">目标数据表</label>
                    <input
                      type="text"
                      value={newRule.targetTable || ''}
                      onChange={(e) => setNewRule(prev => ({ ...prev, targetTable: e.target.value }))}
                      className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                      placeholder="例如: sensor_data"
                    />
                  </div>
                )}

                {selectedBridge.type === 'mqtt' && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">目标 MQTT Topic</label>
                    <input
                      type="text"
                      value={newRule.targetMqttTopic || ''}
                      onChange={(e) => setNewRule(prev => ({ ...prev, targetMqttTopic: e.target.value }))}
                      className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                      placeholder="例如: forwarded/sensors/data"
                    />
                  </div>
                )}

                {selectedBridge.type === 'rocketmq' && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">RocketMQ Topic</label>
                    <input
                      type="text"
                      value={newRule.rocketmqTopic || ''}
                      onChange={(e) => setNewRule(prev => ({ ...prev, rocketmqTopic: e.target.value }))}
                      className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                      placeholder="例如: mqtt-messages"
                    />
                  </div>
                )}

                {selectedBridge.type === 'redis' && (
                  <div>
                    <label className="text-slate-200 font-medium mb-2 block">Redis Key</label>
                    <input
                      type="text"
                      value={newRule.redisKey || ''}
                      onChange={(e) => setNewRule(prev => ({ ...prev, redisKey: e.target.value }))}
                      className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                      placeholder="例如: mqtt:sensors:data"
                    />
                  </div>
                )}

                {selectedBridge.type === 'websocket' && (
                  <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
                    <p className="text-blue-300 text-sm">
                      WebSocket 桥接器会将匹配的 MQTT 消息实时推送到 WebSocket 连接。消息将以 JSON 格式发送，包含 topic、payload、timestamp 等字段。
                    </p>
                  </div>
                )}

                <div className="flex items-center gap-3">
                  <button
                    onClick={() => setNewRule(prev => ({ ...prev, enabled: !prev.enabled }))}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      newRule.enabled ? 'bg-blue-500' : 'bg-slate-600'
                    }`}
                  >
                    <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      newRule.enabled ? 'translate-x-6' : 'translate-x-1'
                    }`} />
                  </button>
                  <span className="text-slate-200">立即启用</span>
                </div>
              </div>

              <div className="p-6 border-t border-slate-700 flex gap-3">
                <button
                  onClick={() => setShowRuleModal(false)}
                  className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={addRule}
                  disabled={!newRule.topic}
                  className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-700 disabled:text-slate-500 text-white rounded-lg transition-colors"
                >
                  添加
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {editingBridge && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setEditingBridge(null)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-slate-800 rounded-xl border border-slate-700 max-w-2xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-slate-700 flex items-center justify-between">
                <h2 className="text-xl font-bold text-white">编辑桥接器</h2>
                <button onClick={() => setEditingBridge(null)} className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-6">
                <div>
                  <label className="text-slate-200 font-medium mb-2 block">桥接器名称</label>
                  <input
                    type="text"
                    value={editingBridge.name}
                    onChange={(e) => setEditingBridge(prev => prev ? { ...prev, name: e.target.value } : null)}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="text-slate-200 font-medium mb-2 block">连接配置</label>
                  <div className="space-y-3">
                    {Object.entries(editingBridge.config).map(([key, value]) => (
                      <div key={key}>
                        <label className="text-slate-400 text-sm mb-1 block">{key}</label>
                        <input
                          type={key.includes('password') ? 'password' : 'text'}
                          value={value}
                          onChange={(e) => setEditingBridge(prev => prev ? {
                            ...prev,
                            config: { ...prev.config, [key]: e.target.value }
                          } : null)}
                          className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                        />
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="p-6 border-t border-slate-700 flex gap-3">
                <button
                  onClick={() => setEditingBridge(null)}
                  className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={updateBridge}
                  className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
                >
                  保存
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showDeleteConfirm && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          >
            <motion.div
              initial={{ scale: 0.95 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.95 }}
              className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <AlertCircle className="w-6 h-6 text-red-400" />
                <h3 className="text-lg font-semibold text-white">确认删除</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要删除此桥接器吗？此操作不可恢复。</p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowDeleteConfirm(false); setBridgeToDelete(null); }}
                  className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={confirmDeleteBridge}
                  className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
                >
                  删除
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showRuleDeleteConfirm && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          >
            <motion.div
              initial={{ scale: 0.95 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.95 }}
              className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <AlertCircle className="w-6 h-6 text-red-400" />
                <h3 className="text-lg font-semibold text-white">确认删除</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要删除此转发规则吗？此操作不可恢复。</p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowRuleDeleteConfirm(false); setRuleToDelete(null); }}
                  className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={confirmDeleteRule}
                  className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
                >
                  删除
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
