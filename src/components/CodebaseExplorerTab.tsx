import React, { useState, useEffect } from 'react';
import { Code2, FileCode, Check, Copy, FolderGit2 } from 'lucide-react';
import { CodebaseFileItem } from '../types';

export const CodebaseExplorerTab: React.FC = () => {
  const [files, setFiles] = useState<CodebaseFileItem[]>([]);
  const [selectedPath, setSelectedPath] = useState<string>('microservices/appointment-order-service/src/main/java/com/healthcare/appointment/saga/AppointmentSagaOrchestrator.java');
  const [fileContent, setFileContent] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);

  const fetchFiles = async () => {
    try {
      const res = await fetch('/api/codebase/files');
      const data = await res.json();
      setFiles(data);
    } catch (e) {
      console.error(e);
    }
  };

  const loadFileContent = async (path: string) => {
    setSelectedPath(path);
    setLoading(true);
    try {
      const res = await fetch(`/api/codebase/file?path=${encodeURIComponent(path)}`);
      const data = await res.json();
      setFileContent(data.content || '// Content unavailable');
    } catch (e) {
      setFileContent('// Failed to read file');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiles();
    loadFileContent(selectedPath);
  }, []);

  const handleCopy = () => {
    navigator.clipboard.writeText(fileContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Group files by category
  const categories = Array.from(new Set(files.map(f => f.category)));

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-teal-500/10 border border-teal-500/30 rounded-xl text-teal-400">
            <Code2 className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100">Enterprise Codebase &amp; Kubernetes Explorer</h3>
            <p className="text-xs text-slate-400">
              Browse production-ready Java 21, Spring Boot 3.4 microservices, Liquibase XML migrations, Docker Compose, and Kubernetes manifests.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left file tree */}
        <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-4 max-h-[700px] overflow-y-auto">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
            <FolderGit2 className="w-4 h-4 text-teal-400" />
            Project File Tree
          </h4>

          <div className="space-y-4">
            {categories.map(cat => (
              <div key={cat} className="space-y-1">
                <span className="text-[11px] font-semibold text-slate-400 font-mono block px-2 py-0.5 bg-slate-950 rounded">
                  {cat}
                </span>
                <div className="space-y-0.5 pl-1">
                  {files.filter(f => f.category === cat).map(f => {
                    const isSelected = selectedPath === f.path;
                    const fileName = f.path.split('/').pop() || f.path;
                    return (
                      <button
                        key={f.path}
                        onClick={() => loadFileContent(f.path)}
                        className={`w-full text-left p-1.5 rounded text-xs font-mono transition-all truncate flex items-center gap-1.5 ${
                          isSelected
                            ? 'bg-teal-500/20 text-teal-300 border border-teal-500/40 font-bold'
                            : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                        }`}
                        title={f.path}
                      >
                        <FileCode className="w-3.5 h-3.5 flex-shrink-0" />
                        <span className="truncate">{fileName}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Code Viewer */}
        <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3 flex flex-col">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="text-xs font-mono text-slate-300 truncate max-w-lg">
              <span className="text-slate-500">File: </span>
              <strong className="text-teal-400">{selectedPath}</strong>
            </div>

            <button
              onClick={handleCopy}
              className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700 transition-colors"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-300">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Copy</span>
                </>
              )}
            </button>
          </div>

          <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs text-slate-200 overflow-x-auto max-h-[600px] flex-1">
            {loading ? (
              <div className="text-slate-500 italic">Loading file content...</div>
            ) : (
              <pre className="text-slate-300 leading-relaxed font-mono whitespace-pre text-[11px]">
                {fileContent}
              </pre>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
