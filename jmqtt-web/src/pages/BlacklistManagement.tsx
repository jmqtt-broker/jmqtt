import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield,
  ShieldOff,
  Search,
  Plus,
  Trash2,
  AlertCircle,
  X,
  Clock,
  Infinity,
  UserX,
  Globe,
  Filter
} from 'lucide-react';
import type { BlacklistItem, BlacklistType } from '../types/mqtt';
import { mockApi } from '../services/mockApi';

export default function BlacklistManagement() {
  const [blacklist, setBlacklist] = useState<BlacklistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<BlacklistType | 'all'>('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState(false);
  const [itemToRemove, setItemToRemove] = useState<BlacklistItem | null>(null);

  const [newItem, setNewItem] = useState<{
    type: BlacklistType;
    value: string;
    reason: string;
    isPermanent: boolean;
    duration: number;
    durationUnit: 'minutes' | 'hours' | 'days';
  }>({
    type: 'clientId',
    value: '',
    reason: '',
    isPermanent: false,
    duration: 1,
    durationUnit: 'hours'
  });

  useEffect(() => {
    loadBlacklist();
  }, []);

  const loadBlacklist = async () => {
    setLoading(true);
    const data = await mockApi.getBlacklist();
    setBlacklist(data);
    setLoading(false);
  };

  const filteredBlacklist = blacklist.filter(item => {
    const matchesSearch = item.value.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (item.reason?.toLowerCase().includes(searchTerm.toLowerCase()) ?? false);
    const matchesType = filterType === 'all' || item.type === filterType;
    return matchesSearch && matchesType;
  });

  const [ipError, setIpError] = useState<string>('');

  const validateIpAddress = (ip: string): boolean => {
    const ipRegex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipRegex.test(ip);
  };

  const handleAddToBlacklist = async () => {
    if (!newItem.value.trim()) return;

    if (newItem.type === 'ip' && !validateIpAddress(newItem.value.trim())) {
      setIpError('请输入有效的IP地址格式（如：192.168.1.1）');
      return;
    }

    let durationInSeconds = 0;
    if (!newItem.isPermanent) {
      const multipliers = { minutes: 60, hours: 3600, days: 86400 };
      durationInSeconds = newItem.duration * multipliers[newItem.durationUnit];
    }

    await mockApi.addToBlacklist({
      type: newItem.type,
      value: newItem.value.trim(),
      reason: newItem.reason.trim() || undefined,
      isPermanent: newItem.isPermanent,
      duration: durationInSeconds
    });

    setShowAddModal(false);
    setIpError('');
    setNewItem({
      type: 'clientId',
      value: '',
      reason: '',
      isPermanent: false,
      duration: 1,
      durationUnit: 'hours'
    });
    loadBlacklist();
  };

  const handleRemoveFromBlacklist = async () => {
    if (itemToRemove) {
      await mockApi.removeFromBlacklist(itemToRemove.id);
      setShowRemoveConfirm(false);
      setItemToRemove(null);
      loadBlacklist();
    }
  };

  const confirmRemove = (item: BlacklistItem) => {
    setItemToRemove(item);
    setShowRemoveConfirm(true);
  };

  const formatDuration = (item: BlacklistItem) => {
    if (item.isPermanent) return '永久';
    if (!item.expiresAt) return '未知';

    const now = new Date();
    const expires = new Date(item.expiresAt);
    const diff = expires.getTime() - now.getTime();

    if (diff <= 0) return '已过期';

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

    if (days > 0) return `${days}天 ${hours}小时`;
    if (hours > 0) return `${hours}小时`;
    return '即将过期';
  };

  const getDurationLabel = () => {
    const labels = { minutes: '分钟', hours: '小时', days: '天' };
    return labels[newItem.durationUnit];
  };

  return (
    <div className="p-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">黑名单管理</h1>
          <p className="text-slate-400 mt-1">管理被禁用的客户端ID和IP地址</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          添加黑名单
        </button>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索客户端ID、IP地址或原因..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as BlacklistType | 'all')}
            className="pl-10 pr-8 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500 appearance-none cursor-pointer"
          >
            <option value="all">全部类型</option>
            <option value="clientId">客户端ID</option>
            <option value="ip">IP地址</option>
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
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">类型</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">值</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">原因</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">有效期</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">添加时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {filteredBlacklist.map((item) => (
                <motion.tr
                  key={item.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="hover:bg-slate-700/50"
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      {item.type === 'clientId' ? (
                        <UserX className="w-4 h-4 text-amber-400" />
                      ) : (
                        <Globe className="w-4 h-4 text-purple-400" />
                      )}
                      <span className="text-slate-300 text-sm">
                        {item.type === 'clientId' ? '客户端ID' : 'IP地址'}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-white font-mono text-sm">{item.value}</span>
                  </td>
                  <td className="px-4 py-3 text-slate-300 text-sm">
                    {item.reason || '-'}
                  </td>
                  <td className="px-4 py-3">
                    {item.isPermanent ? (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-500/20 text-red-400 rounded text-xs">
                        <Infinity className="w-3 h-3" />
                        永久
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-amber-500/20 text-amber-400 rounded text-xs">
                        <Clock className="w-3 h-3" />
                        {formatDuration(item)}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-slate-400 text-sm">
                    {new Date(item.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => confirmRemove(item)}
                      className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                      title="移除黑名单"
                    >
                      <ShieldOff className="w-4 h-4" />
                    </button>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
          {filteredBlacklist.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <Shield className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无黑名单记录</p>
            </div>
          )}
        </div>
      )}

      <AnimatePresence>
        {showAddModal && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
              onClick={() => setShowAddModal(false)}
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
                    <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center">
                      <Shield className="w-5 h-5 text-red-400" />
                    </div>
                    <h3 className="text-lg font-semibold text-white">添加黑名单</h3>
                  </div>
                  <button
                    onClick={() => setShowAddModal(false)}
                    className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="p-6 space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">类型</label>
                    <div className="flex gap-3">
                      <button
                        onClick={() => setNewItem({ ...newItem, type: 'clientId' })}
                        className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg border transition-all ${
                          newItem.type === 'clientId'
                            ? 'bg-amber-500/20 border-amber-500/50 text-amber-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        <UserX className="w-4 h-4" />
                        客户端ID
                      </button>
                      <button
                        onClick={() => setNewItem({ ...newItem, type: 'ip' })}
                        className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg border transition-all ${
                          newItem.type === 'ip'
                            ? 'bg-purple-500/20 border-purple-500/50 text-purple-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        <Globe className="w-4 h-4" />
                        IP地址
                      </button>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      {newItem.type === 'clientId' ? '客户端ID' : 'IP地址'}
                    </label>
                    <input
                      type="text"
                      value={newItem.value}
                      onChange={(e) => {
                        setNewItem({ ...newItem, value: e.target.value });
                        if (newItem.type === 'ip') setIpError('');
                      }}
                      placeholder={newItem.type === 'clientId' ? '请输入客户端ID' : '请输入IP地址'}
                      className={`w-full px-4 py-2 bg-slate-900 border rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 ${
                        ipError && newItem.type === 'ip' ? 'border-red-500' : 'border-slate-700'
                      }`}
                    />
                    {newItem.type === 'ip' && ipError && (
                      <p className="mt-2 text-red-400 text-sm flex items-center gap-1">
                        <AlertCircle className="w-4 h-4" />
                        {ipError}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">原因（可选）</label>
                    <input
                      type="text"
                      value={newItem.reason}
                      onChange={(e) => setNewItem({ ...newItem, reason: e.target.value })}
                      placeholder="请输入拉黑原因"
                      className="w-full px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">有效期</label>
                    <div className="flex gap-3 mb-3">
                      <button
                        onClick={() => setNewItem({ ...newItem, isPermanent: false })}
                        className={`flex-1 px-4 py-2 rounded-lg border transition-all ${
                          !newItem.isPermanent
                            ? 'bg-blue-500/20 border-blue-500/50 text-blue-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        限时禁用
                      </button>
                      <button
                        onClick={() => setNewItem({ ...newItem, isPermanent: true })}
                        className={`flex-1 px-4 py-2 rounded-lg border transition-all ${
                          newItem.isPermanent
                            ? 'bg-red-500/20 border-red-500/50 text-red-400'
                            : 'bg-slate-700/50 border-slate-600 text-slate-400 hover:bg-slate-700'
                        }`}
                      >
                        永久禁用
                      </button>
                    </div>

                    {!newItem.isPermanent && (
                      <div className="flex gap-2">
                        <input
                          type="number"
                          min={1}
                          value={newItem.duration}
                          onChange={(e) => setNewItem({ ...newItem, duration: parseInt(e.target.value) || 1 })}
                          className="flex-1 px-4 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white focus:outline-none focus:border-blue-500"
                        />
                        <select
                          value={newItem.durationUnit}
                          onChange={(e) => setNewItem({ ...newItem, durationUnit: e.target.value as 'minutes' | 'hours' | 'days' })}
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
                    onClick={() => setShowAddModal(false)}
                    className="px-4 py-2 text-slate-400 hover:text-white transition-colors text-sm font-medium"
                  >
                    取消
                  </button>
                  <button
                    onClick={handleAddToBlacklist}
                    disabled={!newItem.value.trim()}
                    className="px-4 py-2 bg-red-500 hover:bg-red-600 disabled:bg-slate-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors text-sm font-medium flex items-center gap-2"
                  >
                    <Shield className="w-4 h-4" />
                    确认添加
                  </button>
                </div>
              </div>
              </motion.div>
            </div>
          </>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showRemoveConfirm && itemToRemove && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
              onClick={() => setShowRemoveConfirm(false)}
            />
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="w-full max-w-md pointer-events-auto"
                onClick={(e) => e.stopPropagation()}
              >
              <div className="bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl overflow-hidden">
                <div className="p-6">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0">
                      <ShieldOff className="w-6 h-6 text-emerald-400" />
                    </div>
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-white mb-2">确认移除黑名单</h3>
                      <p className="text-slate-400 text-sm">
                        确定要将 <span className="text-white font-medium">{itemToRemove.type === 'clientId' ? '客户端ID' : 'IP地址'}</span>
                        <span className="text-white font-mono"> {itemToRemove.value} </span>
                        从黑名单中移除吗？移除后该客户端/IP将可以重新连接。
                      </p>
                    </div>
                  </div>
                </div>
                <div className="px-6 py-4 bg-slate-800/50 border-t border-slate-700 flex justify-end gap-3">
                  <button
                    onClick={() => setShowRemoveConfirm(false)}
                    className="px-4 py-2 text-slate-400 hover:text-white transition-colors text-sm font-medium"
                  >
                    取消
                  </button>
                  <button
                    onClick={handleRemoveFromBlacklist}
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition-colors text-sm font-medium flex items-center gap-2"
                  >
                    <ShieldOff className="w-4 h-4" />
                    确认移除
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
