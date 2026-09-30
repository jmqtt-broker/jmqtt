import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Clock,
  Trash2,
  X,
  User,
  MessageSquare,
  Tag,
  Hash,
  Copy,
  CheckCircle2,
  FileText,
  ChevronLeft,
  ChevronRight,
  Search
} from 'lucide-react';
import type { DelayedMessage } from '../types/mqtt';
import { mockApi } from '../services/mockApi';

const PAGE_SIZE = 10;

export default function DelayedMessages() {
  const [messages, setMessages] = useState<DelayedMessage[]>([]);
  const [selectedMessage, setSelectedMessage] = useState<DelayedMessage | null>(null);
  const [showDetailDrawer, setShowDetailDrawer] = useState(false);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [clientIdFilter, setClientIdFilter] = useState('');
  const [topicFilter, setTopicFilter] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [messageToDelete, setMessageToDelete] = useState<string | null>(null);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [currentTime, setCurrentTime] = useState(Date.now());

  useEffect(() => {
    loadMessages();
    const timer = setInterval(() => setCurrentTime(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const loadMessages = async () => {
    setLoading(true);
    const data = await mockApi.getDelayedMessages();
    setMessages(data);
    setLoading(false);
  };

  const filteredMessages = messages.filter(msg => {
    const matchesClient = clientIdFilter === '' || msg.clientId.toLowerCase().includes(clientIdFilter.toLowerCase());
    const matchesTopic = topicFilter === '' || msg.topic.toLowerCase().includes(topicFilter.toLowerCase());
    return matchesClient && matchesTopic;
  });

  const totalPages = Math.ceil(filteredMessages.length / PAGE_SIZE);
  const paginatedMessages = filteredMessages.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  const handleDelete = (id: string) => {
    setMessageToDelete(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (messageToDelete) {
      await mockApi.deleteDelayedMessage(messageToDelete);
      setMessages(prev => prev.filter(m => m.id !== messageToDelete));
      if (selectedMessage?.id === messageToDelete) {
        setSelectedMessage(null);
        setShowDetailDrawer(false);
      }
      setShowDeleteConfirm(false);
      setMessageToDelete(null);
    }
  };

  const handleMessageClick = (msg: DelayedMessage) => {
    setSelectedMessage(msg);
    setShowDetailDrawer(true);
  };

  const closeDetailDrawer = () => {
    setShowDetailDrawer(false);
    setTimeout(() => setSelectedMessage(null), 300);
  };

  const formatTime = (iso: string) => new Date(iso).toLocaleString('zh-CN');

  const formatDuration = (seconds: number) => {
    if (seconds < 60) return `${seconds}秒`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}小时`;
    return `${Math.floor(seconds / 86400)}天`;
  };

  const getRemainingTime = (scheduledTime: string) => {
    const remaining = new Date(scheduledTime).getTime() - currentTime;
    if (remaining <= 0) return '即将发送';
    return formatDuration(Math.ceil(remaining / 1000));
  };

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Clock className="w-6 h-6 text-purple-400" />
          <h1 className="text-2xl font-bold text-white">延时消息管理</h1>
        </div>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索客户端ID..."
            value={clientIdFilter}
            onChange={(e) => { setClientIdFilter(e.target.value); setCurrentPage(1); }}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-purple-500"
          />
        </div>
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索Topic..."
            value={topicFilter}
            onChange={(e) => { setTopicFilter(e.target.value); setCurrentPage(1); }}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-purple-500"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-400 mr-3" />
          加载中...
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-900">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">客户端</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">主题</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">延时</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">剩余时间</th>
                <th className="px-4 py-3 text-center text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {paginatedMessages.map((msg) => (
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
                    <td className="px-4 py-3 text-sm text-slate-400">{formatDuration(msg.delaySeconds)}</td>
                    <td className="px-4 py-3 text-sm text-amber-400 font-medium">{getRemainingTime(msg.scheduledTime)}</td>
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
          {paginatedMessages.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无延时消息</p>
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-700">
              <span className="text-slate-400 text-sm">
                共 {filteredMessages.length} 条，第 {currentPage}/{totalPages} 页
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                  disabled={currentPage === 1}
                  className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-50"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                  disabled={currentPage === totalPages}
                  className="p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-50"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
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
                    <span className="text-purple-400 font-mono">{selectedMessage.topic}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Tag className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">QoS:</span>
                    <span className="text-white">{selectedMessage.qos}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">发布时间:</span>
                    <span className="text-white">{formatTime(selectedMessage.publishTime)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-amber-400" />
                    <span className="text-slate-400">延时:</span>
                    <span className="text-amber-400">{formatDuration(selectedMessage.delaySeconds)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-emerald-400" />
                    <span className="text-slate-400">计划发送时间:</span>
                    <span className="text-emerald-400">{formatTime(selectedMessage.scheduledTime)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-cyan-400" />
                    <span className="text-slate-400">剩余时间:</span>
                    <span className="text-cyan-400 font-medium">{getRemainingTime(selectedMessage.scheduledTime)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Hash className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">编码:</span>
                    <span className="text-white">{selectedMessage.encoding}</span>
                  </div>
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
                <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center">
                  <Trash2 className="w-5 h-5 text-red-400" />
                </div>
                <h3 className="text-lg font-semibold text-white">确认删除</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要删除这条延时消息吗？此操作不可恢复。</p>
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
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
