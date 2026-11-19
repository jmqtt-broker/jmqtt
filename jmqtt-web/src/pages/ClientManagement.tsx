import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Wifi, WifiOff, X, Power, Filter, Search, Activity, Eye, AlertCircle, Shield, ShieldOff, Clock, Infinity } from 'lucide-react';
import type { MQTTClient } from '../types/mqtt';
import { mockApi } from '../services/mockApi';
import { mqttVersions } from '../mock/mqttData';

export default function ClientManagement() {
  const navigate = useNavigate();
  const [clients, setClients] = useState<MQTTClient[]>([]);
  const [selectedClient, setSelectedClient] = useState<MQTTClient | null>(null);
  const [filterVersion, setFilterVersion] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [showDisconnectConfirm, setShowDisconnectConfirm] = useState(false);
  const [clientToDisconnect, setClientToDisconnect] = useState<string | null>(null);
  const [clientToDisconnectName, setClientToDisconnectName] = useState<string>('');
  const [showBlacklistModal, setShowBlacklistModal] = useState(false);
  const [clientToBlacklist, setClientToBlacklist] = useState<MQTTClient | null>(null);
  const [blacklistForm, setBlacklistForm] = useState({
    reason: '',
    isPermanent: false,
    duration: 1,
    durationUnit: 'hours' as 'minutes' | 'hours' | 'days'
  });

  useEffect(() => {
    loadClients();
  }, []);

  const loadClients = async () => {
    setLoading(true);
    const data = await mockApi.getClients();
    setClients(data);
    setLoading(false);
  };

  const handleDisconnect = (id: string, clientId: string) => {
    setClientToDisconnect(id);
    setClientToDisconnectName(clientId);
    setShowDisconnectConfirm(true);
  };

  const confirmDisconnect = async () => {
    if (clientToDisconnect) {
      await mockApi.disconnectClient(clientToDisconnect);
      setClients(prev => prev.filter(c => c.id !== clientToDisconnect));
      setSelectedClient(null);
      setShowDisconnectConfirm(false);
      setClientToDisconnect(null);
      setClientToDisconnectName('');
    }
  };

  const handleAddToBlacklist = (client: MQTTClient) => {
    setClientToBlacklist(client);
    setShowBlacklistModal(true);
  };

  const confirmAddToBlacklist = async () => {
    if (clientToBlacklist) {
      let durationInSeconds = 0;
      if (!blacklistForm.isPermanent) {
        const multipliers = { minutes: 60, hours: 3600, days: 86400 };
        durationInSeconds = blacklistForm.duration * multipliers[blacklistForm.durationUnit];
      }

      await mockApi.addToBlacklist({
        type: 'clientId',
        value: clientToBlacklist.clientId,
        reason: blacklistForm.reason,
        isPermanent: blacklistForm.isPermanent,
        duration: durationInSeconds
      });

      setClients(prev => prev.filter(c => c.id !== clientToBlacklist.id));
      setSelectedClient(null);
      setShowBlacklistModal(false);
      setClientToBlacklist(null);
      setBlacklistForm({
        reason: '',
        isPermanent: false,
        duration: 1,
        durationUnit: 'hours'
      });
    }
  };

  const filteredClients = clients.filter(client => {
    const matchesVersion = filterVersion === 'all' || client.protocolVersion === filterVersion;
    const matchesSearch = client.clientId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (client.username?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false);
    return matchesVersion && matchesSearch;
  });

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${hours}h ${mins}m`;
  };

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">客户端管理</h1>
          <p className="text-slate-400 mt-1">管理MQTT Broker在线客户端连接</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-emerald-400">
            <Wifi className="w-5 h-5" />
            <span className="font-medium">{clients.length} 在线</span>
          </div>
        </div>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索客户端ID或用户名..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <select
            value={filterVersion}
            onChange={(e) => setFilterVersion(e.target.value)}
            className="pl-10 pr-8 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500 appearance-none cursor-pointer"
          >
            <option value="all">所有版本</option>
            {mqttVersions.map(v => (
              <option key={v} value={v}>MQTT {v}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-900/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">客户端ID</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">用户名</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">协议版本</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">连接时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">Keepalive</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">消息(收/发)</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {filteredClients.map((client) => (
                <motion.tr
                  key={client.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="hover:bg-slate-700/50 cursor-pointer"
                  onClick={() => setSelectedClient(client)}
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      {client.isBridge ? (
                        <Activity className="w-4 h-4 text-amber-400" />
                      ) : (
                        <Wifi className="w-4 h-4 text-emerald-400" />
                      )}
                      <span className="text-white font-medium">{client.clientId}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-300">{client.username || '-'}</td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs">
                      {client.protocolVersion}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-400 text-sm">
                    {new Date(client.connectedAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-slate-400 text-sm">{client.keepalive}s</td>
                  <td className="px-4 py-3 text-slate-300 text-sm">
                    {client.messagesReceived.toLocaleString()} / {client.messagesSent.toLocaleString()}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/monitor/clients/${client.id}`);
                        }}
                        className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                        title="查看详情"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDisconnect(client.id, client.clientId);
                        }}
                        className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                        title="断开连接"
                      >
                        <Power className="w-4 h-4" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAddToBlacklist(client);
                        }}
                        className="p-2 text-amber-400 hover:bg-amber-500/20 rounded-lg transition-colors"
                        title="加入黑名单"
                      >
                        <Shield className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
          {filteredClients.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <WifiOff className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无匹配的客户端</p>
            </div>
          )}
        </div>
      )}

      <AnimatePresence>
        {selectedClient && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedClient(null)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-slate-800 rounded-xl border border-slate-700 max-w-2xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-slate-700 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-emerald-500/20 rounded-lg flex items-center justify-center">
                    <Wifi className="w-5 h-5 text-emerald-400" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">{selectedClient.clientId}</h2>
                    <p className="text-slate-400 text-sm">{selectedClient.peername}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedClient(null)}
                  className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">协议版本</p>
                    <p className="text-white font-medium">MQTT {selectedClient.protocolVersion}</p>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">Clean Session</p>
                    <p className="text-white font-medium">{selectedClient.cleanSession ? '是' : '否'}</p>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">Keepalive</p>
                    <p className="text-white font-medium">{selectedClient.keepalive} 秒</p>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">桥接模式</p>
                    <p className="text-white font-medium">{selectedClient.isBridge ? '是' : '否'}</p>
                  </div>
                </div>

                <div className="bg-slate-900/50 rounded-lg p-4">
                  <h3 className="text-white font-medium mb-3">流量统计</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-slate-400 text-sm">接收消息</p>
                      <p className="text-emerald-400 font-medium">{selectedClient.messagesReceived.toLocaleString()}</p>
                      <p className="text-slate-500 text-xs">{formatBytes(selectedClient.bytesReceived)}</p>
                    </div>
                    <div>
                      <p className="text-slate-400 text-sm">发送消息</p>
                      <p className="text-blue-400 font-medium">{selectedClient.messagesSent.toLocaleString()}</p>
                      <p className="text-slate-500 text-xs">{formatBytes(selectedClient.bytesSent)}</p>
                    </div>
                  </div>
                </div>

                <div className="bg-slate-900/50 rounded-lg p-4">
                  <h3 className="text-white font-medium mb-3">订阅主题</h3>
                  {selectedClient.subscriptions.length > 0 ? (
                    <div className="space-y-2">
                      {selectedClient.subscriptions.map((sub) => (
                        <div key={sub.id} className="flex items-center justify-between py-2 border-b border-slate-700 last:border-0">
                          <span className="text-slate-300 font-mono text-sm">{sub.topic}</span>
                          <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs">QoS {sub.qos}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-slate-500">暂无订阅</p>
                  )}
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => handleDisconnect(selectedClient.id, selectedClient.clientId)}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-red-500/20 text-red-400 hover:bg-red-500/30 rounded-lg transition-colors"
                  >
                    <Power className="w-4 h-4" />
                    断开连接
                  </button>
                  <button
                    onClick={() => handleAddToBlacklist(selectedClient)}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-amber-500/20 text-amber-400 hover:bg-amber-500/30 rounded-lg transition-colors"
                  >
                    <Shield className="w-4 h-4" />
                    加入黑名单
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showDisconnectConfirm && (
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
                <h3 className="text-lg font-semibold text-white">确认断开连接</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要断开客户端 <span className="text-cyan-400 font-mono">{clientToDisconnectName}</span> 的连接吗？</p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowDisconnectConfirm(false); setClientToDisconnect(null); setClientToDisconnectName(''); }}
                  className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={confirmDisconnect}
                  className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
                >
                  断开连接
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showBlacklistModal && clientToBlacklist && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
              onClick={() => setShowBlacklistModal(false)}
            />
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="w-full max-w-lg pointer-events-auto"
                onClick={(e) => e.stopPropagation()}
              >
              <div className="bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl overflow-hidden">
                <div className="p-6 border-b border-slate-700 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-amber-500/20 flex items-center justify-center">
                      <Shield className="w-5 h-5 text-amber-400" />
                    </div>
                    <h3 className="text-lg font-semibold text-white">加入黑名单</h3>
                  </div>
                  <button
                    onClick={() => setShowBlacklistModal(false)}
                    className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="p-6 space-y-4">
                  <div className="bg-amber-500/10 border border-amber-500/30 rounded-lg p-4">
                    <div className="flex items-start gap-3">
                      <AlertCircle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-amber-300 text-sm font-medium mb-1">警告</p>
                        <p className="text-amber-200/80 text-sm">
                          加入黑名单后会立即踢出客户端连接，确认操作？
                        </p>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">客户端ID</label>
                    <div className="px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono">
                      {clientToBlacklist.clientId}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">原因（可选）</label>
                    <input
                      type="text"
                      value={blacklistForm.reason}
                      onChange={(e) => setBlacklistForm({ ...blacklistForm, reason: e.target.value })}
                      placeholder="请输入拉黑原因"
                      className="w-full px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">有效期</label>
                    <div className="flex gap-3 mb-3">
                      <button
                        onClick={() => setBlacklistForm({ ...blacklistForm, isPermanent: false })}
                        className={`flex-1 px-4 py-2 rounded-lg border transition-all ${
                          !blacklistForm.isPermanent
                            ? 'bg-blue-500/20 border-blue-500/50 text-blue-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        限时禁用
                      </button>
                      <button
                        onClick={() => setBlacklistForm({ ...blacklistForm, isPermanent: true })}
                        className={`flex-1 px-4 py-2 rounded-lg border transition-all ${
                          blacklistForm.isPermanent
                            ? 'bg-red-500/20 border-red-500/50 text-red-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        永久禁用
                      </button>
                    </div>

                    {!blacklistForm.isPermanent && (
                      <div className="flex gap-2">
                        <input
                          type="number"
                          min={1}
                          value={blacklistForm.duration}
                          onChange={(e) => setBlacklistForm({ ...blacklistForm, duration: parseInt(e.target.value) || 1 })}
                          className="flex-1 px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                        />
                        <select
                          value={blacklistForm.durationUnit}
                          onChange={(e) => setBlacklistForm({ ...blacklistForm, durationUnit: e.target.value as 'minutes' | 'hours' | 'days' })}
                          className="px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                        >
                          <option value="minutes">分钟</option>
                          <option value="hours">小时</option>
                          <option value="days">天</option>
                        </select>
                      </div>
                    )}
                  </div>
                </div>

                <div className="px-6 py-4 bg-slate-800/50 border-t border-slate-700 flex justify-end gap-3">
                  <button
                    onClick={() => setShowBlacklistModal(false)}
                    className="px-4 py-2 text-slate-400 hover:text-white transition-colors text-sm font-medium"
                  >
                    取消
                  </button>
                  <button
                    onClick={confirmAddToBlacklist}
                    className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors text-sm font-medium flex items-center gap-2"
                  >
                    <Shield className="w-4 h-4" />
                    确认加入
                  </button>
                </div>
              </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
