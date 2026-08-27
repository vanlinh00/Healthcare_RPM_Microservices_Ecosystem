import React, { useState, useEffect, useRef } from 'react';
import { Activity, Heart, AlertTriangle, Radio, ShieldAlert, Cpu, CheckCircle2, Zap } from 'lucide-react';
import { PatientVitalTelemetry } from '../types';

export const RpmTelemetryTab: React.FC = () => {
  const [patientId, setPatientId] = useState('PAT-101 (Eleanor Vance)');
  const [deviceId, setDeviceId] = useState('RPM-OXIMETER-88');
  const [heartRate, setHeartRate] = useState(78);
  const [systolicBp, setSystolicBp] = useState(124);
  const [diastolicBp, setDiastolicBp] = useState(82);
  const [spo2, setSpo2] = useState(98.5);
  const [bloodGlucose, setBloodGlucose] = useState(95);
  const [lastTelemetry, setLastTelemetry] = useState<PatientVitalTelemetry | null>(null);
  const [liveStreamActive, setLiveStreamActive] = useState(true);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Ingest telemetry to backend
  const sendTelemetry = async (override?: Partial<PatientVitalTelemetry>) => {
    const payload = {
      patientId: patientId.split(' ')[0],
      deviceId,
      heartRate: override?.heartRate ?? heartRate,
      systolicBp: override?.systolicBp ?? systolicBp,
      diastolicBp: override?.diastolicBp ?? diastolicBp,
      spo2: override?.spo2 ?? spo2,
      bloodGlucose: override?.bloodGlucose ?? bloodGlucose,
    };

    try {
      const res = await fetch('/api/telemetry/ingest', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = await res.json();
      setLastTelemetry(data);
    } catch (e) {
      console.error(e);
    }
  };

  // Anomaly Injection triggers
  const triggerAnomaly = (type: 'TACHYCARDIA' | 'HYPOXIA' | 'HYPERTENSIVE_CRISIS' | 'HYPOGLYCEMIA') => {
    if (type === 'TACHYCARDIA') {
      setHeartRate(148);
      sendTelemetry({ heartRate: 148 });
    } else if (type === 'HYPOXIA') {
      setSpo2(84.0);
      sendTelemetry({ spo2: 84.0 });
    } else if (type === 'HYPERTENSIVE_CRISIS') {
      setSystolicBp(195);
      setDiastolicBp(125);
      sendTelemetry({ systolicBp: 195, diastolicBp: 125 });
    } else if (type === 'HYPOGLYCEMIA') {
      setBloodGlucose(52);
      sendTelemetry({ bloodGlucose: 52 });
    }
  };

  const resetNormal = () => {
    setHeartRate(75);
    setSystolicBp(120);
    setDiastolicBp(80);
    setSpo2(98.5);
    setBloodGlucose(95);
    sendTelemetry({ heartRate: 75, systolicBp: 120, diastolicBp: 80, spo2: 98.5, bloodGlucose: 95 });
  };

  // Real-time ECG Waveform Canvas Render Loop
  useEffect(() => {
    let animationFrameId: number;
    let offset = 0;
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const render = () => {
      ctx.fillStyle = '#020617';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Draw subtle ECG grid lines
      ctx.strokeStyle = '#1e293b';
      ctx.lineWidth = 0.5;
      for (let x = 0; x < canvas.width; x += 20) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, canvas.height);
        ctx.stroke();
      }
      for (let y = 0; y < canvas.height; y += 20) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(canvas.width, y);
        ctx.stroke();
      }

      // Draw Real-time ECG Waveform
      ctx.strokeStyle = heartRate > 120 ? '#ef4444' : '#10b981';
      ctx.lineWidth = 2;
      ctx.beginPath();

      const centerY = canvas.height / 2;
      for (let x = 0; x < canvas.width; x++) {
        const t = (x + offset) % 180;
        let y = centerY;

        // P-Q-R-S-T synthetic pulse math
        if (t > 40 && t < 55) {
          y -= Math.sin(((t - 40) / 15) * Math.PI) * 12; // P wave
        } else if (t >= 65 && t < 70) {
          y += (t - 65) * 3; // Q drop
        } else if (t >= 70 && t < 80) {
          y -= (1 - Math.abs(t - 75) / 5) * 55; // R peak
        } else if (t >= 80 && t < 86) {
          y += (86 - t) * 3; // S dip
        } else if (t > 110 && t < 140) {
          y -= Math.sin(((t - 110) / 30) * Math.PI) * 16; // T wave
        }

        if (x === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      }
      ctx.stroke();

      offset += heartRate > 120 ? 4 : 2;
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [heartRate]);

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-teal-500/10 border border-teal-500/30 rounded-xl text-teal-400">
              <Activity className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">Patient Telemetry &amp; Remote Monitoring (RPM)</h3>
              <p className="text-xs text-slate-400">
                Continuous IoT vital streams (ECG, SpO2, Blood Pressure, Glucose), WebSocket ICU broadcasting, and automated Code-Blue emergency dispatch.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/30 rounded-lg text-xs font-mono text-emerald-400">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
              WebSocket /ws/rpm/stream ACTIVE
            </span>
          </div>
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Live ECG Waveform Monitor */}
        <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Heart className="w-4 h-4 text-rose-400 animate-pulse" />
              <h4 className="text-sm font-bold text-slate-200">ICU Telemetry Waveform Lead II</h4>
            </div>
            <div className="text-xs font-mono text-slate-400">
              Patient: <strong className="text-teal-400">{patientId}</strong> • Device: {deviceId}
            </div>
          </div>

          <div className="rounded-lg overflow-hidden border border-slate-800 bg-slate-950">
            <canvas ref={canvasRef} width={640} height={180} className="w-full h-44 block" />
          </div>

          {/* Vitals KPI cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className={`p-3 rounded-lg border ${heartRate > 120 ? 'bg-red-950/40 border-red-800 text-red-300' : 'bg-slate-950 border-slate-800 text-slate-200'}`}>
              <span className="text-[11px] text-slate-400 block">Heart Rate</span>
              <div className="text-2xl font-bold font-mono mt-0.5">{heartRate} <span className="text-xs font-normal">BPM</span></div>
            </div>

            <div className={`p-3 rounded-lg border ${systolicBp >= 180 ? 'bg-red-950/40 border-red-800 text-red-300' : 'bg-slate-950 border-slate-800 text-slate-200'}`}>
              <span className="text-[11px] text-slate-400 block">Blood Pressure</span>
              <div className="text-2xl font-bold font-mono mt-0.5">{systolicBp}/{diastolicBp} <span className="text-xs font-normal">mmHg</span></div>
            </div>

            <div className={`p-3 rounded-lg border ${spo2 < 90 ? 'bg-red-950/40 border-red-800 text-red-300' : 'bg-slate-950 border-slate-800 text-slate-200'}`}>
              <span className="text-[11px] text-slate-400 block">Oxygen Sat (SpO2)</span>
              <div className="text-2xl font-bold font-mono mt-0.5">{spo2}%</div>
            </div>

            <div className={`p-3 rounded-lg border ${bloodGlucose < 70 ? 'bg-amber-950/40 border-amber-800 text-amber-300' : 'bg-slate-950 border-slate-800 text-slate-200'}`}>
              <span className="text-[11px] text-slate-400 block">Blood Glucose</span>
              <div className="text-2xl font-bold font-mono mt-0.5">{bloodGlucose} <span className="text-xs font-normal">mg/dL</span></div>
            </div>
          </div>
        </div>

        {/* Anomaly Testing & Code Blue Dispatch Center */}
        <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            RPM Anomaly &amp; Code Blue Simulator
          </h4>
          <p className="text-xs text-slate-400">
            Inject abnormal patient biometric anomalies to test automatic Kafka event publishing and isolated emergency thread pool dispatch.
          </p>

          <div className="grid grid-cols-1 gap-2 pt-1">
            <button
              onClick={() => triggerAnomaly('TACHYCARDIA')}
              className="w-full text-left p-2.5 rounded-lg bg-red-950/30 hover:bg-red-900/40 border border-red-800/40 text-xs text-red-200 transition-all font-medium flex items-center justify-between"
            >
              <span>🚨 Inject Tachycardia (148 BPM)</span>
              <Zap className="w-3.5 h-3.5 text-red-400" />
            </button>

            <button
              onClick={() => triggerAnomaly('HYPOXIA')}
              className="w-full text-left p-2.5 rounded-lg bg-red-950/30 hover:bg-red-900/40 border border-red-800/40 text-xs text-red-200 transition-all font-medium flex items-center justify-between"
            >
              <span>🚨 Inject Hypoxia (SpO2 84%)</span>
              <Zap className="w-3.5 h-3.5 text-red-400" />
            </button>

            <button
              onClick={() => triggerAnomaly('HYPERTENSIVE_CRISIS')}
              className="w-full text-left p-2.5 rounded-lg bg-red-950/30 hover:bg-red-900/40 border border-red-800/40 text-xs text-red-200 transition-all font-medium flex items-center justify-between"
            >
              <span>🚨 Inject Hypertensive Crisis (195/125)</span>
              <Zap className="w-3.5 h-3.5 text-red-400" />
            </button>

            <button
              onClick={() => triggerAnomaly('HYPOGLYCEMIA')}
              className="w-full text-left p-2.5 rounded-lg bg-amber-950/30 hover:bg-amber-900/40 border border-amber-800/40 text-xs text-amber-200 transition-all font-medium flex items-center justify-between"
            >
              <span>⚠️ Inject Hypoglycemia (52 mg/dL)</span>
              <Zap className="w-3.5 h-3.5 text-amber-400" />
            </button>

            <button
              onClick={resetNormal}
              className="w-full text-center p-2.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 font-semibold transition-all mt-2"
            >
              Reset to Normal Baseline Vitals
            </button>
          </div>

          {lastTelemetry?.anomalyFlag && lastTelemetry.anomalyFlag !== 'NORMAL' && (
            <div className="p-3 bg-red-950/50 border border-red-700 rounded-lg text-xs text-red-200 space-y-1">
              <div className="font-bold flex items-center gap-1.5 text-red-300">
                <ShieldAlert className="w-4 h-4 text-red-400" />
                EMERGENCY CODE BLUE DISPATCHED!
              </div>
              <p className="text-[11px] text-red-300">
                Anomaly: <strong>{lastTelemetry.anomalyFlag}</strong>. Kafka topic `healthcare.emergency.events` published. Isolated thread pool `emergencyCodeBlueExecutor` alerted on-call doctors via PUSH/SMS.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
