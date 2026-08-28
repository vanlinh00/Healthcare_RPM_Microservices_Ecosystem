import React, { useState, useEffect } from 'react';
import { Server, CheckCircle2, RefreshCw, Cpu, Database, Shield, Zap, ArrowRight, Activity, Terminal } from 'lucide-react';
import { MicroserviceInfo } from '../types';

export const ServiceTopologyTab: React.FC = () => {
  const [services, setServices] = useState<MicroserviceInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedService, setSelectedService] = useState<MicroserviceInfo | null>(null);

  const fetchServices = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/microservices');
      const data = await res.json();
      setServices(data);
      if (!selectedService && data.length > 0) {
        setSelectedService(data[0]);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServices();
  }, []);

  return (
    <div className="space-y-6">
      {/* Top Banner Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Eureka Service Registry</span>
            <Server className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 mt-2">8 Services</p>
          <p className="text-xs text-emerald-400 mt-1 flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            19 Replicas Healthy & Registered
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Spring Cloud Gateway</span>
            <Zap className="w-4 h-4 text-teal-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 mt-2">1,450 req/s</p>
          <p className="text-xs text-slate-400 mt-1">Reactive Netty • JWT Header Relay</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Apache Kafka & Outbox</span>
            <Database className="w-4 h-4 text-purple-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 mt-2">KRaft Mode</p>
          <p className="text-xs text-purple-400 mt-1">Transactional Outbox Pattern Active</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400">Keycloak 24 IAM</span>
            <Shield className="w-4 h-4 text-blue-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 mt-2">6 RBAC Roles</p>
          <p className="text-xs text-blue-400 mt-1">OIDC / TOTP 2FA Enforced</p>
        </div>
      </div>

      {/* Visual Architectural Map */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-base font-semibold text-slate-100">Enterprise Microservices Architecture Topology</h3>
            <p className="text-xs text-slate-400">1:1 Domain Mapping with Spring Cloud, Keycloak IAM, Kafka KRaft, and PostgreSQL</p>
          </div>
          <button
            onClick={fetchServices}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-md transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh Heartbeats
          </button>
        </div>

        {/* Microservices Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {services.map(srv => {
            const isSelected = selectedService?.id === srv.id;
            return (
              <div
                key={srv.id}
                onClick={() => setSelectedService(srv)}
                className={`p-4 rounded-lg border transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-slate-800/90 border-teal-500/60 shadow-lg ring-1 ring-teal-500/30'
                    : 'bg-slate-800/40 border-slate-700/60 hover:bg-slate-800/70 hover:border-slate-600'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                    <span className="text-xs font-mono font-bold text-slate-200">{srv.id}</span>
                  </div>
                  <span className="text-[10px] font-mono bg-slate-700 text-slate-300 px-1.5 py-0.5 rounded">
                    Port {srv.port}
                  </span>
                </div>
                <h4 className="text-sm font-semibold text-slate-100 mt-2">{srv.name}</h4>
                <p className="text-xs text-slate-400 mt-1 line-clamp-2">{srv.role}</p>

                <div className="mt-3 pt-3 border-t border-slate-700/50 flex items-center justify-between text-[11px] text-slate-400">
                  <span>{srv.tech}</span>
                  <span className="font-mono text-teal-400 font-semibold">{srv.instances} Pods</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Selected Microservice Deep-Dive Inspection Panel */}
      {selectedService && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-teal-500/10 border border-teal-500/30 rounded-lg text-teal-400">
                <Cpu className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-base font-bold text-slate-100">{selectedService.name} (`{selectedService.id}`)</h4>
                <p className="text-xs text-slate-400 font-mono">Package: com.healthcare.{selectedService.id.replace(/-/g, '')} • Java 17 • Spring Boot 3.4</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-1 text-xs rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-medium">
                Eureka Heartbeat: {selectedService.health}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
            <div className="space-y-4">
              <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Runtime Configuration</h5>
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 text-xs font-mono text-slate-300 space-y-1.5">
                <div><span className="text-teal-400">server.port:</span> {selectedService.port}</div>
                <div><span className="text-teal-400">spring.application.name:</span> {selectedService.id}</div>
                <div><span className="text-teal-400">replicas.active:</span> {selectedService.instances} Instances</div>
                <div><span className="text-teal-400">metrics.traffic:</span> {selectedService.traffic}</div>
              </div>
            </div>

            <div className="space-y-4">
              <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Architectural Role</h5>
              <p className="text-xs text-slate-300 bg-slate-950 p-3 rounded-lg border border-slate-800 leading-relaxed">
                {selectedService.role}
              </p>
            </div>

            <div className="space-y-4">
              <h5 className="text-xs font-semibold uppercase tracking-wider text-slate-400">Infrastructure Dependencies</h5>
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 text-xs text-slate-300 space-y-2">
                <div className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
                  <span>PostgreSQL multi-db with Liquibase change sets</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-purple-400"></span>
                  <span>Kafka Topic Publisher &amp; Consumer</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-red-400"></span>
                  <span>Redis / Redisson Distributed Locks</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
