import React, { useState, useEffect } from 'react';
import { Database, Play, RefreshCw, Send, Radio, CheckCircle, Clock, AlertCircle } from 'lucide-react';
import { OutboxEventItem, KafkaTopicMessage } from '../types';

export const OutboxKafkaTab: React.FC = () => {
  const [events, setEvents] = useState<OutboxEventItem[]>([]);
  const [kafkaTopics, setKafkaTopics] = useState<Record<string, KafkaTopicMessage[]>>({});
  const [loading, setLoading] = useState(false);
  const [dequeuing, setDequeuing] = useState(false);

  const fetchOutbox = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/outbox/events');
      const data = await res.json();
      setEvents(data.events || []);
      setKafkaTopics(data.kafkaTopics || {});
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const triggerDequeue = async () => {
    setDequeuing(true);
    try {
      await fetch('/api/outbox/dequeue', { method: 'POST' });
      await fetchOutbox();
    } catch (e) {
      console.error(e);
    } finally {
      setDequeuing(false);
    }
  };

  useEffect(() => {
    fetchOutbox();
    const interval = setInterval(fetchOutbox, 4000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-purple-500/10 border border-purple-500/30 rounded-xl text-purple-400">
              <Database className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">Transactional Outbox Pattern &amp; Kafka Bus</h3>
              <p className="text-xs text-slate-400">
                Guarantees dual-write atomic consistency between PostgreSQL local transaction table and Apache Kafka topic brokers.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={triggerDequeue}
              disabled={dequeuing}
              className="px-3 py-1.5 bg-purple-600 hover:bg-purple-500 text-white font-medium text-xs rounded-lg flex items-center gap-1.5 transition-all shadow-md"
            >
              <Send className={`w-3.5 h-3.5 ${dequeuing ? 'animate-bounce' : ''}`} />
              Trigger Outbox Scheduler Poller
            </button>

            <button
              onClick={fetchOutbox}
              className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg border border-slate-700 transition-colors"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Outbox Table */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <Database className="w-4 h-4 text-purple-400" />
              PostgreSQL Table: `transactional_outbox_events`
            </h4>
            <span className="text-xs font-mono text-slate-400">{events.length} Total Events</span>
          </div>

          <div className="overflow-x-auto border border-slate-800 rounded-lg">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-950 text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-2.5">Event ID</th>
                  <th className="p-2.5">Aggregate</th>
                  <th className="p-2.5">Type</th>
                  <th className="p-2.5">Status</th>
                  <th className="p-2.5">Created At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {events.map(ev => {
                  const isSent = ev.status === 'SENT';
                  const isPending = ev.status === 'PENDING';
                  return (
                    <tr key={ev.id} className="hover:bg-slate-800/40">
                      <td className="p-2.5 font-bold text-slate-200">{ev.id}</td>
                      <td className="p-2.5 text-teal-400">{ev.aggregateType}:{ev.aggregateId}</td>
                      <td className="p-2.5 text-purple-300">{ev.eventType}</td>
                      <td className="p-2.5">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            isSent
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                              : isPending
                              ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                              : 'bg-red-500/10 text-red-400'
                          }`}
                        >
                          {ev.status}
                        </span>
                      </td>
                      <td className="p-2.5 text-slate-500 text-[11px]">{new Date(ev.createdAt).toLocaleTimeString()}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Kafka Topics Consumer Viewer */}
        <div className="lg:col-span-5 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <Radio className="w-4 h-4 text-emerald-400" />
            Kafka Broker Cluster (KRaft Partition Log)
          </h4>

          <div className="space-y-3">
            {Object.entries(kafkaTopics).map(([topic, msgsList]) => {
              const msgs = (msgsList || []) as KafkaTopicMessage[];
              return (
              <div key={topic} className="bg-slate-950 border border-slate-800 p-3 rounded-lg space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-mono text-teal-400 font-semibold">{topic}</span>
                  <span className="text-[10px] text-slate-500 font-mono">{msgs.length} msgs</span>
                </div>

                {msgs.length === 0 ? (
                  <p className="text-[11px] text-slate-600 italic">No messages on topic yet.</p>
                ) : (
                  <div className="space-y-1.5">
                    {msgs.slice(0, 3).map((m, i) => (
                      <div key={i} className="p-2 rounded bg-slate-900 border border-slate-800 text-[11px] font-mono text-slate-300">
                        <div className="flex items-center justify-between text-slate-400 text-[10px]">
                          <span>Key: {m.key}</span>
                          <span>{new Date(m.timestamp).toLocaleTimeString()}</span>
                        </div>
                        <div className="mt-1 text-purple-300 truncate">
                          {JSON.stringify(m.payload)}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );})}
          </div>
        </div>
      </div>
    </div>
  );
};
