import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { mockApi } from '../services/mockApi';
import type { MQTTUser, ACLRule } from '../types/mqtt';
import { Users, Trash2, Edit3, Shield, Plus, X, AlertCircle } from 'lucide-react';

export default function UserManagement() {
  const [users, setUsers] = useState<MQTTUser[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState<MQTTUser | null>(null);
  const [formData, setFormData] = useState({ username: '', password: '', isSuperuser: false });
  const [showAclModal, setShowAclModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<MQTTUser | null>(null);
  const [aclForm, setAclForm] = useState({ topic: '', permission: 'allow' as const, action: 'pubsub' as const, qos: [0, 1, 2] });
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [userToDelete, setUserToDelete] = useState<string | null>(null);
  const [showAclDeleteConfirm, setShowAclDeleteConfirm] = useState(false);
  const [aclRuleToDelete, setAclRuleToDelete] = useState<string | null>(null);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    const data = await mockApi.getUsers();
    setUsers(data);
  };

  const filteredUsers = users.filter(u => u.username.toLowerCase().includes(searchTerm.toLowerCase()));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingUser) {
      await mockApi.updateUser(editingUser.id, formData);
    } else {
      await mockApi.createUser(formData);
    }
    setShowModal(false);
    setEditingUser(null);
    setFormData({ username: '', password: '', isSuperuser: false });
    loadUsers();
  };

  const handleDelete = async (id: string) => {
    setUserToDelete(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    if (userToDelete) {
      await mockApi.deleteUser(userToDelete);
      loadUsers();
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    }
  };

  const openEdit = (user: MQTTUser) => {
    setEditingUser(user);
    setFormData({ username: user.username, password: '', isSuperuser: user.isSuperuser });
    setShowModal(true);
  };

  const openAcl = (user: MQTTUser) => {
    setSelectedUser(user);
    setShowAclModal(true);
  };

  const addAclRule = () => {
    if (selectedUser) {
      const newRule: ACLRule = {
        id: Math.random().toString(36).substr(2, 9),
        userId: selectedUser.id,
        ...aclForm
      };
      const updatedRules = [...(selectedUser.aclRules || []), newRule];
      mockApi.updateUser(selectedUser.id, { aclRules: updatedRules });
      setSelectedUser({ ...selectedUser, aclRules: updatedRules });
      setAclForm({ topic: '', permission: 'allow', action: 'pubsub', qos: [0, 1, 2] });
    }
  };

  const deleteAclRule = (ruleId: string) => {
    setAclRuleToDelete(ruleId);
    setShowAclDeleteConfirm(true);
  };

  const confirmAclDelete = () => {
    if (selectedUser && aclRuleToDelete) {
      const updatedRules = selectedUser.aclRules?.filter(r => r.id !== aclRuleToDelete) || [];
      mockApi.updateUser(selectedUser.id, { aclRules: updatedRules });
      setSelectedUser({ ...selectedUser, aclRules: updatedRules });
      setShowAclDeleteConfirm(false);
      setAclRuleToDelete(null);
    }
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Users className="w-6 h-6 text-blue-400" />
          <h1 className="text-2xl font-bold text-white">用户管理</h1>
        </div>
        <button
          onClick={() => { setEditingUser(null); setFormData({ username: '', password: '', isSuperuser: false }); setShowModal(true); }}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
        >
          <Plus className="w-4 h-4" />
          添加用户
        </button>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="搜索用户名..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-64 px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-blue-500"
        />
      </div>

      <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
        <table className="w-full">
          <thead className="bg-slate-900">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">用户名</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">超级用户</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">ACL规则</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-slate-400">创建时间</th>
              <th className="px-4 py-3 text-center text-sm font-medium text-slate-400">操作</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map(user => (
              <tr key={user.id} className="border-t border-slate-700 hover:bg-slate-700/50">
                <td className="px-4 py-3 text-white">{user.username}</td>
                <td className="px-4 py-3">
                  {user.isSuperuser ? (
                    <span className="text-emerald-400">是</span>
                  ) : (
                    <span className="text-slate-500">否</span>
                  )}
                </td>
                <td className="px-4 py-3 text-slate-300">{user.aclRules?.length || 0} 条</td>
                <td className="px-4 py-3 text-slate-400 text-sm">{new Date(user.createdAt).toLocaleString()}</td>
                <td className="px-4 py-3 text-center">
                  <div className="flex items-center justify-center gap-2">
                    <button
                      onClick={() => openAcl(user)}
                      className="p-2 text-blue-400 hover:bg-blue-500/20 rounded-lg transition-colors"
                      title="ACL规则"
                    >
                      <Shield className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => openEdit(user)}
                      className="p-2 text-amber-400 hover:bg-amber-500/20 rounded-lg transition-colors"
                      title="编辑"
                    >
                      <Edit3 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(user.id)}
                      className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                      title="删除"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filteredUsers.length === 0 && (
          <div className="text-center py-12 text-slate-500">
            <Users className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>暂无用户</p>
          </div>
        )}
      </div>

      <AnimatePresence>
        {showModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <motion.div initial={{ scale: 0.9 }} animate={{ scale: 1 }} exit={{ scale: 0.9 }} className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md">
              <div className="flex items-center justify-between p-4 border-b border-slate-700">
                <h2 className="text-xl font-bold text-white">{editingUser ? '编辑用户' : '添加用户'}</h2>
                <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <form onSubmit={handleSubmit} className="p-4 space-y-4">
                <div>
                  <label className="block text-slate-300 mb-2 text-sm">用户名</label>
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                    required
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-2 text-sm">密码 {editingUser && '(留空不修改)'}</label>
                  <input
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white focus:outline-none focus:border-blue-500"
                    required={!editingUser}
                  />
                </div>
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    checked={formData.isSuperuser}
                    onChange={(e) => setFormData({ ...formData, isSuperuser: e.target.checked })}
                    className="mr-2 w-4 h-4 rounded border-slate-600 bg-slate-700 text-blue-500 focus:ring-blue-500"
                  />
                  <label className="text-slate-300 text-sm">超级用户</label>
                </div>
                <div className="flex justify-end gap-3 pt-2">
                  <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-300 hover:text-white transition-colors">取消</button>
                  <button type="submit" className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors">保存</button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showAclModal && selectedUser && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <motion.div initial={{ scale: 0.9 }} animate={{ scale: 1 }} exit={{ scale: 0.9 }} className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-2xl max-h-[80vh] overflow-hidden">
              <div className="flex items-center justify-between p-4 border-b border-slate-700">
                <h2 className="text-xl font-bold text-white">ACL规则管理 - {selectedUser.username}</h2>
                <button onClick={() => { setShowAclModal(false); loadUsers(); }} className="text-slate-400 hover:text-white">
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="p-4 space-y-4 overflow-auto max-h-[60vh]">
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="Topic"
                    value={aclForm.topic}
                    onChange={(e) => setAclForm({ ...aclForm, topic: e.target.value })}
                    className="flex-1 px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500"
                  />
                  <select
                    value={aclForm.permission}
                    onChange={(e) => setAclForm({ ...aclForm, permission: e.target.value as 'allow' | 'deny' })}
                    className="px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500"
                  >
                    <option value="allow">允许</option>
                    <option value="deny">拒绝</option>
                  </select>
                  <select
                    value={aclForm.action}
                    onChange={(e) => setAclForm({ ...aclForm, action: e.target.value as 'publish' | 'subscribe' | 'pubsub' })}
                    className="px-3 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white text-sm focus:outline-none focus:border-blue-500"
                  >
                    <option value="publish">发布</option>
                    <option value="subscribe">订阅</option>
                    <option value="pubsub">发布/订阅</option>
                  </select>
                  <button onClick={addAclRule} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm transition-colors">添加</button>
                </div>
                <div className="space-y-2">
                  {selectedUser.aclRules?.map(rule => (
                    <div key={rule.id} className="flex items-center justify-between bg-slate-700/50 rounded-lg px-3 py-2">
                      <div className="text-sm flex items-center gap-3">
                        <span className="text-white font-mono">{rule.topic}</span>
                        <span className={`px-2 py-0.5 rounded text-xs ${rule.permission === 'allow' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-red-500/20 text-red-400'}`}>
                          {rule.permission === 'allow' ? '允许' : '拒绝'}
                        </span>
                        <span className="text-slate-400">{rule.action}</span>
                      </div>
                      <button
                        onClick={() => deleteAclRule(rule.id)}
                        className="p-1.5 text-red-400 hover:bg-red-500/20 rounded transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                  {(!selectedUser.aclRules || selectedUser.aclRules.length === 0) && (
                    <p className="text-slate-500 text-center py-4">暂无ACL规则</p>
                  )}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showDeleteConfirm && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6">
              <div className="flex items-center gap-3 mb-4">
                <AlertCircle className="w-6 h-6 text-red-400" />
                <h3 className="text-lg font-semibold text-white">确认删除</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要删除此用户吗？此操作不可恢复。</p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowDeleteConfirm(false); setUserToDelete(null); }}
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

      <AnimatePresence>
        {showAclDeleteConfirm && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-md p-6">
              <div className="flex items-center gap-3 mb-4">
                <AlertCircle className="w-6 h-6 text-red-400" />
                <h3 className="text-lg font-semibold text-white">确认删除</h3>
              </div>
              <p className="text-slate-300 mb-6">确定要删除此ACL规则吗？此操作不可恢复。</p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => { setShowAclDeleteConfirm(false); setAclRuleToDelete(null); }}
                  className="px-4 py-2 text-slate-300 hover:text-white transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={confirmAclDelete}
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
