import React, { useState, useEffect } from 'react';
import { Radio, ShieldCheck, MapPin, Thermometer, CheckCircle2, AlertTriangle, Cpu, FileCheck } from 'lucide-react';

export const CareDispatchTab: React.FC = () => {
  const [specialty, setSpecialty] = useState('ICU');
  const [dispatchResult, setDispatchResult] = useState<any | null>(null);
  const [loading, setLoading] = useState(false);

  // Digital POD state
  const [prescriptionId, setPrescriptionId] = useState('RX-9912');
  const [patientId, setPatientId] = useState('PAT-101');
  const [recipientName, setRecipientName] = useState('Eleanor Vance');
  const [sampleTemp, setSampleTemp] = useState(4.2);
  const [podResult, setPodResult] = useState<any | null>(null);

  const calculateDispatch = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/dispatch/match', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          patientLat: 37.7749,
          patientLon: -122.4194,
          specialty,
        }),
      });
      const data = await res.json();
      setDispatchResult(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const generatePod = async () => {
    try {
      const res = await fetch('/api/fulfillment/pod', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prescriptionId,
          patientId,
          recipientName,
          currentTemp: Number(sampleTemp),
        }),
      });
      const data = await res.json();
      setPodResult(data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    calculateDispatch();
    generatePod();
  }, []);

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400">
            <Radio className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100">Care Dispatch Scoring &amp; Cold-Chain Digital POD</h3>
            <p className="text-xs text-slate-400">
              Weighted responder scoring (Proximity 40%, Specialty 30%, Workload 20%, Rating 10%) &amp; HMAC-SHA256 digital proof of delivery.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Clinical Dispatch Matching Engine */}
        <div className="lg:col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <MapPin className="w-4 h-4 text-emerald-400" />
              Nurse &amp; Physician Dispatch Scoring
            </h4>

            <div className="flex items-center gap-2">
              <select
                value={specialty}
                onChange={e => {
                  setSpecialty(e.target.value);
                  setTimeout(calculateDispatch, 50);
                }}
                className="bg-slate-950 border border-slate-700 text-xs text-slate-200 rounded-lg p-1.5"
              >
                <option value="ICU">ICU Critical Care</option>
                <option value="CARDIOLOGY">Cardiology Care</option>
                <option value="PEDIATRICS">Pediatrics</option>
              </select>
            </div>
          </div>

          {dispatchResult?.allCandidates && (
            <div className="space-y-2.5">
              {dispatchResult.allCandidates.map((c: any, idx: number) => {
                const isTop = idx === 0;
                return (
                  <div
                    key={c.candidate.id}
                    className={`p-3.5 rounded-lg border transition-all text-xs ${
                      isTop
                        ? 'bg-emerald-950/30 border-emerald-500/60 ring-1 ring-emerald-500/30'
                        : 'bg-slate-950 border-slate-800 text-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {isTop && (
                          <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold text-[10px]">
                            OPTIMAL MATCH
                          </span>
                        )}
                        <span className="font-bold text-slate-100">{c.candidate.name}</span>
                        <span className="text-[10px] font-mono text-slate-400">({c.candidate.id})</span>
                      </div>
                      <div className="font-mono font-bold text-teal-300 text-sm">
                        {c.totalScore.toFixed(1)} <span className="text-[10px] text-slate-400">/ 100</span>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-2 mt-2 pt-2 border-t border-slate-800/80 text-[11px] text-slate-400">
                      <div>Distance: <strong className="text-slate-200">{c.distanceKm} km</strong></div>
                      <div>ETA: <strong className="text-teal-400">{c.estimatedEtaMinutes} mins</strong></div>
                      <div>Specialty: <strong className="text-slate-200">{c.candidate.specialty}</strong></div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Digital Proof of Dispensation (POD) & Cold-Chain */}
        <div className="lg:col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <FileCheck className="w-4 h-4 text-purple-400" />
            Digital Proof of Delivery (POD) &amp; Cold-Chain Telemetry
          </h4>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] text-slate-400 mb-1">Prescription ID</label>
              <input
                type="text"
                value={prescriptionId}
                onChange={e => setPrescriptionId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg p-2"
              />
            </div>
            <div>
              <label className="block text-[11px] text-slate-400 mb-1">Sample Temp (°C)</label>
              <input
                type="number"
                step="0.1"
                value={sampleTemp}
                onChange={e => setSampleTemp(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-xs rounded-lg p-2"
              />
            </div>
          </div>

          <button
            onClick={generatePod}
            className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-teal-300 border border-teal-500/40 text-xs font-semibold rounded-lg transition-all"
          >
            Compute HMAC-SHA256 Digital Signature &amp; Validate Chain
          </button>

          {podResult && (
            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 space-y-2 text-xs font-mono">
              <div className="flex items-center justify-between">
                <span className="text-slate-400">POD Receipt ID:</span>
                <span className="text-purple-300 font-bold">{podResult.podId}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-400">Digital Recipient Signature:</span>
                <span className="text-slate-200">{podResult.recipientSignature}</span>
              </div>
              <div className="text-slate-400">
                <span>HMAC-SHA256 Hash:</span>
                <div className="text-[10px] text-emerald-400 break-all bg-slate-900 p-1.5 rounded mt-0.5">
                  {podResult.cryptographicHash}
                </div>
              </div>

              <div className={`p-2.5 rounded border mt-2 flex items-center justify-between ${
                podResult.coldChainTelemetry.isBreached
                  ? 'bg-red-950/40 border-red-800 text-red-300'
                  : 'bg-emerald-950/40 border-emerald-800 text-emerald-300'
              }`}>
                <span>Cold-Chain Status:</span>
                <strong className="font-bold">{podResult.coldChainTelemetry.status}</strong>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
