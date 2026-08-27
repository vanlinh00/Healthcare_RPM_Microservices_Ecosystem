import React, { useState } from 'react';
import { Layers, Play, Lock, ShieldAlert, CheckCircle2, XCircle, RotateCcw, AlertTriangle, ArrowRight, ShieldCheck } from 'lucide-react';
import { SagaExecutionResponse } from '../types';

export const SagaOrchestratorTab: React.FC = () => {
  const [patientId, setPatientId] = useState('PAT-101 (Eleanor Vance)');
  const [doctorId, setDoctorId] = useState('DOC-204');
  const [doctorName, setDoctorName] = useState('Dr. Emily Vance (Cardiologist)');
  const [consultationType, setConsultationType] = useState('SPECIALIST');
  const [pricingStrategy, setPricingStrategy] = useState('SpecialistPricingStrategy');
  const [baseFee, setBaseFee] = useState(150);
  const [simulateFailure, setSimulateFailure] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sagaResult, setSagaResult] = useState<SagaExecutionResponse | null>(null);

  // Concurrency lock test state
  const [concurrencyLoading, setConcurrencyLoading] = useState(false);
  const [useRedissonLock, setUseRedissonLock] = useState(true);
  const [concurrencyResult, setConcurrencyResult] = useState<any | null>(null);

  const runSaga = async () => {
    setLoading(true);
    setSagaResult(null);
    try {
      const res = await fetch('/api/saga/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientId: patientId.split(' ')[0],
          doctorId,
          doctorName,
          consultationType,
          baseFee: Number(baseFee),
          pricingStrategy,
          insuranceCoverage: 0.8,
          simulateInsuranceFailure: simulateFailure,
        }),
      });
      const data = await res.json();
      setSagaResult(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const runConcurrencyTest = async () => {
    setConcurrencyLoading(true);
    setConcurrencyResult(null);
    try {
      const res = await fetch('/api/concurrency/test-lock', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          doctorId: 'DOC-204',
          slot: '2026-08-28 09:00 AM',
          concurrentClients: 5,
          useRedissonLock,
        }),
      });
      const data = await res.json();
      setConcurrencyResult(data);
    } catch (e) {
      console.error(e);
    } finally {
      setConcurrencyLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Overview Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/30 rounded-xl text-indigo-400">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100">Saga Orchestrator &amp; Distributed Concurrency Control</h3>
            <p className="text-xs text-slate-400">
              Orchestrates multi-service transactions: Redisson Distributed Lock (`RLock`) &rarr; Insurance &amp; Strategy Copay &rarr; Care Resource Lock &rarr; Transactional Outbox &rarr; Automated Compensation Rollbacks.
            </p>
          </div>
        </div>
      </div>

      {/* Section 1: Live Saga Orchestrator Visualizer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Controls */}
        <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <Play className="w-4 h-4 text-teal-400" />
            Book Consultation Saga Runner
          </h4>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Patient Profile</label>
            <select
              value={patientId}
              onChange={e => setPatientId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg p-2.5"
            >
              <option value="PAT-101 (Eleanor Vance)">PAT-101 - Eleanor Vance (Hypertension RPM)</option>
              <option value="PAT-102 (Marcus Thorne)">PAT-102 - Marcus Thorne (Diabetes RPM)</option>
              <option value="PAT-103 (Sophia Lin)">PAT-103 - Sophia Lin (COPD RPM)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Target Specialist Doctor</label>
            <select
              value={doctorId}
              onChange={e => {
                setDoctorId(e.target.value);
                setDoctorName(e.target.options[e.target.selectedIndex].text);
              }}
              className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg p-2.5"
            >
              <option value="DOC-204">Dr. Emily Vance (Cardiologist) - DOC-204</option>
              <option value="DOC-308">Dr. Alexander Hayes (Endocrinologist) - DOC-308</option>
              <option value="DOC-412">Dr. Sarah Jenkins (Pulmonologist) - DOC-412</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Pricing &amp; Copay Strategy Pattern</label>
            <select
              value={pricingStrategy}
              onChange={e => setPricingStrategy(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg p-2.5 font-mono text-[11px]"
            >
              <option value="SpecialistPricingStrategy">SpecialistPricingStrategy ($50 Floor + 15% Coinsurance)</option>
              <option value="StandardConsultationPricingStrategy">StandardConsultationPricingStrategy (Max $25 Copay)</option>
              <option value="EmergencyCarePricingStrategy">EmergencyCarePricingStrategy ($100 ER Flat Fee)</option>
              <option value="InsuranceCoveragePricingStrategy">InsuranceCoveragePricingStrategy (HMO/PPO Dynamic)</option>
            </select>
          </div>

          <div className="pt-2">
            <label className="flex items-center gap-2 p-3 rounded-lg bg-red-950/30 border border-red-800/40 cursor-pointer">
              <input
                type="checkbox"
                checked={simulateFailure}
                onChange={e => setSimulateFailure(e.target.checked)}
                className="rounded border-slate-700 text-red-500 focus:ring-red-500"
              />
              <span className="text-xs text-red-300 font-medium">
                Simulate Step 2 Insurance Rejection (Trigger Automated Saga Compensation Rollback)
              </span>
            </label>
          </div>

          <button
            onClick={runSaga}
            disabled={loading}
            className="w-full py-2.5 px-4 rounded-lg bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs transition-all flex items-center justify-center gap-2 shadow-lg shadow-teal-900/30"
          >
            {loading ? (
              <span className="animate-spin">⏳</span>
            ) : (
              <>
                <Play className="w-3.5 h-3.5" />
                Execute Consultation Saga
              </>
            )}
          </button>
        </div>

        {/* Saga Execution State Machine Graph */}
        <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <Layers className="w-4 h-4 text-indigo-400" />
              Saga Execution State Machine
            </h4>
            {sagaResult && (
              <span
                className={`text-xs px-2.5 py-0.5 rounded-full font-bold font-mono ${
                  sagaResult.success
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                    : 'bg-red-500/10 text-red-400 border border-red-500/30'
                }`}
              >
                Status: {sagaResult.finalStatus}
              </span>
            )}
          </div>

          {!sagaResult ? (
            <div className="py-16 text-center border border-dashed border-slate-800 rounded-lg">
              <Layers className="w-10 h-10 text-slate-700 mx-auto mb-2" />
              <p className="text-xs text-slate-500">Click &apos;Execute Consultation Saga&apos; to watch the distributed transaction live.</p>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center gap-4 text-xs font-mono text-slate-400 bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                <span>Saga ID: <strong className="text-teal-400">{sagaResult.sagaId}</strong></span>
                <span>Appointment ID: <strong className="text-slate-200">{sagaResult.appointmentId}</strong></span>
                {sagaResult.copay && <span>Patient Copay: <strong className="text-emerald-400">${sagaResult.copay}</strong></span>}
              </div>

              {/* Steps timeline */}
              <div className="space-y-2.5">
                {sagaResult.steps.map((step, idx) => {
                  const isCompensated = step.status === 'COMPENSATED';
                  const isFailed = step.status === 'FAILED';
                  const isSuccess = step.status === 'SUCCESS';

                  return (
                    <div
                      key={idx}
                      className={`p-3 rounded-lg border text-xs flex items-start gap-3 transition-all ${
                        isCompensated
                          ? 'bg-amber-950/30 border-amber-800/40 text-amber-200'
                          : isFailed
                          ? 'bg-red-950/30 border-red-800/40 text-red-200'
                          : 'bg-slate-950 border-slate-800 text-slate-300'
                      }`}
                    >
                      <div className="mt-0.5">
                        {isSuccess && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
                        {isFailed && <XCircle className="w-4 h-4 text-red-400" />}
                        {isCompensated && <RotateCcw className="w-4 h-4 text-amber-400 animate-spin" />}
                      </div>

                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-slate-100">{step.name}</span>
                          <span className="font-mono text-[10px] text-slate-400">{step.durationMs}ms</span>
                        </div>
                        <p className="text-[11px] text-slate-400 font-mono mt-0.5">{step.service}</p>
                        <p className="text-xs text-slate-300 mt-1">{step.detail}</p>
                      </div>
                    </div>
                  );
                })}
              </div>

              {sagaResult.outboxId && (
                <div className="p-3 bg-purple-950/30 border border-purple-800/40 rounded-lg text-xs text-purple-200 flex items-center justify-between">
                  <span>Kafka Outbox Event Created: <strong>{sagaResult.outboxId}</strong></span>
                  <span className="text-[10px] bg-purple-900/60 px-2 py-0.5 rounded font-mono">Status: PENDING &rarr; AUTO_DISPATCH</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Section 2: Redisson Distributed Lock Battle Simulator */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <Lock className="w-4 h-4 text-emerald-400" />
              Redisson Distributed Locking Concurrency Stress-Test
            </h4>
            <p className="text-xs text-slate-400">
              Simulates 5 concurrent microservice threads trying to book the exact same doctor slot (`DOC-204 @ 09:00 AM`).
            </p>
          </div>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-1.5 text-xs text-slate-300 cursor-pointer">
              <input
                type="checkbox"
                checked={useRedissonLock}
                onChange={e => setUseRedissonLock(e.target.checked)}
                className="rounded border-slate-700 text-teal-500 focus:ring-teal-500"
              />
              <span>Enable Redisson RLock</span>
            </label>

            <button
              onClick={runConcurrencyTest}
              disabled={concurrencyLoading}
              className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-teal-300 border border-teal-500/40 text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all"
            >
              {concurrencyLoading ? 'Testing...' : 'Fire 5 Concurrent Requests'}
            </button>
          </div>
        </div>

        {concurrencyResult && (
          <div className="space-y-3 pt-2">
            <div className={`p-3 rounded-lg border text-xs font-medium ${
              concurrencyResult.useRedissonLock
                ? 'bg-emerald-950/40 border-emerald-800/50 text-emerald-300'
                : 'bg-red-950/40 border-red-800/50 text-red-300'
            }`}>
              {concurrencyResult.summary}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-5 gap-2">
              {concurrencyResult.results.map((res: any, i: number) => (
                <div
                  key={i}
                  className={`p-3 rounded-lg border text-xs font-mono ${
                    res.acquiredLock
                      ? 'bg-emerald-950/30 border-emerald-800/60 text-emerald-200'
                      : 'bg-slate-950 border-slate-800 text-slate-400'
                  }`}
                >
                  <div className="font-bold text-slate-200 mb-1">{res.client}</div>
                  <div className="text-[11px] leading-tight">{res.outcome}</div>
                  <div className="text-[10px] text-slate-500 mt-2">Latency: {res.timeTakenMs}ms</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
