import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  GitBranch,
  List,
  Search,
  ChevronRight,
  Users,
  Mail,
  X,
  Filter,
  Eye,
  Settings,
  Check,
  XCircle,
  User
} from 'lucide-react';
import { mockApi } from '../services/mockApi';
import type { TopicNode, SubscriptionDetail } from '../types/mqtt';

interface TreeNodeProps {
  node: TopicNode;
  level: number;
  searchTerm: string;
  onSelect: (node: TopicNode) => void;
}

const TreeNode: React.FC<TreeNodeProps> = ({ node, level, searchTerm, onSelect }) => {
  const [isExpanded, setIsExpanded] = useState(level < 2);
  const hasChildren = node.children && node.children.length > 0;
  const isMatch = searchTerm && node.name.toLowerCase().includes(searchTerm.toLowerCase());

  useEffect(() => {
    if (searchTerm) {
      setIsExpanded(true);
    }
  }, [searchTerm]);

  return (
    <div className="select-none">
      <motion.div
        className={`flex items-center py-2 px-3 rounded-lg cursor-pointer transition-colors ${
          isMatch ? 'bg-amber-500/20 border border-amber-500/50' : 'hover:bg-slate-800'
        }`}
        style={{ paddingLeft: `${level * 24 + 12}px` }}
        onClick={() => {
          if (hasChildren) {
            setIsExpanded(!isExpanded);
          }
          onSelect(node);
        }}
      >
        {hasChildren && (
          <motion.div
            animate={{ rotate: isExpanded ? 90 : 0 }}
            transition={{ duration: 0.2 }}
          >
            <ChevronRight className="w-4 h-4 text-slate-500 mr-2" />
          </motion.div>
        )}
        {!hasChildren && <span className="w-5 mr-2" />}
        {hasChildren ? (
          <GitBranch className="w-4 h-4 text-amber-500 mr-2" />
        ) : (
          <Mail className="w-4 h-4 text-slate-400 mr-2" />
        )}
        <span className="text-slate-200 text-sm flex-1">{node.name || 'root'}</span>
        <div className="flex items-center gap-4 text-xs text-slate-500">
          <span className="flex items-center gap-1">
            <Users className="w-3 h-3" />
            {node.subscriberCount}
          </span>
          <span className="flex items-center gap-1">
            <Mail className="w-3 h-3" />
            {node.messageCount.toLocaleString()}
          </span>
        </div>
      </motion.div>
      <AnimatePresence>
        {isExpanded && hasChildren && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
          >
            {node.children.map((child, index) => (
              <TreeNode
                key={`${child.path}-${index}`}
                node={child}
                level={level + 1}
                searchTerm={searchTerm}
                onSelect={onSelect}
              />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const ViewToggle: React.FC<{
  view: 'tree' | 'list';
  onChange: (view: 'tree' | 'list') => void;
}> = ({ view, onChange }) => (
  <div className="flex items-center bg-slate-800 rounded-lg p-1">
    <button
      onClick={() => onChange('tree')}
      className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm transition-colors ${
        view === 'tree'
          ? 'bg-amber-500 text-white'
          : 'text-slate-400 hover:text-white'
      }`}
    >
      <GitBranch className="w-4 h-4" />
      树形视图
    </button>
    <button
      onClick={() => onChange('list')}
      className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm transition-colors ${
        view === 'list'
          ? 'bg-amber-500 text-white'
          : 'text-slate-400 hover:text-white'
      }`}
    >
      <List className="w-4 h-4" />
      列表视图
    </button>
  </div>
);

const SubscriptionDetailModal: React.FC<{
  subscription: SubscriptionDetail;
  onClose: () => void;
}> = ({ subscription, onClose }) => {
  const qosLabels = ['最多一次', '至少一次', '恰好一次'];

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-lg"
    >
      <div className="flex items-center justify-between p-4 border-b border-slate-700">
        <h3 className="text-lg font-semibold text-white flex items-center gap-2">
          <Eye className="w-5 h-5 text-amber-400" />
          订阅详情
        </h3>
        <button onClick={onClose} className="text-slate-400 hover:text-white">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="p-4 space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-slate-900/50 rounded-lg p-3">
            <p className="text-slate-400 text-xs mb-1">客户端ID</p>
            <p className="text-white font-medium">{subscription.clientId}</p>
          </div>
          <div className="bg-slate-900/50 rounded-lg p-3">
            <p className="text-slate-400 text-xs mb-1">订阅ID</p>
            <p className="text-white font-medium font-mono text-sm">{subscription.id}</p>
          </div>
        </div>
        <div className="bg-slate-900/50 rounded-lg p-3">
          <p className="text-slate-400 text-xs mb-1">主题</p>
          <p className="text-amber-400 font-medium font-mono">{subscription.topic}</p>
        </div>
        <div className="bg-slate-900/50 rounded-lg p-3">
          <p className="text-slate-400 text-xs mb-1">QoS 等级</p>
          <div className="flex items-center gap-2">
            <span className={`px-2 py-1 rounded text-xs font-medium ${
              subscription.qos === 2 ? 'bg-purple-500/20 text-purple-400' :
              subscription.qos === 1 ? 'bg-blue-500/20 text-blue-400' :
              'bg-slate-500/20 text-slate-400'
            }`}>
              QoS {subscription.qos}
            </span>
            <span className="text-slate-300 text-sm">{qosLabels[subscription.qos]}</span>
          </div>
        </div>
        <div className="bg-slate-900/50 rounded-lg p-3">
          <p className="text-slate-400 text-xs mb-2 flex items-center gap-2">
            <Settings className="w-3 h-3" />
            订阅选项 (MQTT 5.0)
          </p>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-slate-300 text-sm">No Local</span>
              {subscription.options.noLocal ? (
                <span className="flex items-center gap-1 text-emerald-400 text-xs">
                  <Check className="w-3 h-3" /> 启用
                </span>
              ) : (
                <span className="flex items-center gap-1 text-slate-500 text-xs">
                  <XCircle className="w-3 h-3" /> 禁用
                </span>
              )}
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-300 text-sm">Retain As Published</span>
              {subscription.options.retainAsPublished ? (
                <span className="flex items-center gap-1 text-emerald-400 text-xs">
                  <Check className="w-3 h-3" /> 启用
                </span>
              ) : (
                <span className="flex items-center gap-1 text-slate-500 text-xs">
                  <XCircle className="w-3 h-3" /> 禁用
                </span>
              )}
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-300 text-sm">Retain Handling</span>
              <span className="text-slate-300 text-xs">
                {subscription.options.retainHandling === 0 ? '发送保留消息' :
                 subscription.options.retainHandling === 1 ? '如果未订阅则发送' : '不发送保留消息'}
              </span>
            </div>
          </div>
        </div>
        <div className="bg-slate-900/50 rounded-lg p-3">
          <p className="text-slate-400 text-xs mb-1">订阅时间</p>
          <p className="text-white text-sm">{new Date(subscription.subscribedAt).toLocaleString('zh-CN')}</p>
        </div>
      </div>
    </motion.div>
  );
};

const TopicTree: React.FC = () => {
  const [topicTree, setTopicTree] = useState<TopicNode | null>(null);
  const [subscriptions, setSubscriptions] = useState<SubscriptionDetail[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [clientIdFilter, setClientIdFilter] = useState('');
  const [view, setView] = useState<'tree' | 'list'>('tree');
  const [loading, setLoading] = useState(true);
  const [selectedNode, setSelectedNode] = useState<TopicNode | null>(null);
  const [selectedSubscription, setSelectedSubscription] = useState<SubscriptionDetail | null>(null);
  const [filterQoS, setFilterQoS] = useState<string>('all');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const [tree, subs] = await Promise.all([
      mockApi.getTopicTree(),
      mockApi.getSubscriptionDetails()
    ]);
    setTopicTree(tree);
    setSubscriptions(subs);
    setLoading(false);
  };

  const flattenTopics = (node: TopicNode, path: string = ''): TopicNode[] => {
    const currentPath = path ? `${path}/${node.name}` : node.name;
    const result: TopicNode[] = [];
    if (node.name) {
      result.push({ ...node, path: currentPath });
    }
    if (node.children) {
      node.children.forEach(child => {
        result.push(...flattenTopics(child, currentPath || ''));
      });
    }
    return result;
  };

  const flatTopics = useMemo(() => {
    if (!topicTree) return [];
    return flattenTopics(topicTree);
  }, [topicTree]);

  const filteredTopics = useMemo(() => {
    let topics = flatTopics;
    if (searchTerm) {
      topics = topics.filter(t =>
        t.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.path.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    return topics;
  }, [flatTopics, searchTerm]);

  const filteredSubscriptions = useMemo(() => {
    let subs = subscriptions;
    if (searchTerm) {
      subs = subs.filter(s =>
        s.topic.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    if (clientIdFilter) {
      subs = subs.filter(s =>
        s.clientId.toLowerCase().includes(clientIdFilter.toLowerCase())
      );
    }
    if (filterQoS !== 'all') {
      subs = subs.filter(s => s.qos === parseInt(filterQoS));
    }
    return subs;
  }, [subscriptions, searchTerm, clientIdFilter, filterQoS]);

  const expandAll = () => {
    const buttons = document.querySelectorAll('[data-tree-toggle]');
    buttons.forEach(btn => (btn as HTMLElement).click());
  };

  const collapseAll = () => {
    setSearchTerm('');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-3">
            <GitBranch className="w-7 h-7 text-amber-400" />
            订阅管理
          </h1>
          <p className="text-slate-400 mt-1">管理 MQTT 主题订阅结构</p>
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-400">
          <span>总主题数: {flatTopics.length}</span>
          <span className="mx-2">|</span>
          <span>总订阅数: {subscriptions.length}</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
          <input
            type="text"
            placeholder={view === 'tree' ? "搜索主题..." : "搜索主题..."}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>
        {view === 'list' && (
          <div className="relative flex-1 max-w-md">
            <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
            <input
              type="text"
              placeholder="搜索客户端ID..."
              value={clientIdFilter}
              onChange={(e) => setClientIdFilter(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-500"
            />
          </div>
        )}
        <ViewToggle view={view} onChange={setView} />
        {view === 'list' && (
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <select
              value={filterQoS}
              onChange={(e) => setFilterQoS(e.target.value)}
              className="pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white text-sm focus:outline-none focus:border-amber-500 appearance-none cursor-pointer"
            >
              <option value="all">所有 QoS</option>
              <option value="0">QoS 0</option>
              <option value="1">QoS 1</option>
              <option value="2">QoS 2</option>
            </select>
          </div>
        )}
        {view === 'tree' && (
          <>
            <button
              onClick={expandAll}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm transition-colors"
            >
              全部展开
            </button>
            <button
              onClick={collapseAll}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm transition-colors"
            >
              全部折叠
            </button>
          </>
        )}
      </div>

      {view === 'tree' ? (
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden">
          <div className="flex items-center px-4 py-3 bg-slate-800/50 border-b border-slate-800 text-xs text-slate-400">
            <span className="flex-1">主题路径</span>
            <span className="w-24 text-center">订阅数</span>
            <span className="w-24 text-center">消息数</span>
          </div>
          <div className="p-2">
            {topicTree && (
              <TreeNode
                node={topicTree}
                level={0}
                searchTerm={searchTerm}
                onSelect={setSelectedNode}
              />
            )}
          </div>
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
          <table className="w-full">
            <thead className="bg-slate-900/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">客户端ID</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">主题</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">QoS</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">订阅时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {filteredSubscriptions.map((sub) => (
                <motion.tr
                  key={sub.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="hover:bg-slate-700/50"
                >
                  <td className="px-4 py-3 text-slate-300">{sub.clientId}</td>
                  <td className="px-4 py-3 text-amber-400 font-mono text-sm">{sub.topic}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      sub.qos === 2 ? 'bg-purple-500/20 text-purple-400' :
                      sub.qos === 1 ? 'bg-blue-500/20 text-blue-400' :
                      'bg-slate-500/20 text-slate-400'
                    }`}>
                      QoS {sub.qos}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-400 text-sm">
                    {new Date(sub.subscribedAt).toLocaleString('zh-CN')}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setSelectedSubscription(sub)}
                      className="text-amber-400 hover:text-amber-300 text-sm flex items-center gap-1"
                    >
                      <Eye className="w-4 h-4" />
                      详情
                    </button>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
          {filteredSubscriptions.length === 0 && (
            <div className="text-center py-12 text-slate-500">
              <List className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>暂无匹配的订阅</p>
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
              className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-lg"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between p-4 border-b border-slate-700">
                <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                  <GitBranch className="w-5 h-5 text-amber-400" />
                  主题详情
                </h3>
                <button onClick={() => setSelectedNode(null)} className="text-slate-400 hover:text-white">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="p-4 space-y-4">
                <div className="bg-slate-900/50 rounded-lg p-3">
                  <p className="text-slate-400 text-xs mb-1">主题路径</p>
                  <p className="text-amber-400 font-medium font-mono">{selectedNode.path || '/'}</p>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-slate-900/50 rounded-lg p-3">
                    <p className="text-slate-400 text-xs mb-1 flex items-center gap-1">
                      <Users className="w-3 h-3" /> 订阅数
                    </p>
                    <p className="text-2xl font-bold text-white">{selectedNode.subscriberCount}</p>
                  </div>
                  <div className="bg-slate-900/50 rounded-lg p-3">
                    <p className="text-slate-400 text-xs mb-1 flex items-center gap-1">
                      <Mail className="w-3 h-3" /> 消息数
                    </p>
                    <p className="text-2xl font-bold text-white">{selectedNode.messageCount.toLocaleString()}</p>
                  </div>
                </div>
                {selectedNode.children && selectedNode.children.length > 0 && (
                  <div className="bg-slate-900/50 rounded-lg p-3">
                    <p className="text-slate-400 text-xs mb-2">子主题</p>
                    <div className="space-y-1">
                      {selectedNode.children.map((child, idx) => (
                        <div key={idx} className="flex items-center justify-between text-sm">
                          <span className="text-slate-300">{child.name}</span>
                          <span className="text-slate-500 text-xs">{child.subscriberCount} 订阅</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {selectedSubscription && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setSelectedSubscription(null)}
          >
            <SubscriptionDetailModal
              subscription={selectedSubscription}
              onClose={() => setSelectedSubscription(null)}
            />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default TopicTree;
