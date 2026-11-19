import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Server,
  Cpu,
  HardDrive,
  Activity,
  Wifi,
  WifiOff,
  Clock,
  X,
  Filter,
  Search,
  Shield,
  Zap
} from 'lucide-react';
import type { ClusterNode } from '../types/mqtt';
import { mockApi } from '../services/mockApi';

const statusConfig = {
  online: { color: 'bg-emerald-500', text: 'text-emerald-400', bg: 'bg-emerald-500/20', icon: Wifi, label: '在线' },
  offline: { color: 'bg-red-500', text: 'text-red-400', bg: 'bg-red-500/20', icon: WifiOff, label: '离线' },
  joining: { color: 'bg-amber-500', text: 'text-amber-400', bg: 'bg-amber-500/20', icon: Activity, label: '加入中' },
  leaving: { color: 'bg-orange-500', text: 'text-orange-400', bg: 'bg-orange-500/20', icon: Activity, label: '离开中' }
};

const roleConfig = {
  keeper: { icon: Shield, color: 'text-violet-400', bg: 'bg-violet-500/20', label: 'Keeper' },
  worker: { icon: Zap, color: 'text-cyan-400', bg: 'bg-cyan-500/20', label: 'Worker' }
};

export default function ClusterNodes() {
  const [nodes, setNodes] = useState<ClusterNode[]>([]);
  const [selectedNode, setSelectedNode] = useState<ClusterNode | null>(null);
  const [filterRole, setFilterRole] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadNodes();
  }, []);

  const loadNodes = async () => {
    setLoading(true);
    const data = await mockApi.getClusterNodes();
    setNodes(data);
    setLoading(false);
  };

  const filteredNodes = nodes.filter(node => {
    const matchesRole = filterRole === 'all' || node.role === filterRole;
    const matchesStatus = filterStatus === 'all' || node.status === filterStatus;
    const matchesSearch = node.nodeId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         node.ip.includes(searchTerm);
    return matchesRole && matchesStatus && matchesSearch;
  });

  const onlineCount = nodes.filter(n => n.status === 'online').length;
  const keeperCount = nodes.filter(n => n.role === 'keeper').length;
  const workerCount = nodes.filter(n => n.role === 'worker').length;
  const totalConnections = nodes.reduce((sum, n) => sum + n.connections, 0);

  const formatTime = (iso: string) => new Date(iso).toLocaleString('zh-CN');

  return (
    <div className="p-6">
      <div className="mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-3">
              <Server className="w-7 h-7 text-violet-400" />
              集群节点管理
            </h1>
            <p className="text-slate-400 mt-1">管理 MQTT Broker 集群节点</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 text-emerald-400">
              <Wifi className="w-5 h-5" />
              <span className="font-medium">{onlineCount}/{nodes.length} 在线</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-4"
        >
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-violet-500/20 rounded-lg">
              <Shield className="w-5 h-5 text-violet-400" />
            </div>
            <span className="text-slate-400 text-sm">Keeper 节点</span>
          </div>
          <p className="text-2xl font-bold text-white">{keeperCount}</p>
        </motion.div>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-4"
        >
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-cyan-500/20 rounded-lg">
              <Zap className="w-5 h-5 text-cyan-400" />
            </div>
            <span className="text-slate-400 text-sm">Worker 节点</span>
          </div>
          <p className="text-2xl font-bold text-white">{workerCount}</p>
        </motion.div>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-4"
        >
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-emerald-500/20 rounded-lg">
              <Wifi className="w-5 h-5 text-emerald-400" />
            </div>
            <span className="text-slate-400 text-sm">在线节点</span>
          </div>
          <p className="text-2xl font-bold text-white">{onlineCount}</p>
        </motion.div>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-4"
        >
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-blue-500/20 rounded-lg">
              <Activity className="w-5 h-5 text-blue-400" />
            </div>
            <span className="text-slate-400 text-sm">总连接数</span>
          </div>
          <p className="text-2xl font-bold text-white">{totalConnections.toLocaleString()}</p>
        </motion.div>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索节点ID或IP..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-violet-500"
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <select
            value={filterRole}
            onChange={(e) => setFilterRole(e.target.value)}
            className="pl-10 pr-8 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-violet-500 appearance-none cursor-pointer"
          >
            <option value="all">所有角色</option>
            <option value="keeper">Keeper</option>
            <option value="worker">Worker</option>
          </select>
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="pl-10 pr-8 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-violet-500 appearance-none cursor-pointer"
          >
            <option value="all">所有状态</option>
            <option value="online">在线</option>
            <option value="offline">离线</option>
            <option value="joining">加入中</option>
            <option value="leaving">离开中</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-violet-500"></div>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-900/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">节点ID</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">IP:端口</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">角色</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">CPU/内存</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">连接数</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">加入时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {filteredNodes.map((node) => {
                const status = statusConfig[node.status];
                const role = roleConfig[node.role];
                const StatusIcon = status.icon;
                const RoleIcon = role.icon;
                return (
                  <motion.tr
                    key={node.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="hover:bg-slate-700/50 cursor-pointer"
                    onClick={() => setSelectedNode(node)}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <RoleIcon className={`w-4 h-4 ${role.color}`} />
                        <span className="text-white font-medium">{node.nodeId}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-300 font-mono text-sm">{node.ip}:{node.port}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-xs ${role.bg} ${role.color}`}>
                        {role.label}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <StatusIcon className={`w-4 h-4 ${status.text}`} />
                        <span className={`text-sm ${status.text}`}>{status.label}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {node.status === 'online' ? (
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <Cpu className="w-3 h-3 text-slate-400" />
                            <div className="w-16 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full ${node.cpuUsage > 80 ? 'bg-red-500' : node.cpuUsage > 60 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                                style={{ width: `${node.cpuUsage}%` }}
                              />
                            </div>
                            <span className="text-xs text-slate-400">{node.cpuUsage.toFixed(1)}%</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <HardDrive className="w-3 h-3 text-slate-400" />
                            <div className="w-16 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full ${node.memoryUsage > 80 ? 'bg-red-500' : node.memoryUsage > 60 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                                style={{ width: `${node.memoryUsage}%` }}
                              />
                            </div>
                            <span className="text-xs text-slate-400">{node.memoryUsage.toFixed(1)}%</span>
                          </div>
                        </div>
                      ) : (
                        <span className="text-slate-500 text-sm">-</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-300">
                      {node.status === 'online' ? node.connections.toLocaleString() : '-'}
                    </td>
                    <td className="px-4 py-3 text-slate-400 text-sm">{formatTime(node.joinedAt)}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedNode(node);
                        }}
                        className="text-violet-400 hover:text-violet-300 text-sm"
                      >
                        详情
                      </button>
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
          {filteredNodes.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <Server className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无匹配的节点</p>
            </div>
          )}
        </div>
      )}

      <AnimatePresence>
        {selectedNode && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedNode(null)}
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
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${roleConfig[selectedNode.role].bg}`}>
                    {React.createElement(roleConfig[selectedNode.role].icon, { className: `w-5 h-5 ${roleConfig[selectedNode.role].color}` })}
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">{selectedNode.nodeId}</h2>
                    <p className="text-slate-400 text-sm">{selectedNode.ip}:{selectedNode.port}</p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedNode(null)}
                  className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">角色</p>
                    <div className="flex items-center gap-2">
                      {React.createElement(roleConfig[selectedNode.role].icon, { className: `w-4 h-4 ${roleConfig[selectedNode.role].color}` })}
                      <span className="text-white font-medium">{roleConfig[selectedNode.role].label}</span>
                    </div>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">状态</p>
                    <div className="flex items-center gap-2">
                      {React.createElement(statusConfig[selectedNode.status].icon, { className: `w-4 h-4 ${statusConfig[selectedNode.status].text}` })}
                      <span className={`font-medium ${statusConfig[selectedNode.status].text}`}>{statusConfig[selectedNode.status].label}</span>
                    </div>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">加入时间</p>
                    <div className="flex items-center gap-2">
                      <Clock className="w-4 h-4 text-slate-400" />
                      <span className="text-white font-medium">{formatTime(selectedNode.joinedAt)}</span>
                    </div>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-4">
                    <p className="text-slate-400 text-sm mb-1">最后心跳</p>
                    <div className="flex items-center gap-2">
                      <Activity className="w-4 h-4 text-slate-400" />
                      <span className="text-white font-medium">{formatTime(selectedNode.lastHeartbeat)}</span>
                    </div>
                  </div>
                </div>

                {selectedNode.status === 'online' && (
                  <>
                    <div className="bg-slate-900/50 rounded-lg p-4">
                      <h3 className="text-white font-medium mb-4 flex items-center gap-2">
                        <Cpu className="w-4 h-4 text-violet-400" />
                        资源使用
                      </h3>
                      <div className="space-y-4">
                        <div>
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-slate-400 text-sm">CPU 使用率</span>
                            <span className="text-white font-medium">{selectedNode.cpuUsage.toFixed(1)}%</span>
                          </div>
                          <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${selectedNode.cpuUsage}%` }}
                              className={`h-full rounded-full ${selectedNode.cpuUsage > 80 ? 'bg-red-500' : selectedNode.cpuUsage > 60 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                            />
                          </div>
                        </div>
                        <div>
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-slate-400 text-sm">内存使用率</span>
                            <span className="text-white font-medium">{selectedNode.memoryUsage.toFixed(1)}%</span>
                          </div>
                          <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${selectedNode.memoryUsage}%` }}
                              className={`h-full rounded-full ${selectedNode.memoryUsage > 80 ? 'bg-red-500' : selectedNode.memoryUsage > 60 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                            />
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="bg-slate-900/50 rounded-lg p-4">
                      <h3 className="text-white font-medium mb-4 flex items-center gap-2">
                        <Activity className="w-4 h-4 text-cyan-400" />
                        连接统计
                      </h3>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p className="text-slate-400 text-sm">活跃连接</p>
                          <p className="text-2xl font-bold text-white">{selectedNode.connections.toLocaleString()}</p>
                        </div>
                        <div>
                          <p className="text-slate-400 text-sm">消息速率</p>
                          <p className="text-2xl font-bold text-white">{selectedNode.messagesPerSecond.toFixed(1)} <span className="text-sm text-slate-400">msg/s</span></p>
                        </div>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
