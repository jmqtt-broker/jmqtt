import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Users,
  Wifi,
  MessageSquare,
  Database,
  HardDrive,
  Activity,
  TrendingUp,
  Server
} from 'lucide-react';
import { mockApi } from '../services/mockApi';
import type { DashboardStats, BrokerStats } from '../types/mqtt';

const StatCard = ({ icon: Icon, label, value, subtext, color }: {
  icon: typeof Users;
  label: string;
  value: string | number;
  subtext?: string;
  color: string;
}) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6 hover:bg-slate-800/70 transition-colors"
  >
    <div className="flex items-start justify-between">
      <div className={`p-3 rounded-lg ${color}`}>
        <Icon className="w-6 h-6 text-white" />
      </div>
    </div>
    <div className="mt-4">
      <p className="text-slate-400 text-sm">{label}</p>
      <p className="text-2xl font-bold text-white mt-1">{value}</p>
      {subtext && <p className="text-slate-500 text-xs mt-1">{subtext}</p>}
    </div>
  </motion.div>
);

const SimpleBarChart = ({ data, color }: { data: number[]; color: string }) => {
  const max = Math.max(...data);
  return (
    <div className="flex items-end gap-1 h-24">
      {data.map((v, i) => (
        <motion.div
          key={i}
          initial={{ height: 0 }}
          animate={{ height: `${(v / max) * 100}%` }}
          transition={{ delay: i * 0.05 }}
          className={`flex-1 rounded-t ${color} opacity-80 hover:opacity-100`}
        />
      ))}
    </div>
  );
};

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [brokerStats, setBrokerStats] = useState<BrokerStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      const [dashStats, broker] = await Promise.all([
        mockApi.getDashboardStats(),
        mockApi.getBrokerStats()
      ]);
      setStats(dashStats);
      setBrokerStats(broker);
      setLoading(false);
    };
    loadData();
  }, []);

  if (loading || !stats || !brokerStats) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
      </div>
    );
  }

  const formatUptime = (seconds: number) => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${days}天 ${hours}小时 ${mins}分钟`;
  };

  const formatBytes = (bytes: number) => {
    if (bytes >= 1e9) return `${(bytes / 1e9).toFixed(2)} GB`;
    if (bytes >= 1e6) return `${(bytes / 1e6).toFixed(2)} MB`;
    if (bytes >= 1e3) return `${(bytes / 1e3).toFixed(2)} KB`;
    return `${bytes} B`;
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">系统概览</h1>
          <p className="text-slate-400 text-sm mt-1">MQTT Broker 实时监控面板</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <Server className="w-4 h-4" />
          <span>版本 {brokerStats.version}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={Users}
          label="总用户数"
          value={stats.totalUsers}
          subtext="包含超级用户"
          color="bg-blue-500"
        />
        <StatCard
          icon={Wifi}
          label="在线客户端"
          value={stats.totalClients}
          subtext={`最大连接: ${brokerStats.clientsMaximum}`}
          color="bg-green-500"
        />
        <StatCard
          icon={MessageSquare}
          label="订阅总数"
          value={stats.totalSubscriptions}
          subtext="活跃订阅主题"
          color="bg-purple-500"
        />
        <StatCard
          icon={Database}
          label="保留消息"
          value={stats.totalRetainedMessages}
          subtext="持久化消息"
          color="bg-orange-500"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-blue-400" />
              <h3 className="text-white font-semibold">消息速率</h3>
            </div>
            <span className="text-2xl font-bold text-blue-400">
              {stats.messagesPerSecond.toFixed(1)}
              <span className="text-sm text-slate-400 ml-1">msg/s</span>
            </span>
          </div>
          <SimpleBarChart
            data={[30, 45, 35, 50, 42, 55, 48, 60, 52, 45, 58, 65]}
            color="bg-blue-500"
          />
          <div className="flex justify-between text-xs text-slate-500 mt-2">
            <span>-60s</span>
            <span>-30s</span>
            <span>现在</span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-green-400" />
              <h3 className="text-white font-semibold">流量趋势</h3>
            </div>
            <span className="text-2xl font-bold text-green-400">
              {formatBytes(brokerStats.bytesPerSecond)}/s
            </span>
          </div>
          <SimpleBarChart
            data={[1200, 1500, 1100, 1800, 1400, 2000, 1600, 2200, 1900, 1700, 2100, 1850]}
            color="bg-green-500"
          />
          <div className="flex justify-between text-xs text-slate-500 mt-2">
            <span>-60s</span>
            <span>-30s</span>
            <span>现在</span>
          </div>
        </motion.div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6"
      >
        <div className="flex items-center gap-2 mb-4">
          <HardDrive className="w-5 h-5 text-slate-400" />
          <h3 className="text-white font-semibold">系统状态</h3>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">运行时间</p>
            <p className="text-white font-medium mt-1">{formatUptime(brokerStats.uptime)}</p>
          </div>
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">接收消息</p>
            <p className="text-white font-medium mt-1">{brokerStats.messagesReceived.toLocaleString()}</p>
          </div>
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">发送消息</p>
            <p className="text-white font-medium mt-1">{brokerStats.messagesSent.toLocaleString()}</p>
          </div>
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">离线消息</p>
            <p className="text-white font-medium mt-1">{stats.totalOfflineMessages}</p>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4 mt-4">
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">接收流量</p>
            <p className="text-white font-medium mt-1">{formatBytes(brokerStats.bytesReceived)}</p>
          </div>
          <div className="bg-slate-900/50 rounded-lg p-4">
            <p className="text-slate-400 text-xs">发送流量</p>
            <p className="text-white font-medium mt-1">{formatBytes(brokerStats.bytesSent)}</p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
