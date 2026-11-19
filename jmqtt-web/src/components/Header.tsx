import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Wifi, ChevronDown, User, Settings, LogOut, Shield } from 'lucide-react';

interface HeaderProps {
  selectedVersion: string;
  onVersionChange: (version: string) => void;
  brokerName: string;
  onLogout: () => void;
}

export default function Header({ selectedVersion, onVersionChange, brokerName, onLogout }: HeaderProps) {
  const [isUserOpen, setIsUserOpen] = useState(false);
  const [isOnline] = useState(true);
  const username = localStorage.getItem('jmqtt_username') || 'admin';
  const nodeRole = 'Keeper';

  return (
    <header className="h-16 bg-slate-900 border-b border-slate-800 flex items-center justify-between px-6">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-600 rounded-xl flex items-center justify-center">
          <Shield className="w-6 h-6 text-white" />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-white font-bold text-lg tracking-tight">JMQ</span>
          <span className="px-2 py-0.5 bg-violet-500/20 text-violet-400 text-xs rounded-full font-medium border border-violet-500/30">
            {nodeRole}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 rounded-lg">
          <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-red-400'}`} />
          <span className="text-sm text-slate-300">{isOnline ? '在线' : '离线'}</span>
        </div>

        <div className="relative">
          <button
            onClick={() => setIsUserOpen(!isUserOpen)}
            className="flex items-center gap-3 hover:bg-slate-800 rounded-lg p-2 transition-colors"
          >
            <div className="w-8 h-8 bg-gradient-to-br from-violet-500 to-purple-600 rounded-full flex items-center justify-center">
              <User className="w-4 h-4 text-white" />
            </div>
            <span className="text-sm text-slate-300 hidden sm:block">{username}</span>
            <ChevronDown className="w-4 h-4 text-slate-400" />
          </button>
          <AnimatePresence>
            {isUserOpen && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="absolute top-full right-0 mt-2 w-40 bg-slate-800 rounded-lg shadow-xl border border-slate-700 overflow-hidden z-50"
              >
                <div className="px-4 py-2 border-b border-slate-700">
                  <p className="text-sm text-white font-medium">{username}</p>
                  <p className="text-xs text-slate-400">管理员</p>
                </div>
                <button className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-slate-300 hover:bg-slate-700 transition-colors">
                  <Settings className="w-4 h-4" />
                  <span>设置</span>
                </button>
                <button
                  onClick={onLogout}
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-400 hover:bg-slate-700 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  <span>退出</span>
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}
