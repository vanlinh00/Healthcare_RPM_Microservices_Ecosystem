import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { ServiceTopologyTab } from './components/ServiceTopologyTab';
import { SagaOrchestratorTab } from './components/SagaOrchestratorTab';
import { OutboxKafkaTab } from './components/OutboxKafkaTab';
import { RpmTelemetryTab } from './components/RpmTelemetryTab';
import { ElasticsearchTab } from './components/ElasticsearchTab';
import { KeycloakIamTab } from './components/KeycloakIamTab';
import { CareDispatchTab } from './components/CareDispatchTab';
import { CodebaseExplorerTab } from './components/CodebaseExplorerTab';

export function App() {
  const [activeTab, setActiveTab] = useState<string>('topology');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-teal-500 selection:text-white">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} notificationCount={2} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'topology' && <ServiceTopologyTab />}
        {activeTab === 'saga' && <SagaOrchestratorTab />}
        {activeTab === 'outbox' && <OutboxKafkaTab />}
        {activeTab === 'telemetry' && <RpmTelemetryTab />}
        {activeTab === 'search' && <ElasticsearchTab />}
        {activeTab === 'iam' && <KeycloakIamTab />}
        {activeTab === 'dispatch' && <CareDispatchTab />}
        {activeTab === 'codebase' && <CodebaseExplorerTab />}
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-950 py-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Healthcare &amp; RPM Microservices Ecosystem • Java 17 / Spring Boot 3.4 / Keycloak 24</span>
          <span className="text-[11px] font-mono text-slate-400">PostgreSQL (Liquibase) • Redisson • Apache Kafka KRaft • Elasticsearch 8.17</span>
        </div>
      </footer>
    </div>
  );
}

export default App;
