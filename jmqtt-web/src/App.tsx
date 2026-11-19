import { useState, useEffect } from 'react';
import { HashRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Dashboard from './pages/Dashboard';
import UserManagement from './pages/UserManagement';
import ClientManagement from './pages/ClientManagement';
import ClientDetail from './pages/ClientDetail';
import TopicTree from './pages/TopicTree';
import RetainedMessages from './pages/RetainedMessages';
import OfflineMessages from './pages/OfflineMessages';
import ClusterNodes from './pages/ClusterNodes';
import MqttConfig from './pages/MqttConfig';
import DataBridge from './pages/DataBridge';
import DelayedMessages from './pages/DelayedMessages';
import BlacklistManagement from './pages/BlacklistManagement';
import Login from './pages/Login';

const BROKER_NAME = 'JMqtt';

function AppContent() {
  const [selectedVersion, setSelectedVersion] = useState('5.0');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const loggedIn = localStorage.getItem('jmqtt_logged_in') === 'true';
    setIsLoggedIn(loggedIn);
  }, []);

  const handleLogin = (username: string, password: string): boolean => {
    if (username === 'admin' && password === 'admin') {
      localStorage.setItem('jmqtt_logged_in', 'true');
      localStorage.setItem('jmqtt_username', username);
      setIsLoggedIn(true);
      navigate('/monitor/dashboard');
      return true;
    }
    return false;
  };

  const handleLogout = () => {
    localStorage.removeItem('jmqtt_logged_in');
    localStorage.removeItem('jmqtt_username');
    setIsLoggedIn(false);
    navigate('/login');
  };

  if (!isLoggedIn) {
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={handleLogin} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex">
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
        brokerName={BROKER_NAME}
      />
      <div className={`flex-1 flex flex-col h-screen transition-all duration-300 ${sidebarCollapsed ? 'ml-16' : 'ml-64'}`}>
        <Header
          selectedVersion={selectedVersion}
          onVersionChange={setSelectedVersion}
          brokerName={BROKER_NAME}
          onLogout={handleLogout}
        />
        <main className="flex-1 overflow-y-auto p-6">
          <Routes>
            <Route path="/" element={<Navigate to="/monitor/dashboard" replace />} />
            <Route path="/login" element={<Navigate to="/monitor/dashboard" replace />} />
            <Route path="/monitor/dashboard" element={<Dashboard />} />
            <Route path="/monitor/clients" element={<ClientManagement />} />
            <Route path="/monitor/clients/:clientId" element={<ClientDetail />} />
            <Route path="/monitor/topics" element={<TopicTree />} />
            <Route path="/monitor/retained" element={<RetainedMessages />} />
            <Route path="/monitor/offline" element={<OfflineMessages />} />
            <Route path="/monitor/delayed" element={<DelayedMessages />} />
            <Route path="/monitor/nodes" element={<ClusterNodes />} />
            <Route path="/bridge" element={<DataBridge />} />
            <Route path="/settings/users" element={<UserManagement />} />
            <Route path="/settings/mqtt" element={<MqttConfig />} />
            <Route path="/settings/blacklist" element={<BlacklistManagement />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function App() {
  return (
    <HashRouter>
      <AppContent />
    </HashRouter>
  );
}

export default App;
