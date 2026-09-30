import { NavLink, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useState, useRef, useEffect } from 'react';
import {
  LayoutDashboard,
  Settings,
  Users,
  Network,
  Bookmark,
  Mail,
  Server,
  ChevronDown,
  Activity,
  GitBranch,
  Radio,
  PanelLeftClose,
  PanelLeftOpen,
  Link2,
  Clock,
  Shield
} from 'lucide-react';

interface SubMenuItem {
  path: string;
  label: string;
  icon: React.ElementType;
}

const dashboardItem: SubMenuItem = { path: '/monitor/dashboard', label: 'Dashboard', icon: LayoutDashboard };

const monitorItems: SubMenuItem[] = [
  { path: '/monitor/clients', label: '客户端管理', icon: Network },
  { path: '/monitor/topics', label: '订阅管理', icon: GitBranch },
  { path: '/monitor/retained', label: '保留消息', icon: Bookmark },
  { path: '/monitor/offline', label: '离线消息', icon: Mail },
  { path: '/monitor/delayed', label: '延时消息', icon: Clock }
];

const clusterItem: SubMenuItem = { path: '/monitor/nodes', label: '集群管理', icon: Server };
const bridgeItem: SubMenuItem = { path: '/bridge', label: '数据桥接', icon: Link2 };

const settingsItems: SubMenuItem[] = [
  { path: '/settings/users', label: '用户管理', icon: Users },
  { path: '/settings/mqtt', label: 'MQTT 配置', icon: Radio },
  { path: '/settings/blacklist', label: '黑名单管理', icon: Shield }
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  brokerName?: string;
}

export default function Sidebar({ collapsed, onToggle, brokerName = 'MQTT Broker' }: SidebarProps) {
  const location = useLocation();
  const [expandedMenus, setExpandedMenus] = useState<string[]>(['monitor', 'settings']);
  const [hoveredMenu, setHoveredMenu] = useState<string | null>(null);
  const navRef = useRef<HTMLElement>(null);
  const scrollPositionRef = useRef<number>(0);

  useEffect(() => {
    const nav = navRef.current;
    if (nav) {
      nav.scrollTop = scrollPositionRef.current;
    }
  });

  const handleNavClick = () => {
    const nav = navRef.current;
    if (nav) {
      scrollPositionRef.current = nav.scrollTop;
    }
  };

  const isMenuActive = (items: SubMenuItem[]) => {
    return items.some(item => location.pathname === item.path || location.pathname.startsWith(item.path + '/'));
  };

  const isItemActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  const toggleMenu = (menu: string) => {
    setExpandedMenus(prev =>
      prev.includes(menu) ? prev.filter(m => m !== menu) : [...prev, menu]
    );
  };

  const CollapsedMenuGroup: React.FC<{
    label: string;
    icon: React.ElementType;
    items: SubMenuItem[];
    id: string;
  }> = ({ label, icon: Icon, items, id }) => {
    const isActive = isMenuActive(items);
    const isHovered = hoveredMenu === id;
    const buttonRef = useRef<HTMLButtonElement>(null);
    const popupRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (
          buttonRef.current &&
          !buttonRef.current.contains(event.target as Node) &&
          popupRef.current &&
          !popupRef.current.contains(event.target as Node)
        ) {
          setHoveredMenu(null);
        }
      };

      if (isHovered) {
        document.addEventListener('mousedown', handleClickOutside);
      }

      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }, [isHovered]);

    return (
      <div className="mb-2 relative">
        <div className="group">
          <button
            ref={buttonRef}
            onMouseEnter={() => setHoveredMenu(id)}
            className={`w-full flex items-center justify-center p-2 rounded-lg transition-all duration-200 ${
              isActive
                ? 'bg-slate-800 text-white'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
            }`}
          >
            <Icon className="w-5 h-5" />
          </button>

          <AnimatePresence>
            {isHovered && (
              <motion.div
                ref={popupRef}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                transition={{ duration: 0.15 }}
                className="fixed left-[72px] bg-slate-800 rounded-xl shadow-2xl border border-slate-700 py-2 z-[100] min-w-[180px]"
                style={{ top: 'auto' }}
              >
                <div className="px-4 py-2 border-b border-slate-700 mb-2">
                  <span className="text-white font-medium text-sm">{label}</span>
                </div>
                <ul className="space-y-1 px-2">
                  {items.map((item) => {
                    const itemActive = isItemActive(item.path);
                    return (
                      <li key={item.path}>
                        <NavLink
                          to={item.path}
                          onClick={() => setHoveredMenu(null)}
                          className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-all duration-200 ${
                            itemActive
                              ? 'bg-blue-600 text-white'
                              : 'text-slate-300 hover:bg-slate-700 hover:text-white'
                          }`}
                        >
                          <item.icon className="w-4 h-4" />
                          <span className="text-sm whitespace-nowrap">{item.label}</span>
                          {itemActive && (
                            <div className="ml-auto w-1.5 h-1.5 bg-cyan-400 rounded-full" />
                          )}
                        </NavLink>
                      </li>
                    );
                  })}
                </ul>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    );
  };

const MenuGroupComponent: React.FC<{
  label: string;
  icon: React.ElementType;
  items: SubMenuItem[];
  id: string;
}> = ({ label, icon: Icon, items, id }) => {
  const isActive = isMenuActive(items);

  return (
    <div className="mb-2">
      <button
        onClick={() => toggleMenu(id)}
        className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
          isActive
            ? 'bg-slate-800 text-white'
            : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
        }`}
      >
        <motion.div whileHover={{ scale: 1.1 }} whileTap={{ scale: 0.95 }}>
          <Icon className="w-5 h-5" />
        </motion.div>
        <span className="font-medium flex-1">{label}</span>
        <motion.div
          animate={{ rotate: expandedMenus.includes(id) ? 180 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <ChevronDown className="w-4 h-4" />
        </motion.div>
      </button>
      <AnimatePresence initial={false}>
        {expandedMenus.includes(id) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <ul className="space-y-1 mt-1 ml-4 pl-4 border-l border-slate-700">
              {items.map((item) => {
                const itemActive = isItemActive(item.path);
                return (
                  <li key={item.path}>
                    <NavLink
                      to={item.path}
                      className={`flex items-center gap-3 px-4 py-2 rounded-lg transition-all duration-200 ${
                        itemActive
                          ? 'bg-blue-600 text-white'
                          : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                      }`}
                    >
                      <item.icon className="w-4 h-4" />
                      <span className="text-sm">{item.label}</span>
                      {itemActive && (
                        <motion.div
                          layoutId="activeIndicator"
                          className="ml-auto w-1.5 h-1.5 bg-cyan-400 rounded-full"
                        />
                      )}
                    </NavLink>
                  </li>
                );
              })}
            </ul>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

  return (
    <aside className={`${collapsed ? 'w-16' : 'w-64'} bg-slate-900 border-r border-slate-800 flex flex-col h-screen fixed left-0 top-0 z-40 transition-all duration-300`}>
      <div className={`p-6 border-b border-slate-800 flex ${collapsed ? 'justify-center' : 'justify-between'} items-center`}>
        {!collapsed && (
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg tracking-tight">JMQ</span>
            </div>
            <div>
              <h1 className="text-white font-bold text-lg">{brokerName}</h1>
              <p className="text-slate-400 text-xs">管理控制台</p>
            </div>
          </div>
        )}
        {collapsed && (
          <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-lg tracking-tight">JMQ</span>
          </div>
        )}
        <button
          onClick={onToggle}
          className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
        >
          {collapsed ? <PanelLeftOpen className="w-5 h-5" /> : <PanelLeftClose className="w-5 h-5" />}
        </button>
      </div>

      <nav ref={navRef} className="flex-1 p-4 overflow-y-auto" onClick={handleNavClick}>
        {collapsed ? (
          <>
            <div className="mb-2">
              <NavLink
                to={dashboardItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center justify-center p-2 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <LayoutDashboard className="w-5 h-5" />
              </NavLink>
            </div>
            <CollapsedMenuGroup
              id="monitor"
              label="监控"
              icon={Activity}
              items={monitorItems}
            />
            <div className="mb-2">
              <NavLink
                to={bridgeItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center justify-center p-2 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <Link2 className="w-5 h-5" />
              </NavLink>
            </div>
            <div className="mb-2">
              <NavLink
                to={clusterItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center justify-center p-2 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <Server className="w-5 h-5" />
              </NavLink>
            </div>
            <CollapsedMenuGroup
              id="settings"
              label="系统设置"
              icon={Settings}
              items={settingsItems}
            />
          </>
        ) : (
          <>
            <div className="mb-2">
              <NavLink
                to={dashboardItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <LayoutDashboard className="w-5 h-5" />
                <span className="font-medium">{dashboardItem.label}</span>
              </NavLink>
            </div>
            <MenuGroupComponent
              id="monitor"
              label="监控"
              icon={Activity}
              items={monitorItems}
            />
            <div className="mb-2">
              <NavLink
                to={bridgeItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <Link2 className="w-5 h-5" />
                <span className="font-medium">{bridgeItem.label}</span>
              </NavLink>
            </div>
            <div className="mb-2">
              <NavLink
                to={clusterItem.path}
                className={({ isActive }) =>
                  `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                  }`
                }
              >
                <Server className="w-5 h-5" />
                <span className="font-medium">{clusterItem.label}</span>
              </NavLink>
            </div>
            <MenuGroupComponent
              id="settings"
              label="系统设置"
              icon={Settings}
              items={settingsItems}
            />
          </>
        )}
      </nav>

      {!collapsed && (
        <div className="p-4 border-t border-slate-800">
          <div className="bg-slate-800 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
              <span className="text-slate-300 text-sm font-medium">服务状态</span>
            </div>
            <p className="text-slate-400 text-xs">MQTT Broker 运行正常</p>
            <p className="text-slate-500 text-xs mt-1">版本: 5.0.26</p>
          </div>
        </div>
      )}
    </aside>
  );
}
