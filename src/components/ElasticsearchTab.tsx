import React, { useState, useEffect } from 'react';
import { Search, Cpu, Tag, FileText, Activity, Clock, Filter } from 'lucide-react';
import { MedicalRecordHit } from '../types';

export const ElasticsearchTab: React.FC = () => {
  const [query, setQuery] = useState('');
  const [anomalyFilter, setAnomalyFilter] = useState('ALL');
  const [records, setRecords] = useState<MedicalRecordHit[]>([]);
  const [tookMs, setTookMs] = useState(2);
  const [totalHits, setTotalHits] = useState(0);
  const [loading, setLoading] = useState(false);

  const performSearch = async (searchQuery: string = query, filter: string = anomalyFilter) => {
    setLoading(true);
    try {
      const res = await fetch(`/api/elasticsearch/search?q=${encodeURIComponent(searchQuery)}&anomaly=${encodeURIComponent(filter)}`);
      const data = await res.json();
      setRecords(data.hits || []);
      setTookMs(data.tookMs || 2);
      setTotalHits(data.totalHits || 0);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    performSearch('', 'ALL');
  }, []);

  const quickQueries = ['Hypertension', 'Hypoglycemia', 'COPD', 'I10', 'SpO2', 'Lisinopril'];

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-blue-500/10 border border-blue-500/30 rounded-xl text-blue-400">
              <Cpu className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">Elasticsearch 8.17 Medical Record &amp; Symptom Search</h3>
              <p className="text-xs text-slate-400">
                Millisecond inverted-index search across diagnosis histories, clinical notes, ICD-10 codes, and continuous telemetry anomalies.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-mono px-3 py-1.5 rounded-lg bg-blue-500/10 border border-blue-500/30 text-blue-300">
              Cluster: <strong className="text-white">healthcare-rpm-cluster</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Search Input and Filter Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={e => {
                setQuery(e.target.value);
                performSearch(e.target.value, anomalyFilter);
              }}
              placeholder="Search diagnoses, symptoms (e.g. 'palpitations', 'dyspnea'), ICD-10 codes, or medications..."
              className="w-full pl-9 pr-4 py-2.5 bg-slate-950 border border-slate-700 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Filter className="w-4 h-4 text-slate-400" />
            <select
              value={anomalyFilter}
              onChange={e => {
                setAnomalyFilter(e.target.value);
                performSearch(query, e.target.value);
              }}
              className="bg-slate-950 border border-slate-700 text-xs text-slate-200 rounded-lg p-2.5"
            >
              <option value="ALL">All Risk Categories</option>
              <option value="LOW">Low Risk</option>
              <option value="MODERATE">Moderate Risk</option>
              <option value="HIGH">High Risk</option>
              <option value="CRITICAL">Critical Code-Blue Risk</option>
            </select>
          </div>
        </div>

        {/* Quick query chips */}
        <div className="flex items-center gap-2 flex-wrap text-xs text-slate-400">
          <span className="text-[11px]">Quick Query Suggestions:</span>
          {quickQueries.map(q => (
            <button
              key={q}
              onClick={() => {
                setQuery(q);
                performSearch(q, anomalyFilter);
              }}
              className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-blue-300 text-xs border border-slate-700 transition-colors"
            >
              {q}
            </button>
          ))}
        </div>

        {/* Search benchmark metadata */}
        <div className="flex items-center justify-between text-xs text-slate-400 pt-2 border-t border-slate-800">
          <span>Found <strong>{totalHits}</strong> indexed medical documents</span>
          <span className="font-mono text-emerald-400 flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            Query took: {tookMs}ms (Elasticsearch Inverted Index)
          </span>
        </div>
      </div>

      {/* Results List */}
      <div className="space-y-3">
        {records.length === 0 ? (
          <div className="p-12 text-center bg-slate-900 border border-slate-800 rounded-xl text-slate-500 text-xs">
            No matching medical records found in Elasticsearch cluster.
          </div>
        ) : (
          records.map(r => (
            <div key={r.id} className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3 hover:border-slate-700 transition-all">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-bold text-slate-100">{r.patientName} ({r.patientId})</h4>
                    <span className="text-[10px] font-mono bg-slate-800 text-slate-300 px-2 py-0.5 rounded">
                      Record: {r.id}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5">Attending Physician: {r.doctorName}</p>
                </div>

                <span
                  className={`text-[10px] font-bold px-2.5 py-1 rounded-full font-mono ${
                    r.anomalyRisk === 'CRITICAL'
                      ? 'bg-red-500/10 text-red-400 border border-red-500/30'
                      : r.anomalyRisk === 'HIGH'
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                      : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                  }`}
                >
                  Risk: {r.anomalyRisk}
                </span>
              </div>

              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 text-xs text-slate-300 space-y-2">
                <div>
                  <span className="text-slate-400 font-semibold">Diagnosis: </span>
                  <strong className="text-teal-300">{r.diagnosis}</strong>
                </div>
                <div>
                  <span className="text-slate-400 font-semibold">Clinical Notes: </span>
                  <span>{r.clinicalNotes}</span>
                </div>
                <div>
                  <span className="text-slate-400 font-semibold">Medications: </span>
                  <span className="font-mono text-purple-300">{r.prescribedMedications}</span>
                </div>
              </div>

              {/* Tag Badges */}
              <div className="flex items-center gap-2 flex-wrap text-xs">
                <span className="text-slate-500 text-[11px]">Symptoms:</span>
                {r.symptoms.map(s => (
                  <span key={s} className="px-2 py-0.5 bg-slate-800 text-slate-300 rounded text-[11px]">
                    {s}
                  </span>
                ))}
                <span className="text-slate-500 text-[11px] ml-2">ICD-10:</span>
                {r.icd10Codes.map(c => (
                  <span key={c} className="px-2 py-0.5 bg-blue-950 text-blue-300 border border-blue-800 rounded font-mono text-[11px]">
                    {c}
                  </span>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
