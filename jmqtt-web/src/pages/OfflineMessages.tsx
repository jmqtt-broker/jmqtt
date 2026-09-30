import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Inbox, Trash2, X, Clock, User, MessageSquare, AlertCircle, Tag, Hash, Copy, CheckCircle2, FileText, Search } from 'lucide-react';
import type { OfflineMessage } from '../types/mqtt';
import { mockApi } from '../services/mockApi';

export default function OfflineMessages() {
  const [messages, setMessages] = useState<OfflineMessage[]>([]);
  const [selectedMessage, setSelectedMessage] = useState<OfflineMessage | null>(null);
  const [showDetailDrawer, setShowDetailDrawer] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [messageToDelete, setMessageToDelete] = useState<string | null>(null);
  const [clientToClear, setClientToClear] = useState<string | null>(null);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [clientIdFilter, setClientIdFilter] = useState('');
  const [topicFilter, setTopicFilter] = useState('');

  useEffect(() => {
    loadMessages();
  }, []);

  const loadMessages = async () => {
    setLoading(true);
    const data = await mockApi.getOfflineMessages();
    setMessages(data);
    setLoading(false);
  };

  const filteredMessages = messages.filter(msg => {
    const matchesClient = clientIdFilter === '' || msg.clientId.toLowerCase().includes(clientIdFilter.toLowerCase());
    const matchesTopic = topicFilter === '' || msg.topic.toLowerCase().includes(topicFilter.toLowerCase());
    return matchesClient && matchesTopic;
  });

  const handleDelete = async (id: string) => {
    setMessageToDelete(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (messageToDelete) {
      await mockApi.deleteOfflineMessage(messageToDelete);
      setMessages(prev => prev.filter(m => m.id !== messageToDelete));
      if (selectedMessage?.id === messageToDelete) {
        setSelectedMessage(null);
        setShowDetailDrawer(false);
      }
      setShowDeleteConfirm(false);
      setMessageToDelete(null);
    }
  };

  const handleClearClient = async (clientId: string) => {
    setClientToClear(clientId);
    setShowClearConfirm(true);
  };

  const confirmClearClient = async () => {
    if (clientToClear) {
      await mockApi.clearOfflineMessages(clientToClear);
      setMessages(prev => prev.filter(m => m.clientId !== clientToClear));
      setShowClearConfirm(false);
      setClientToClear(null);
    }
  };

  const handleMessageClick = (msg: OfflineMessage) => {
    setSelectedMessage(msg);
    setShowDetailDrawer(true);
  };

  const closeDetailDrawer = () => {
    setShowDetailDrawer(false);
    setTimeout(() => setSelectedMessage(null), 300);
  };

  const formatTime = (iso: string) => new Date(iso).toLocaleString('zh-CN');

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Inbox className="w-6 h-6 text-cyan-400" />
          <h1 className="text-2xl font-bold text-white">离线消息管理</h1>
        </div>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索客户端ID..."
            value={clientIdFilter}
            onChange={(e) => setClientIdFilter(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
        </div>
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索Topic..."
            value={topicFilter}
            onChange={(e) => setTopicFilter(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-400 mr-3" />
          加载中...
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-900">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">客户端</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">主题</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">QoS</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">时间</th>
                <th className="px-4 py-3 text-center text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {filteredMessages.map((msg) => (
                  <motion.tr
                    key={msg.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={() => handleMessageClick(msg)}
                    className="border-t border-slate-700 cursor-pointer transition-colors hover:bg-slate-700/50"
                  >
                    <td className="px-4 py-3 text-sm text-slate-300">{msg.clientId}</td>
                    <td className="px-4 py-3 text-sm text-slate-300 font-mono">{msg.topic}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        msg.qos === 2 ? 'bg-purple-500/20 text-purple-400' :
                        msg.qos === 1 ? 'bg-blue-500/20 text-blue-400' :
                        'bg-slate-500/20 text-slate-400'
                      }`}>
                        QoS {msg.qos}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-400">{formatTime(msg.timestamp)}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={(e) => { e.stopPropagation(); handleDelete(msg.id); }}
                        className="p-2 text-slate-400 hover:text-red-400 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </motion.tr>
                ))}
              </AnimatePresence>
            </tbody>
          </table>
          {filteredMessages.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <Inbox className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无离线消息</p>
            </div>
          )}
        </div>
      )}

      <AnimatePresence>
        {showDetailDrawer && selectedMessage && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={closeDetailDrawer}
              className="fixed inset-0 bg-black/50 z-40"
            />
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="fixed right-0 top-0 h-full w-[480px] bg-slate-800 border-l border-slate-700 z-50 overflow-y-auto"
            >
              <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-lg font-semibold text-white">消息详情</h3>
                  <button
                    onClick={closeDetailDrawer}
                    className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <User className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">客户端:</span>
                    <span className="text-white font-medium">{selectedMessage.clientId}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <MessageSquare className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">主题:</span>
                    <span className="text-cyan-400 font-mono">{selectedMessage.topic}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Tag className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">QoS:</span>
                    <span className="text-white">{selectedMessage.qos}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">时间:</span>
                    <span className="text-white">{formatTime(selectedMessage.timestamp)}</span>
                  </div>
                  {selectedMessage.expiryTime && (
                    <div className="flex items-center gap-3">
                      <AlertCircle className="w-4 h-4 text-amber-400" />
                      <span className="text-slate-400">过期时间:</span>
                      <span className="text-amber-400">{formatTime(selectedMessage.expiryTime)}</span>
                    </div>
                  )}
                  {selectedMessage.retain !== undefined && (
                    <div className="flex items-center gap-3">
                      <FileText className="w-4 h-4 text-slate-400" />
                      <span className="text-slate-400">保留标志:</span>
                      <span className="text-white">{selectedMessage.retain ? '是' : '否'}</span>
                    </div>
                  )}
                  {selectedMessage.payloadFormatIndicator !== undefined && (
                    <div className="flex items-center gap-3">
                      <FileText className="w-4 h-4 text-slate-400" />
                      <span className="text-slate-400">有效载荷指示器:</span>
                      <span className="text-white">{selectedMessage.payloadFormatIndicator ? '是' : '否'}</span>
                    </div>
                  )}
                  {selectedMessage.messageExpiryInterval !== undefined && selectedMessage.messageExpiryInterval > 0 && (
                    <div className="flex items-center gap-3">
                      <Clock className="w-4 h-4 text-amber-400" />
                      <span className="text-slate-400">消息过期时间:</span>
                      <span className="text-amber-400">{selectedMessage.messageExpiryInterval} 秒</span>
                    </div>
                  )}
                  {selectedMessage.contentType && (
                    <div className="flex items-center gap-3">
                      <Tag className="w-4 h-4 text-slate-400" />
                      <span className="text-slate-400">内容类型:</span>
                      <span className="text-white font-mono text-sm">{selectedMessage.contentType}</span>
                    </div>
                  )}
                  {selectedMessage.responseTopic && (
                    <div className="flex items-center gap-3">
                      <MessageSquare className="w-4 h-4 text-slate-400" />
                      <span className="text-slate-400">响应主题:</span>
                      <span className="text-cyan-400 font-mono text-sm">{selectedMessage.responseTopic}</span>
                    </div>
                  )}
                  {selectedMessage.correlationData && (
                    <div className="flex items-center gap-3">
                      <Hash className="w-4 h-4 text-slate-400" />
                      <span className="text-slate-400">关联数据:</span>
                      <div className="flex items-center gap-2">
                        <span className="text-white font-mono text-sm">{selectedMessage.correlationData}</span>
                        <button
                          onClick={() => copyToClipboard(selectedMessage.correlationData!, 'correlation')}
                          className="text-slate-500 hover:text-blue-400 transition-colors"
                        >
                          {copiedField === 'correlation' ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>
                  )}
                  {selectedMessage.userProperties && selectedMessage.userProperties.length > 0 && (
                    <div>
                      <span className="text-slate-400 text-sm mb-2 block">用户属性:</span>
                      <div className="bg-slate-900/50 rounded-lg p-3 space-y-2">
                        {selectedMessage.userProperties.map((prop, index) => (
                          <div key={index} className="flex items-center justify-between">
                            <span className="text-slate-400 text-sm">{prop.key}</span>
                            <span className="text-white text-sm font-mono">{prop.value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  <div>
                    <span className="text-slate-400 text-sm mb-2 block">消息内容:</span>
                    <pre className="bg-slate-900 rounded-lg p-4 text-sm text-slate-300 font-mono overflow-auto max-h-64">
                      {selectedMessage.payload}
                    </pre>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6"
          >
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="w-6 h-6 text-red-400" />
              <h3 className="text-lg font-semibold text-white">确认删除</h3>
            </div>
            <p className="text-slate-300 mb-6">确定要删除这条离线消息吗？此操作不可恢复。</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowDeleteConfirm(false); setMessageToDelete(null); }}
                className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
              >
                取消
              </button>
              <button
                onClick={confirmDelete}
                className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
              >
                删除
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {showClearConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6"
          >
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="w-6 h-6 text-red-400" />
              <h3 className="text-lg font-semibold text-white">确认清空</h3>
            </div>
            <p className="text-slate-300 mb-6">确定要清空客户端 <span className="text-cyan-400 font-mono">{clientToClear}</span> 的所有离线消息吗？此操作不可恢复。</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setShowClearConfirm(false); setClientToClear(null); }}
                className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
              >
                取消
              </button>
              <button
                onClick={confirmClearClient}
                className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
              >
                清空
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
