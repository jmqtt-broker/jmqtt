import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Radio,
  Save,
  RotateCcw,
  AlertCircle,
  CheckCircle2,
  MessageSquare,
  Users,
  Layers,
  Webhook,
  Shield,
  Lock,
  Filter,
  Send,
  Play
} from 'lucide-react';

interface MqttConfig {
  maxQoS: number;
  maxPacketSize: number;
  maxNonQoS0Messages: number;
  maxTopicAlias: number;
  maxClientIdLength: number;
  maxTopicLevels: number;
  wildcardSubscription: boolean;
  sharedSubscription: boolean;
  retainedMessages: boolean;
  offlineMessages: boolean;
  sessionExpiryInterval: number;
}

interface WebhookConfig {
  enabled: boolean;
  url: string;
  timeout: number;
}

interface Webhooks {
  clientAuth: WebhookConfig;
  blacklistCheck: WebhookConfig;
  subscribeAuth: WebhookConfig;
  publishAuth: WebhookConfig;
}

const defaultConfig: MqttConfig = {
  maxQoS: 2,
  maxPacketSize: 20,
  maxNonQoS0Messages: 65535,
  maxTopicAlias: 65535,
  maxClientIdLength: 300,
  maxTopicLevels: 20,
  wildcardSubscription: true,
  sharedSubscription: true,
  retainedMessages: true,
  offlineMessages: true,
  sessionExpiryInterval: 2
};

const defaultWebhooks: Webhooks = {
  clientAuth: {
    enabled: false,
    url: 'http://localhost:8080/webhook/client-auth',
    timeout: 5000
  },
  blacklistCheck: {
    enabled: false,
    url: 'http://localhost:8080/webhook/blacklist-check',
    timeout: 3000
  },
  subscribeAuth: {
    enabled: false,
    url: 'http://localhost:8080/webhook/subscribe-auth',
    timeout: 5000
  },
  publishAuth: {
    enabled: false,
    url: 'http://localhost:8080/webhook/publish-auth',
    timeout: 5000
  }
};

const qosOptions = [
  { value: 0, label: 'QoS 0 - 最多一次', description: '消息可能丢失' },
  { value: 1, label: 'QoS 1 - 至少一次', description: '消息可能重复' },
  { value: 2, label: 'QoS 2 - 恰好一次', description: '消息保证送达' }
];

const ConfigSection: React.FC<{
  title: string;
  icon: React.ElementType;
  children: React.ReactNode;
}> = ({ title, icon: Icon, children }) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6"
  >
    <div className="flex items-center gap-3 mb-6">
      <div className="p-2 bg-blue-500/20 rounded-lg">
        <Icon className="w-5 h-5 text-blue-400" />
      </div>
      <h3 className="text-lg font-semibold text-white">{title}</h3>
    </div>
    <div className="space-y-5">
      {children}
    </div>
  </motion.div>
);

const ConfigItem: React.FC<{
  label: string;
  description?: string;
  children: React.ReactNode;
}> = ({ label, description, children }) => (
  <div className="flex items-start justify-between gap-4">
    <div className="flex-1">
      <label className="text-slate-200 font-medium">{label}</label>
      {description && (
        <p className="text-slate-500 text-sm mt-1">{description}</p>
      )}
    </div>
    <div className="w-48">
      {children}
    </div>
  </div>
);

const ToggleItem: React.FC<{
  label: string;
  description?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}> = ({ label, description, checked, onChange }) => (
  <div className="flex items-center justify-between gap-4">
    <div className="flex-1">
      <label className="text-slate-200 font-medium">{label}</label>
      {description && (
        <p className="text-slate-500 text-sm mt-1">{description}</p>
      )}
    </div>
    <button
      onClick={() => onChange(!checked)}
      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
        checked ? 'bg-blue-500' : 'bg-slate-600'
      }`}
    >
      <span
        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
          checked ? 'translate-x-6' : 'translate-x-1'
        }`}
      />
    </button>
  </div>
);

export default function MqttConfig() {
  const [config, setConfig] = useState<MqttConfig>(defaultConfig);
  const [webhooks, setWebhooks] = useState<Webhooks>(defaultWebhooks);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<'mqtt' | 'webhook'>('mqtt');
  const [testResult, setTestResult] = useState<{hook: string; success: boolean; message: string} | null>(null);

  const handleSave = async () => {
    setSaving(true);
    await new Promise(resolve => setTimeout(resolve, 500));
    setSaving(false);
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  const handleReset = () => {
    setConfig(defaultConfig);
    setWebhooks(defaultWebhooks);
  };

  const updateConfig = (key: keyof MqttConfig, value: any) => {
    setConfig(prev => ({ ...prev, [key]: value }));
  };

  const updateWebhook = (key: keyof Webhooks, field: keyof WebhookConfig, value: any) => {
    setWebhooks(prev => ({
      ...prev,
      [key]: { ...prev[key], [field]: value }
    }));
  };

  const testWebhook = async (hookKey: keyof Webhooks, hookName: string) => {
    const hook = webhooks[hookKey];
    if (!hook.enabled || !hook.url) {
      setTestResult({ hook: hookName, success: false, message: 'Webhook 未启用或 URL 为空' });
      return;
    }

    setTestResult({ hook: hookName, success: true, message: '测试请求已发送，等待响应...' });
    await new Promise(resolve => setTimeout(resolve, 1000));

    const mockResponse = { code: 0, msg: 'success' };
    if (mockResponse.code === 0) {
      setTestResult({ hook: hookName, success: true, message: `测试成功: ${mockResponse.msg}` });
    } else {
      setTestResult({ hook: hookName, success: false, message: `测试失败: ${mockResponse.msg}` });
    }
    setTimeout(() => setTestResult(null), 3000);
  };

  const webhookConfigs = [
    {
      key: 'clientAuth' as keyof Webhooks,
      title: '客户端登录认证',
      icon: Lock,
      description: 'MQTT 客户端连接时的登录认证 Hook',
      params: [
        { name: 'clientId', type: 'string', desc: '客户端 ID' },
        { name: 'username', type: 'string', desc: '用户名' },
        { name: 'password', type: 'string', desc: '密码' }
      ]
    },
    {
      key: 'blacklistCheck' as keyof Webhooks,
      title: '黑名单校验',
      icon: Shield,
      description: '检查客户端是否在黑名单中',
      params: [
        { name: 'clientId', type: 'string', desc: '客户端 ID' },
        { name: 'ip', type: 'string', desc: '客户端 IP 地址' },
        { name: 'port', type: 'number', desc: '客户端端口' }
      ]
    },
    {
      key: 'subscribeAuth' as keyof Webhooks,
      title: '订阅权限校验',
      icon: Filter,
      description: '判断客户端是否有权限订阅该 Topic',
      params: [
        { name: 'clientId', type: 'string', desc: '客户端 ID' },
        { name: 'topic', type: 'string', desc: '订阅主题' }
      ]
    },
    {
      key: 'publishAuth' as keyof Webhooks,
      title: '发布权限校验',
      icon: Send,
      description: '判断客户端是否有权限向该 Topic 发布消息',
      params: [
        { name: 'clientId', type: 'string', desc: '客户端 ID' },
        { name: 'topic', type: 'string', desc: '发布主题' }
      ]
    }
  ];

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-3">
            <Radio className="w-7 h-7 text-blue-400" />
            MQTT 配置
          </h1>
          <p className="text-slate-400 mt-1">配置 MQTT Broker 运行参数和 Webhook</p>
        </div>
        <div className="flex items-center gap-3">
          {saved && (
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="flex items-center gap-2 text-emerald-400"
            >
              <CheckCircle2 className="w-5 h-5" />
              <span className="text-sm">保存成功</span>
            </motion.div>
          )}
          <button
            onClick={handleReset}
            className="flex items-center gap-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            重置
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {saving ? '保存中...' : '保存配置'}
          </button>
        </div>
      </div>

      <div className="flex gap-2 border-b border-slate-700">
        <button
          onClick={() => setActiveTab('mqtt')}
          className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
            activeTab === 'mqtt'
              ? 'text-blue-400 border-blue-400'
              : 'text-slate-400 border-transparent hover:text-white'
          }`}
        >
          <Radio className="w-4 h-4" />
          MQTT 配置
        </button>
        <button
          onClick={() => setActiveTab('webhook')}
          className={`flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2 ${
            activeTab === 'webhook'
              ? 'text-blue-400 border-blue-400'
              : 'text-slate-400 border-transparent hover:text-white'
          }`}
        >
          <Webhook className="w-4 h-4" />
          Webhook 配置
        </button>
      </div>

      {testResult && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          className={`p-4 rounded-lg flex items-center gap-3 ${
            testResult.success ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'
          }`}
        >
          {testResult.success ? <CheckCircle2 className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
          <div>
            <p className="font-medium">{testResult.hook}</p>
            <p className="text-sm opacity-80">{testResult.message}</p>
          </div>
        </motion.div>
      )}

      {activeTab === 'mqtt' ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ConfigSection title="消息配置" icon={MessageSquare}>
            <ConfigItem label="最大 QoS" description="允许的最大服务质量等级">
              <select
                value={config.maxQoS}
                onChange={(e) => updateConfig('maxQoS', parseInt(e.target.value))}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
              >
                {qosOptions.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </ConfigItem>

            <ConfigItem label="最大报文大小" description="允许的最大 MQTT 报文大小">
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  value={config.maxPacketSize}
                  onChange={(e) => updateConfig('maxPacketSize', parseInt(e.target.value) || 0)}
                  className="flex-1 px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                  min={1}
                  max={256}
                />
                <span className="text-slate-400 text-sm">MB</span>
              </div>
            </ConfigItem>

            <ConfigItem label="非 QoS 0 最大消息数" description="非 QoS 0 会话的最大消息数">
              <input
                type="number"
                value={config.maxNonQoS0Messages}
                onChange={(e) => updateConfig('maxNonQoS0Messages', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                min={1}
              />
            </ConfigItem>

            <ConfigItem label="主题别名最大值" description="每个连接允许的最大主题别名数">
              <input
                type="number"
                value={config.maxTopicAlias}
                onChange={(e) => updateConfig('maxTopicAlias', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                min={0}
              />
            </ConfigItem>
          </ConfigSection>

          <ConfigSection title="连接配置" icon={Users}>
            <ConfigItem label="最大客户端 ID 长度" description="客户端 ID 允许的最大字符数">
              <input
                type="number"
                value={config.maxClientIdLength}
                onChange={(e) => updateConfig('maxClientIdLength', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                min={1}
                max={65535}
              />
            </ConfigItem>

            <ConfigItem label="最大主题层级" description="主题名称允许的最大层级数">
              <input
                type="number"
                value={config.maxTopicLevels}
                onChange={(e) => updateConfig('maxTopicLevels', parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                min={1}
                max={128}
              />
            </ConfigItem>

            <ConfigItem label="会话过期间隔" description="非5.0客户端会话过期时间">
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  value={config.sessionExpiryInterval}
                  onChange={(e) => updateConfig('sessionExpiryInterval', parseInt(e.target.value) || 0)}
                  className="flex-1 px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                  min={0}
                />
                <span className="text-slate-400 text-sm">小时</span>
              </div>
            </ConfigItem>
          </ConfigSection>

          <ConfigSection title="功能开关" icon={Layers}>
            <ToggleItem
              label="通配符订阅"
              description="允许使用 + 和 # 通配符进行订阅"
              checked={config.wildcardSubscription}
              onChange={(checked) => updateConfig('wildcardSubscription', checked)}
            />
            <ToggleItem
              label="共享订阅"
              description="允许多个客户端共享同一个订阅"
              checked={config.sharedSubscription}
              onChange={(checked) => updateConfig('sharedSubscription', checked)}
            />
            <ToggleItem
              label="保留消息"
              description="允许发布和接收保留消息"
              checked={config.retainedMessages}
              onChange={(checked) => updateConfig('retainedMessages', checked)}
            />
            <ToggleItem
              label="离线消息"
              description="为离线客户端存储消息"
              checked={config.offlineMessages}
              onChange={(checked) => updateConfig('offlineMessages', checked)}
            />
          </ConfigSection>

          <ConfigSection title="配置说明" icon={AlertCircle}>
            <div className="bg-slate-900/50 rounded-lg p-4 space-y-3">
              <div className="flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5" />
                <div>
                  <p className="text-slate-200 font-medium">配置生效时间</p>
                  <p className="text-slate-500 text-sm">配置更改将在保存后立即生效，无需重启服务。</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5" />
                <div>
                  <p className="text-slate-200 font-medium">默认值恢复</p>
                  <p className="text-slate-500 text-sm">点击"重置"按钮可将所有配置项恢复为默认值。</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-amber-400 mt-0.5" />
                <div>
                  <p className="text-slate-200 font-medium">注意事项</p>
                  <p className="text-slate-500 text-sm">修改最大报文大小等关键参数可能影响现有连接，请谨慎操作。</p>
                </div>
              </div>
            </div>
          </ConfigSection>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-500/20 rounded-lg">
                <Webhook className="w-5 h-5 text-blue-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Webhook 说明</h3>
                <p className="text-slate-400 text-sm">所有 Webhook 使用 HTTP POST 请求，参数以 JSON 格式发送</p>
              </div>
            </div>
            <div className="bg-slate-900/50 rounded-lg p-4">
              <p className="text-slate-300 text-sm mb-2">响应格式：</p>
              <pre className="text-slate-400 text-sm font-mono">
{`{
  "code": 0,    // 0 表示通过，非 0 表示未通过
  "msg": ""     // 未通过时的原因说明
}`}
              </pre>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {webhookConfigs.map((webhook) => {
              const hook = webhooks[webhook.key];
              return (
                <motion.div
                  key={webhook.key}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-6"
                >
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-blue-500/20 rounded-lg">
                        <webhook.icon className="w-5 h-5 text-blue-400" />
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold text-white">{webhook.title}</h3>
                        <p className="text-slate-400 text-sm">{webhook.description}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => updateWebhook(webhook.key, 'enabled', !hook.enabled)}
                      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                        hook.enabled ? 'bg-blue-500' : 'bg-slate-600'
                      }`}
                    >
                      <span
                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                          hook.enabled ? 'translate-x-6' : 'translate-x-1'
                        }`}
                      />
                    </button>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <label className="text-slate-300 text-sm font-medium mb-2 block">请求 URL</label>
                      <input
                        type="text"
                        value={hook.url}
                        onChange={(e) => updateWebhook(webhook.key, 'url', e.target.value)}
                        disabled={!hook.enabled}
                        className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        placeholder="http://localhost:8080/webhook/..."
                      />
                    </div>

                    <div>
                      <label className="text-slate-300 text-sm font-medium mb-2 block">超时时间 (ms)</label>
                      <input
                        type="number"
                        value={hook.timeout}
                        onChange={(e) => updateWebhook(webhook.key, 'timeout', parseInt(e.target.value) || 5000)}
                        disabled={!hook.enabled}
                        className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                        min={1000}
                        max={30000}
                        step={1000}
                      />
                    </div>

                    <div>
                      <label className="text-slate-300 text-sm font-medium mb-2 block">请求参数</label>
                      <div className="bg-slate-900/50 rounded-lg p-3">
                        {webhook.params.map((param, idx) => (
                          <div key={param.name} className={`flex items-center justify-between py-1 ${idx !== webhook.params.length - 1 ? 'border-b border-slate-700/50' : ''}`}>
                            <span className="text-slate-300 text-sm font-mono">{param.name}</span>
                            <span className="text-slate-500 text-xs">{param.type}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <button
                      onClick={() => testWebhook(webhook.key, webhook.title)}
                      disabled={!hook.enabled}
                      className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 disabled:bg-slate-800 disabled:text-slate-500 text-slate-200 rounded-lg transition-colors"
                    >
                      <Play className="w-4 h-4" />
                      测试 Webhook
                    </button>
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
