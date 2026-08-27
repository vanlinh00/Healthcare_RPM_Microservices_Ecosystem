import React from 'react';
import { Activity, ShieldCheck, Server, Database, Radio, Layers, Code2, Bell, Cpu } from 'lucide-react';

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  notificationCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, setActiveTab, notificationCount }) => {
  const navItems = [
    { id: 'topology', label: 'Ecosystem & Topology', icon: Server },
    { id: 'saga', label: 'Saga & Distributed Lock', icon: Layers },
    { id: 'outbox', label: 'Outbox & Kafka Bus', icon: Database },
    { id: 'telemetry', label: 'RPM Live Telemetry', icon: Activity },
    { id: 'search', label: 'Elasticsearch Search', icon: Cpu },
    { id: 'iam', label: 'Keycloak 24 IAM & 2FA', icon: ShieldCheck },
    { id: 'dispatch', label: 'Care Dispatch & POD', icon: Radio },
    { id: 'codebase', label: 'Code & IaC Explorer', icon: Code2 },
  ];

  return (
    <header className="bg-slate-900 border-b border-slate-800 text-slate-100 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-teal-500/10 border border-teal-500/30 flex items-center justify-center text-teal-400">
              <Activity className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-white tracking-tight">Healthcare & RPM Platform</span>
                <span className="text-[10px] uppercase font-bold tracking-widest px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/30 text-emerald-400">
                  Java 21 / Spring 3.4
                </span>
                <span className="text-[10px] uppercase font-bold tracking-widest px-2 py-0.5 rounded bg-blue-500/10 border border-blue-500/30 text-blue-400">
                  HIPAA Ready
                </span>
              </div>
              <p className="text-xs text-slate-400">Enterprise Microservices Ecosystem • Keycloak IAM • Kafka Outbox • Redisson</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <div className="hidden lg:flex items-center gap-1 text-xs text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-md border border-slate-700">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
              <span className="font-medium text-emerald-300">8/8 Microservices UP</span>
              <span className="text-slate-500 mx-1">•</span>
              <span>Eureka Discovery Active</span>
            </div>
          </div>
        </div>

        {/* Tab Navigation */}
        <nav className="flex space-x-1 overflow-x-auto no-scrollbar py-2 border-t border-slate-800/60">
          {navItems.map(item => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                id={`tab-${item.id}`}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3.5 py-1.5 text-xs font-medium rounded-md whitespace-nowrap transition-all ${
                  isActive
                    ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-teal-400' : 'text-slate-400'}`} />
                {item.label}
              </button>
            );
          })}
        </nav>
      </div>
    </header>
  );
};
