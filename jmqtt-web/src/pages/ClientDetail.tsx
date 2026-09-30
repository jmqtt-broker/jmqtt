import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, NavLink } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Wifi,
  WifiOff,
  ArrowLeft,
  Server,
  Clock,
  Activity,
  Settings,
  FileText,
  MessageSquare,
  Power,
  ChevronRight,
  Copy,
  CheckCircle2,
  AlertCircle,
  Info,
  Tag,
  Calendar,
  Hash,
  Maximize2,
  Shield,
  Zap,
  Bell,
  Trash2,
  X
} from 'lucide-react';
import type { MQTTClient, ClientDetail as ClientDetailType } from '../types/mqtt';
import { mockApi } from '../services/mockApi';

export default function ClientDetail() {
  const { clientId } = useParams<{ clientId: string }>();
  const navigate = useNavigate();
  const [client, setClient] = useState<MQTTClient | null>(null);
  const [clientDetail, setClientDetail] = useState<ClientDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'connection' | 'will'>('overview');
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [showDisconnectConfirm, setShowDisconnectConfirm] = useState(false);

  useEffect(() => {
    if (clientId) {
      loadClientDetail(clientId);
    }
  }, [clientId]);

  const loadClientDetail = async (id: string) => {
    setLoading(true);
    const [clientData, detailData] = await Promise.all([
      mockApi.getClientById(id),
      mockApi.getClientDetailById(id)
    ]);
    setClient(clientData);
    setClientDetail(detailData);
    setLoading(false);
  };

  const handleDisconnect = () => {
    setShowDisconnectConfirm(true);
  };

  const confirmDisconnect = async () => {
    if (client) {
      await mockApi.disconnectClient(client.id);
      setShowDisconnectConfirm(false);
      navigate('/monitor/clients');
    }
  };

  const copyToClipboard = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDuration = (seconds: number) => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (days > 0) return `${days}天 ${hours}小时`;
    if (hours > 0) return `${hours}小时 ${mins}分钟`;
    return `${mins}分钟`;
  };

  if (loading) {
    return (
      <div className="p-6 flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!client || !clientDetail) {
    return (
      <div className="p-6">
        <div className="text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-3 text-red-400" />
          <p className="text-slate-400">客户端不存在或已被断开</p>
          <NavLink
            to="/monitor/clients"
            className="mt-4 inline-flex items-center gap-2 text-blue-400 hover:text-blue-300"
          >
            <ArrowLeft className="w-4 h-4" />
            返回客户端列表
          </NavLink>
        </div>
      </div>
    );
  }

  const InfoCard = ({ title, children, className = '' }: { title: string; children: React.ReactNode; className?: string }) => (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`bg-slate-800 rounded-xl border border-slate-700 ${className}`}
    >
      <div className="p-4 border-b border-slate-700">
        <h3 className="text-white font-medium">{title}</h3>
      </div>
      <div className="p-4">{children}</div>
    </motion.div>
  );

  const InfoItem = ({ label, value, copyable = false, copyValue = '' }: { label: string; value: React.ReactNode; copyable?: boolean; copyValue?: string }) => (
    <div className="flex items-start justify-between py-2 border-b border-slate-700/50 last:border-0">
      <span className="text-slate-400 text-sm">{label}</span>
      <div className="flex items-center gap-2">
        <span className="text-white text-sm font-medium">{value}</span>
        {copyable && (
          <button
            onClick={() => copyToClipboard(copyValue || String(value), label)}
            className="text-slate-500 hover:text-blue-400 transition-colors"
          >
            {copiedField === label ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
          </button>
        )}
      </div>
    </div>
  );

  const StatusBadge = ({ status }: { status: string }) => {
    const isOnline = status === 'online';
    return (
      <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium ${
        isOnline ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-700 text-slate-400'
      }`}>
        {isOnline ? <Wifi className="w-3 h-3" /> : <WifiOff className="w-3 h-3" />}
        {isOnline ? '在线' : '离线'}
      </span>
    );
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <NavLink
          to="/monitor/clients"
          className="inline-flex items-center gap-2 text-slate-400 hover:text-white transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          返回客户端列表
        </NavLink>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className={`w-14 h-14 rounded-xl flex items-center justify-center ${
              clientDetail.status === 'online' ? 'bg-emerald-500/20' : 'bg-slate-700'
            }`}>
              {clientDetail.status === 'online' ? (
                <Wifi className="w-7 h-7 text-emerald-400" />
              ) : (
                <WifiOff className="w-7 h-7 text-slate-400" />
              )}
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">{client.clientId}</h1>
              <div className="flex items-center gap-3 mt-1">
                <StatusBadge status={clientDetail.status} />
                <span className="text-slate-400 text-sm">{client.username || '未认证'}</span>
                <span className="text-slate-500 text-sm">|</span>
                <span className="text-slate-400 text-sm">{client.peername}</span>
              </div>
            </div>
          </div>

          <button
            onClick={handleDisconnect}
            className="flex items-center gap-2 px-4 py-2 bg-red-500/20 text-red-400 hover:bg-red-500/30 rounded-lg transition-colors"
          >
            <Power className="w-4 h-4" />
            断开连接
          </button>
        </div>
      </div>

      <div className="flex gap-2 mb-6 border-b border-slate-700">
        {[
          { id: 'overview', label: '概览', icon: Info },
          { id: 'connection', label: '连接属性', icon: Settings },
          { id: 'will', label: '遗嘱消息', icon: FileText }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as typeof activeTab)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
              activeTab === tab.id
                ? 'text-blue-400 border-blue-400'
                : 'text-slate-400 border-transparent hover:text-white'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <InfoCard title="基本信息">
              <div className="grid grid-cols-2 gap-4">
                <InfoItem label="所属节点" value={clientDetail.node} />
                <InfoItem label="客户端版本" value={clientDetail.version} />
                <InfoItem label="协议版本" value={`MQTT ${client.protocolVersion}`} />
                <InfoItem label="Clean Start" value={clientDetail.cleanStart ? '是' : '否'} />
                <InfoItem label="心跳周期" value={`${client.keepalive} 秒`} />
                <InfoItem label="客户端ID" value={client.clientId} copyable />
              </div>
            </InfoCard>

            <InfoCard title="时间信息">
              <div className="grid grid-cols-2 gap-4">
                <InfoItem
                  label="上线时间"
                  value={new Date(clientDetail.connectedAt).toLocaleString('zh-CN')}
                />
                <InfoItem
                  label="连接时长"
                  value={formatDuration(clientDetail.connectionDuration)}
                />
                {clientDetail.disconnectedAt && (
                  <InfoItem
                    label="离线时间"
                    value={new Date(clientDetail.disconnectedAt).toLocaleString('zh-CN')}
                  />
                )}
                {clientDetail.lastSeen && (
                  <InfoItem
                    label="最后活跃"
                    value={new Date(clientDetail.lastSeen).toLocaleString('zh-CN')}
                  />
                )}
              </div>
            </InfoCard>

            <InfoCard title="流量统计">
              <div className="grid grid-cols-4 gap-4">
                <div className="bg-slate-900/50 rounded-lg p-4">
                  <p className="text-slate-400 text-xs mb-1">接收消息</p>
                  <p className="text-emerald-400 text-xl font-bold">{client.messagesReceived.toLocaleString()}</p>
                  <p className="text-slate-500 text-xs">{formatBytes(client.bytesReceived)}</p>
                </div>
                <div className="bg-slate-900/50 rounded-lg p-4">
                  <p className="text-slate-400 text-xs mb-1">发送消息</p>
                  <p className="text-blue-400 text-xl font-bold">{client.messagesSent.toLocaleString()}</p>
                  <p className="text-slate-500 text-xs">{formatBytes(client.bytesSent)}</p>
                </div>
                <div className="bg-slate-900/50 rounded-lg p-4">
                  <p className="text-slate-400 text-xs mb-1">订阅数</p>
                  <p className="text-amber-400 text-xl font-bold">{client.subscriptions.length}</p>
                  <p className="text-slate-500 text-xs">活跃订阅</p>
                </div>
                <div className="bg-slate-900/50 rounded-lg p-4">
                  <p className="text-slate-400 text-xs mb-1">消息速率</p>
                  <p className="text-purple-400 text-xl font-bold">{clientDetail.messagesPerSecond.toFixed(1)}</p>
                  <p className="text-slate-500 text-xs">msg/s</p>
                </div>
              </div>
            </InfoCard>
          </div>

          <div className="space-y-6">
            <InfoCard title="订阅列表">
              {client.subscriptions.length > 0 ? (
                <div className="space-y-2">
                  {client.subscriptions.map((sub) => (
                    <div
                      key={sub.id}
                      className="flex items-center justify-between py-2 border-b border-slate-700/50 last:border-0"
                    >
                      <span className="text-slate-300 text-sm font-mono">{sub.topic}</span>
                      <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 rounded text-xs">
                        QoS {sub.qos}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-500 text-sm">暂无订阅</p>
              )}
            </InfoCard>

            <InfoCard title="客户端属性">
              <div className="space-y-2">
                <div className="flex items-center justify-between py-2">
                  <span className="text-slate-400 text-sm">桥接模式</span>
                  <span className={`text-sm ${client.isBridge ? 'text-amber-400' : 'text-slate-500'}`}>
                    {client.isBridge ? '是' : '否'}
                  </span>
                </div>
                <div className="flex items-center justify-between py-2">
                  <span className="text-slate-400 text-sm">持久会话</span>
                  <span className={`text-sm ${!client.cleanSession ? 'text-emerald-400' : 'text-slate-500'}`}>
                    {!client.cleanSession ? '是' : '否'}
                  </span>
                </div>
              </div>
            </InfoCard>
          </div>
        </div>
      )}

      {activeTab === 'connection' && clientDetail.connectionProps && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <InfoCard title="会话配置">
            <div className="space-y-2">
              <InfoItem
                label="Session 有效期"
                value={clientDetail.connectionProps.sessionExpiryInterval > 0
                  ? `${clientDetail.connectionProps.sessionExpiryInterval} 秒`
                  : '永不过期'
                }
              />
              <InfoItem
                label="最大接收值 (Receive Maximum)"
                value={clientDetail.connectionProps.receiveMaximum}
              />
              <InfoItem
                label="最大数据包大小"
                value={formatBytes(clientDetail.connectionProps.maximumPacketSize)}
              />
              <InfoItem
                label="主题别名最大值"
                value={clientDetail.connectionProps.topicAliasMaximum}
              />
            </div>
          </InfoCard>

          <InfoCard title="功能标志">
            <div className="space-y-2">
              <InfoItem
                label="请求响应信息"
                value={clientDetail.connectionProps.requestResponseInfo ? '已启用' : '未启用'}
              />
              <InfoItem
                label="请求错误信息"
                value={clientDetail.connectionProps.requestProblemInfo ? '已启用' : '未启用'}
              />
              <InfoItem
                label="保留消息可用"
                value={clientDetail.connectionProps.retainAvailable ? '是' : '否'}
              />
              <InfoItem
                label="通配符订阅可用"
                value={clientDetail.connectionProps.wildcardSubscriptionAvailable ? '是' : '否'}
              />
              <InfoItem
                label="共享订阅可用"
                value={clientDetail.connectionProps.sharedSubscriptionAvailable ? '是' : '否'}
              />
              <InfoItem
                label="订阅标识符可用"
                value={clientDetail.connectionProps.subscriptionIdentifierAvailable ? '是' : '否'}
              />
            </div>
          </InfoCard>

          {clientDetail.connectionProps.userProperties && clientDetail.connectionProps.userProperties.length > 0 && (
            <InfoCard title="用户属性" className="lg:col-span-2">
              <div className="grid grid-cols-2 gap-4">
                {clientDetail.connectionProps.userProperties.map((prop, index) => (
                  <div key={index} className="flex items-center justify-between py-2 border-b border-slate-700/50">
                    <span className="text-slate-400 text-sm">{prop.key}</span>
                    <span className="text-white text-sm font-mono">{prop.value}</span>
                  </div>
                ))}
              </div>
            </InfoCard>
          )}
        </div>
      )}

      {activeTab === 'will' && clientDetail.willMessage && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <InfoCard title="遗嘱消息基本信息">
            <div className="space-y-2">
              <InfoItem label="主题" value={clientDetail.willMessage.topic} copyable />
              <InfoItem label="QoS" value={`QoS ${clientDetail.willMessage.qos}`} />
              <InfoItem
                label="保留标志"
                value={clientDetail.willMessage.retain ? '是' : '否'}
              />
              <InfoItem
                label="有效载荷指示器"
                value={clientDetail.willMessage.payloadFormatIndicator ? '是' : '否'}
              />
            </div>
          </InfoCard>

          <InfoCard title="遗嘱消息时间配置">
            <div className="space-y-2">
              <InfoItem
                label="延迟时间"
                value={`${clientDetail.willMessage.willDelayInterval} 秒`}
              />
              <InfoItem
                label="消息过期时间"
                value={clientDetail.willMessage.messageExpiryInterval > 0
                  ? `${clientDetail.willMessage.messageExpiryInterval} 秒`
                  : '永不过期'
                }
              />
              <InfoItem
                label="内容类型"
                value={clientDetail.willMessage.contentType || 'application/octet-stream'}
              />
              <InfoItem
                label="响应主题"
                value={clientDetail.willMessage.responseTopic || '-'}
              />
            </div>
          </InfoCard>

          <InfoCard title="遗嘱消息内容" className="lg:col-span-2">
            <div className="bg-slate-900/50 rounded-lg p-4">
              <div className="flex items-center justify-between mb-3">
                <span className="text-slate-400 text-sm">消息内容</span>
                <button
                  onClick={() => copyToClipboard(clientDetail.willMessage!.payload, 'payload')}
                  className="text-slate-500 hover:text-blue-400 transition-colors"
                >
                  {copiedField === 'payload' ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                </button>
              </div>
              <pre className="text-slate-300 text-sm font-mono whitespace-pre-wrap break-all">
                {clientDetail.willMessage.payload}
              </pre>
            </div>
            {clientDetail.willMessage.correlationData && (
              <div className="mt-4 bg-slate-900/50 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-slate-400 text-sm">对比数据 (Correlation Data)</span>
                  <button
                    onClick={() => copyToClipboard(clientDetail.willMessage!.correlationData!, 'correlation')}
                    className="text-slate-500 hover:text-blue-400 transition-colors"
                  >
                    {copiedField === 'correlation' ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
                <pre className="text-slate-300 text-sm font-mono whitespace-pre-wrap break-all">
                  {clientDetail.willMessage.correlationData}
                </pre>
              </div>
            )}
          </InfoCard>

          {clientDetail.willMessage.userProperties && clientDetail.willMessage.userProperties.length > 0 && (
            <InfoCard title="用户属性" className="lg:col-span-2">
              <div className="grid grid-cols-2 gap-4">
                {clientDetail.willMessage.userProperties.map((prop, index) => (
                  <div key={index} className="flex items-center justify-between py-2 border-b border-slate-700/50">
                    <span className="text-slate-400 text-sm">{prop.key}</span>
                    <span className="text-white text-sm font-mono">{prop.value}</span>
                  </div>
                ))}
              </div>
            </InfoCard>
          )}
        </div>
      )}

      {activeTab === 'will' && !clientDetail.willMessage && (
        <div className="text-center py-12">
          <Bell className="w-12 h-12 mx-auto mb-3 text-slate-500" />
          <p className="text-slate-400">此客户端未设置遗嘱消息</p>
        </div>
      )}

      <AnimatePresence>
        {showDisconnectConfirm && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
              onClick={() => setShowDisconnectConfirm(false)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md z-50"
            >
              <div className="bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl overflow-hidden">
                <div className="p-6">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-full bg-red-500/20 flex items-center justify-center flex-shrink-0">
                      <Power className="w-6 h-6 text-red-400" />
                    </div>
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-white mb-2">确认断开连接</h3>
                      <p className="text-slate-400 text-sm">
                        确定要断开客户端 <span className="text-white font-medium">{client?.clientId}</span> 的连接吗？此操作将立即终止该客户端的会话。
                      </p>
                    </div>
                  </div>
                </div>
                <div className="px-6 py-4 bg-slate-800/50 border-t border-slate-700 flex justify-end gap-3">
                  <button
                    onClick={() => setShowDisconnectConfirm(false)}
                    className="px-4 py-2 text-slate-400 hover:text-white transition-colors text-sm font-medium"
                  >
                    取消
                  </button>
                  <button
                    onClick={confirmDisconnect}
                    className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors text-sm font-medium flex items-center gap-2"
                  >
                    <Power className="w-4 h-4" />
                    确认断开
                  </button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
