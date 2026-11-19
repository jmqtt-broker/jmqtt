import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FileText, Trash2, Plus, X, Clock, MessageSquare, Tag, Hash, Copy, CheckCircle2, AlertCircle, Search, User } from 'lucide-react';
import type { RetainedMessage } from '../types/mqtt';
import { mockApi } from '../services/mockApi';
import { qosLevels } from '../mock/mqttData';

export default function RetainedMessages() {
  const [messages, setMessages] = useState<RetainedMessage[]>([]);
  const [selectedMessage, setSelectedMessage] = useState<RetainedMessage | null>(null);
  const [showDetailDrawer, setShowDetailDrawer] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [messageToDelete, setMessageToDelete] = useState<string | null>(null);
  const [newMessage, setNewMessage] = useState({
    topic: '',
    payload: '',
    qos: 1,
    encoding: 'plain' as const,
    retain: true,
    payloadFormatIndicator: false,
    messageExpiryInterval: 0,
    contentType: '',
    responseTopic: '',
    correlationData: ''
  });
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [clientIdFilter, setClientIdFilter] = useState('');
  const [topicFilter, setTopicFilter] = useState('');

  useEffect(() => {
    loadMessages();
  }, []);

  const loadMessages = async () => {
    const data = await mockApi.getRetainedMessages();
    setMessages(data);
  };

  const filteredMessages = messages.filter(msg => {
    const matchesClient = clientIdFilter === '' || msg.topic.toLowerCase().includes(clientIdFilter.toLowerCase());
    const matchesTopic = topicFilter === '' || msg.topic.toLowerCase().includes(topicFilter.toLowerCase());
    return matchesClient && matchesTopic;
  });

  const handleDelete = async (id: string) => {
    setMessageToDelete(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (messageToDelete) {
      await mockApi.deleteRetainedMessage(messageToDelete);
      loadMessages();
      setShowDeleteConfirm(false);
      setMessageToDelete(null);
      if (selectedMessage?.id === messageToDelete) {
        setSelectedMessage(null);
        setShowDetailDrawer(false);
      }
    }
  };

  const handlePublish = async () => {
    await mockApi.publishRetainedMessage(newMessage);
    setNewMessage({
      topic: '',
      payload: '',
      qos: 1,
      encoding: 'plain',
      retain: true,
      payloadFormatIndicator: false,
      messageExpiryInterval: 0,
      contentType: '',
      responseTopic: '',
      correlationData: ''
    });
    setShowCreateModal(false);
    loadMessages();
  };

  const handleMessageClick = (msg: RetainedMessage) => {
    setSelectedMessage(msg);
    setShowDetailDrawer(true);
  };

  const closeDetailDrawer = () => {
    setShowDetailDrawer(false);
    setTimeout(() => setSelectedMessage(null), 300);
  };

  const formatPayload = (msg: RetainedMessage) => {
    if (msg.encoding === 'base64') {
      try {
        return atob(msg.payload);
      } catch {
        return msg.payload;
      }
    }
    if (msg.encoding === 'hex') {
      try {
        return msg.payload.match(/.{1,2}/g)?.map(b => String.fromCharCode(parseInt(b, 16))).join('') || msg.payload;
      } catch {
        return msg.payload;
      }
    }
    return msg.payload;
  };

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const formatTime = (iso: string) => new Date(iso).toLocaleString('zh-CN');

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <FileText className="w-6 h-6 text-amber-400" />
          <h1 className="text-2xl font-bold text-white">保留消息管理</h1>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          发布保留消息
        </button>
      </div>

      <div className="mb-6 flex gap-4">
        <div className="flex-1 relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索客户端ID..."
            value={clientIdFilter}
            onChange={(e) => setClientIdFilter(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
          <input
            type="text"
            placeholder="搜索Topic..."
            value={topicFilter}
            onChange={(e) => setTopicFilter(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>
      </div>

      <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
        <table className="w-full">
          <thead className="bg-slate-900">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">主题</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">QoS</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">编码</th>
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
                  <td className="px-4 py-3 text-sm text-slate-400">{msg.encoding}</td>
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
            <FileText className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>暂无保留消息</p>
          </div>
        )}
      </div>

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
                    <MessageSquare className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">主题:</span>
                    <span className="text-amber-400 font-mono">{selectedMessage.topic}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Tag className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">QoS:</span>
                    <span className="text-white">{selectedMessage.qos}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Hash className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">编码:</span>
                    <span className="text-white">{selectedMessage.encoding}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">时间:</span>
                    <span className="text-white">{formatTime(selectedMessage.timestamp)}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <AlertCircle className="w-4 h-4 text-slate-400" />
                    <span className="text-slate-400">保留标志:</span>
                    <span className="text-white">{selectedMessage.retain ? '是' : '否'}</span>
                  </div>
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
                      {formatPayload(selectedMessage)}
                    </pre>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-lg max-h-[90vh] overflow-y-auto"
          >
            <div className="flex items-center justify-between p-4 border-b border-slate-700">
              <h3 className="text-lg font-semibold text-white">发布保留消息</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white"><X className="w-5 h-5" /></button>
            </div>
            <div className="p-4 space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">Topic</label>
                <input
                  type="text"
                  value={newMessage.topic}
                  onChange={(e) => setNewMessage({ ...newMessage, topic: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:border-blue-500 focus:outline-none"
                  placeholder="输入topic..."
                />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">QoS</label>
                <select
                  value={newMessage.qos}
                  onChange={(e) => setNewMessage({ ...newMessage, qos: parseInt(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:border-blue-500 focus:outline-none"
                >
                  {qosLevels.map(q => <option key={q.value} value={q.value}>{q.label}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">编码</label>
                <select
                  value={newMessage.encoding}
                  onChange={(e) => setNewMessage({ ...newMessage, encoding: e.target.value as 'plain' | 'base64' | 'hex' })}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:border-blue-500 focus:outline-none"
                >
                  <option value="plain">Plain Text</option>
                  <option value="base64">Base64</option>
                  <option value="hex">Hex</option>
                </select>
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">Payload</label>
                <textarea
                  value={newMessage.payload}
                  onChange={(e) => setNewMessage({ ...newMessage, payload: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:border-blue-500 focus:outline-none h-32 resize-none"
                  placeholder="输入消息内容..."
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button onClick={() => setShowCreateModal(false)} className="px-4 py-2 text-slate-300 hover:text-white transition-colors">取消</button>
                <button onClick={handlePublish} className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors">发布</button>
              </div>
            </div>
          </motion.div>
        </div>
      )}

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
            <p className="text-slate-300 mb-6">确定要删除这条保留消息吗？此操作不可恢复。</p>
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
    </div>
  );
}
