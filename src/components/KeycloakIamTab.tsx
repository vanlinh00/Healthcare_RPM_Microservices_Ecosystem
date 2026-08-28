import React, { useState, useEffect } from 'react';
import {
  ShieldCheck,
  Key,
  Lock,
  CheckCircle,
  ShieldAlert,
  Copy,
  RefreshCw,
  UserCheck,
  LogIn,
  LogOut,
  FileCode,
  Activity,
  Check,
  AlertCircle,
  Users,
  Plus,
  Trash2,
  Edit,
  Search,
  Layers,
  Award,
  ArrowRight
} from 'lucide-react';

interface AuditLog {
  id: number;
  userId: string;
  action: string;
  ipAddress: string;
  userAgent: string;
  status: string;
  hipaaEventType: string;
  timestamp: string;
}

interface KeycloakRole {
  id: string;
  name: string;
  description: string;
  composite: boolean;
  clientRole: boolean;
  containerId: string;
  attributes?: Record<string, string[]>;
  compositeSubRoles?: string[];
}

interface EffectivePermissions {
  userId: string;
  username: string;
  email: string;
  directRealmRoles: string[];
  compositeEffectiveRealmRoles: string[];
  directClientRoles: Record<string, string[]>;
  compositeEffectiveClientRoles: Record<string, string[]>;
  totalEffectiveRolesCount: number;
}

export const KeycloakIamTab: React.FC = () => {
  const [selectedRole, setSelectedRole] = useState<'PATIENT' | 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'LAB_TECH' | 'ADMIN'>('DOCTOR');
  const [username, setUsername] = useState('doctor_emily');
  const [password, setPassword] = useState('Password123!');
  const [totpCode, setTotpCode] = useState('849201');
  const [tokenResponse, setTokenResponse] = useState<any | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [logoutMessage, setLogoutMessage] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'tester' | 'roles' | 'audit' | 'code'>('roles');

  // Roles Management state
  const [roles, setRoles] = useState<KeycloakRole[]>([]);
  const [roleSearchQuery, setRoleSearchQuery] = useState('');
  const [isCreatingRole, setIsCreatingRole] = useState(false);
  const [newRoleName, setNewRoleName] = useState('');
  const [newRoleDesc, setNewRoleDesc] = useState('');
  const [newRoleIsComposite, setNewRoleIsComposite] = useState(false);
  const [selectedSubRoles, setSelectedSubRoles] = useState<string[]>([]);
  const [newRoleClearance, setNewRoleClearance] = useState('LEVEL_2');
  const [roleActionMessage, setRoleActionMessage] = useState<string | null>(null);
  const [roleActionError, setRoleActionError] = useState<string | null>(null);

  // User Mappings & Effective Auditing state
  const [auditUserId, setAuditUserId] = useState('usr-doc-204');
  const [effectiveAuditData, setEffectiveAuditData] = useState<EffectivePermissions | null>(null);
  const [mappingRoleToAssign, setMappingRoleToAssign] = useState('ADMIN');
  const [activeCodeFile, setActiveCodeFile] = useState<
    'controller' | 'service' | 'serviceImpl' | 'config' | 'props' | 'dto' | 'exceptions' | 'yaml'
  >('controller');

  const fetchRoles = async () => {
    try {
      const url = roleSearchQuery ? `/api/v1/iam/roles?query=${encodeURIComponent(roleSearchQuery)}` : '/api/v1/iam/roles';
      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        setRoles(data);
      }
    } catch (e) {
      console.error('Failed to load Keycloak roles', e);
    }
  };

  const fetchAuditLogs = async () => {
    try {
      const res = await fetch('/api/v1/auth/audit-logs');
      if (res.ok) {
        const data = await res.json();
        setAuditLogs(data);
      }
    } catch (e) {
      console.error('Failed to load audit logs', e);
    }
  };

  const fetchEffectivePermissions = async (uid: string) => {
    try {
      const res = await fetch(`/api/v1/iam/mappings/users/${uid}/effective`);
      if (res.ok) {
        const data = await res.json();
        setEffectiveAuditData(data);
      }
    } catch (e) {
      console.error('Failed to fetch effective permissions', e);
    }
  };

  const handleCreateRole = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRoleName.trim()) return;

    setRoleActionMessage(null);
    setRoleActionError(null);
    try {
      const res = await fetch('/api/v1/iam/roles', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: newRoleName.trim().toUpperCase(),
          description: newRoleDesc,
          composite: newRoleIsComposite,
          subRoleNames: newRoleIsComposite ? selectedSubRoles : [],
          clientRole: false,
          attributes: {
            clearance_level: [newRoleClearance],
            created_via: ['KEYCLOAK_ADMIN_CLIENT_SDK_JAVA17']
          }
        })
      });

      const data = await res.json();
      if (!res.ok) {
        setRoleActionError(data.message || data.error || 'Failed to create role');
      } else {
        setRoleActionMessage(`Role '${data.name}' created successfully via Keycloak Admin Client.`);
        setIsCreatingRole(false);
        setNewRoleName('');
        setNewRoleDesc('');
        setSelectedSubRoles([]);
        fetchRoles();
        fetchAuditLogs();
      }
    } catch (err: any) {
      setRoleActionError(err.message || 'Network error');
    }
  };

  const handleDeleteRole = async (roleName: string) => {
    if (!confirm(`Are you sure you want to delete role '${roleName}' from Keycloak?`)) return;
    setRoleActionMessage(null);
    setRoleActionError(null);
    try {
      const res = await fetch(`/api/v1/iam/roles/${roleName}`, {
        method: 'DELETE'
      });
      const data = await res.json();
      if (res.ok) {
        setRoleActionMessage(data.message || `Role '${roleName}' deleted.`);
        fetchRoles();
      } else {
        setRoleActionError(data.message || 'Failed to delete role');
      }
    } catch (err: any) {
      setRoleActionError(err.message || 'Delete error');
    }
  };

  const handleAssignRoleToUser = async () => {
    if (!auditUserId || !mappingRoleToAssign) return;
    setRoleActionMessage(null);
    setRoleActionError(null);
    try {
      const res = await fetch('/api/v1/iam/mappings/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: auditUserId,
          roleNames: [mappingRoleToAssign]
        })
      });
      const data = await res.json();
      if (res.ok) {
        setRoleActionMessage(`Assigned role '${mappingRoleToAssign}' to user '${auditUserId}'.`);
        fetchEffectivePermissions(auditUserId);
      } else {
        setRoleActionError(data.message || 'Failed to assign role');
      }
    } catch (err: any) {
      setRoleActionError(err.message || 'Error assigning role');
    }
  };

  const handleRevokeRoleFromUser = async (roleToRevoke: string) => {
    if (!auditUserId || !roleToRevoke) return;
    setRoleActionMessage(null);
    setRoleActionError(null);
    try {
      const res = await fetch('/api/v1/iam/mappings/users', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: auditUserId,
          roleNames: [roleToRevoke]
        })
      });
      const data = await res.json();
      if (res.ok) {
        setRoleActionMessage(`Revoked role '${roleToRevoke}' from user '${auditUserId}'.`);
        fetchEffectivePermissions(auditUserId);
      } else {
        setRoleActionError(data.message || 'Failed to revoke role');
      }
    } catch (err: any) {
      setRoleActionError(err.message || 'Error revoking role');
    }
  };

  const handleLogin = async () => {
    setLoading(true);
    setLoginError(null);
    setLogoutMessage(null);
    try {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usernameOrEmail: username,
          password: password,
          totpCode: totpCode,
          deviceId: 'healthcare-workstation-react-v1'
        })
      });

      const data = await res.json();

      if (!res.ok) {
        setLoginError(data.error || 'Authentication failed');
        setTokenResponse(null);
      } else if (data.totp_required && !data.totp_verified) {
        setTokenResponse(data);
        setLoginError('TOTP 2FA Challenge: Please enter the 6-digit code from your authenticator app.');
      } else {
        setTokenResponse(data);
      }
      fetchAuditLogs();
    } catch (err: any) {
      setLoginError(err.message || 'Network error communicating with user-auth-service');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    setLoading(true);
    setLoginError(null);
    try {
      const res = await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': tokenResponse?.access_token ? `Bearer ${tokenResponse.access_token}` : ''
        },
        body: JSON.stringify({
          refresh_token: tokenResponse?.refresh_token || 'rt-current-session',
          session_state: tokenResponse?.session_state,
          all_sessions: false,
          userId: tokenResponse?.user?.id || 'usr-doc-204'
        })
      });

      const data = await res.json();
      setTokenResponse(null);
      setLogoutMessage(data.message || 'Logged out successfully from Keycloak IAM');
      fetchAuditLogs();
    } catch (err: any) {
      setLoginError(err.message || 'Logout failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRefreshToken = async () => {
    if (!tokenResponse?.refresh_token) {
      setLoginError('No active refresh token available to renew.');
      return;
    }
    setLoading(true);
    setLoginError(null);
    try {
      const res = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          refresh_token: tokenResponse.refresh_token
        })
      });

      const data = await res.json();
      if (res.ok) {
        setTokenResponse((prev: any) => ({
          ...prev,
          access_token: data.access_token,
          refresh_token: data.refresh_token,
          expires_in: data.expires_in,
          decoded: data.decoded
        }));
        setLogoutMessage('Token successfully renewed via Keycloak OpenID Connect refresh_token grant.');
      } else {
        setLoginError(data.error || 'Token refresh failed');
      }
      fetchAuditLogs();
    } catch (err: any) {
      setLoginError(err.message || 'Refresh failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRolePreset = (role: 'PATIENT' | 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'LAB_TECH' | 'ADMIN') => {
    setSelectedRole(role);
    setLoginError(null);
    setLogoutMessage(null);

    let u = 'user_pat';
    if (role === 'DOCTOR') u = 'doctor_emily';
    else if (role === 'NURSE') u = 'nurse_sarah';
    else if (role === 'PHARMACIST') u = 'pharm_alex';
    else if (role === 'LAB_TECH') u = 'tech_kevin';
    else if (role === 'ADMIN') u = 'admin_sys';

    setUsername(u);
    setPassword('Password123!');
  };

  const copyToken = () => {
    if (tokenResponse?.access_token) {
      navigator.clipboard.writeText(tokenResponse.access_token);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  useEffect(() => {
    fetchRoles();
    fetchAuditLogs();
    fetchEffectivePermissions(auditUserId);
    handleLogin();
  }, []);

  useEffect(() => {
    fetchRoles();
  }, [roleSearchQuery]);

  const rolesList: Array<'PATIENT' | 'DOCTOR' | 'NURSE' | 'PHARMACIST' | 'LAB_TECH' | 'ADMIN'> = [
    'PATIENT', 'DOCTOR', 'NURSE', 'PHARMACIST', 'LAB_TECH', 'ADMIN'
  ];

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-blue-500/10 border border-blue-500/30 rounded-xl text-blue-400">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">
                Keycloak 24 IAM &amp; Admin SDK Engine (<code className="text-teal-400 font-mono">user-auth-service</code>)
              </h3>
              <p className="text-xs text-slate-400">
                Production-grade Java Keycloak Admin SDK implementation with Role CRUD, Composite Role Trees, User/Group Mappings, and HIPAA Audit Trails.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-mono px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-300">
              Realm: <strong>healthcare-realm</strong>
            </span>
            <span className="text-xs font-mono px-3 py-1.5 rounded-lg bg-blue-500/10 border border-blue-500/30 text-blue-300">
              Port: <strong>8081</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Sub tabs navigation */}
      <div className="flex border-b border-slate-800 space-x-4 overflow-x-auto">
        <button
          onClick={() => setActiveSubTab('roles')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 whitespace-nowrap ${
            activeSubTab === 'roles'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Layers className="w-4 h-4" />
          Roles &amp; Permissions Manager ({roles.length})
        </button>
        <button
          onClick={() => setActiveSubTab('tester')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 whitespace-nowrap ${
            activeSubTab === 'tester'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <LogIn className="w-4 h-4" />
          Login &amp; Logout OIDC API
        </button>
        <button
          onClick={() => setActiveSubTab('audit')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 whitespace-nowrap ${
            activeSubTab === 'audit'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Activity className="w-4 h-4" />
          HIPAA Auth Audit Logs ({auditLogs.length})
        </button>
        <button
          onClick={() => setActiveSubTab('code')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 whitespace-nowrap ${
            activeSubTab === 'code'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <FileCode className="w-4 h-4" />
          Java Admin SDK Architecture
        </button>
      </div>

      {/* Subtab 1: Keycloak Roles & Permissions Management */}
      {activeSubTab === 'roles' && (
        <div className="space-y-6">
          {/* Action Alerts */}
          {roleActionMessage && (
            <div className="p-3 bg-emerald-950/50 border border-emerald-800 rounded-xl text-xs text-emerald-300 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>{roleActionMessage}</span>
              </div>
              <button onClick={() => setRoleActionMessage(null)} className="text-slate-400 hover:text-slate-200 text-xs">
                ✕
              </button>
            </div>
          )}

          {roleActionError && (
            <div className="p-3 bg-rose-950/50 border border-rose-800 rounded-xl text-xs text-rose-300 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{roleActionError}</span>
              </div>
              <button onClick={() => setRoleActionError(null)} className="text-slate-400 hover:text-slate-200 text-xs">
                ✕
              </button>
            </div>
          )}

          {/* Top Controls: Search + Create Role Button */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-500" />
              <input
                type="text"
                value={roleSearchQuery}
                onChange={e => setRoleSearchQuery(e.target.value)}
                placeholder="Search roles (e.g., DOCTOR, ADMIN)..."
                className="w-full pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
              />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <button
                onClick={fetchRoles}
                className="py-1.5 px-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 rounded-lg flex items-center gap-1.5"
              >
                <RefreshCw className="w-3.5 h-3.5 text-teal-400" />
                Refresh
              </button>

              <button
                onClick={() => setIsCreatingRole(!isCreatingRole)}
                className="py-1.5 px-3.5 bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs rounded-lg flex items-center gap-1.5 shadow-sm"
              >
                <Plus className="w-4 h-4" />
                {isCreatingRole ? 'Cancel' : 'Create Role (KeycloakBuilder)'}
              </button>
            </div>
          </div>

          {/* Create Role Form Modal / Panel */}
          {isCreatingRole && (
            <form onSubmit={handleCreateRole} className="bg-slate-900 border border-teal-500/40 rounded-xl p-5 space-y-4 shadow-lg">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h4 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                  <Plus className="w-4 h-4 text-teal-400" />
                  Define New Keycloak IAM Role
                </h4>
                <span className="text-xs text-slate-400 font-mono">POST /api/v1/iam/roles</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Role Name (UPPERCASE alphanumeric)</label>
                  <input
                    type="text"
                    required
                    value={newRoleName}
                    onChange={e => setNewRoleName(e.target.value)}
                    placeholder="e.g. CLINICAL_RESEARCHER"
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                  />
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">HIPAA Clearance Level Attribute</label>
                  <select
                    value={newRoleClearance}
                    onChange={e => setNewRoleClearance(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                  >
                    <option value="LEVEL_1">LEVEL_1 (Standard Patient / Telemetry Access)</option>
                    <option value="LEVEL_2">LEVEL_2 (Nurse / Pharmacist / Lab Tech Access)</option>
                    <option value="LEVEL_3">LEVEL_3 (Attending Physician / Diagnostics Clearance)</option>
                    <option value="LEVEL_4_EXECUTIVE">LEVEL_4_EXECUTIVE (Chief Medical Officer)</option>
                    <option value="LEVEL_4_ROOT">LEVEL_4_ROOT (IAM System Administrator)</option>
                  </select>
                </div>

                <div className="md:col-span-2">
                  <label className="block text-xs text-slate-400 mb-1">Description</label>
                  <input
                    type="text"
                    value={newRoleDesc}
                    onChange={e => setNewRoleDesc(e.target.value)}
                    placeholder="Clinical duties and data access authorization parameters"
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-teal-500"
                  />
                </div>

                <div className="md:col-span-2 space-y-2">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="compositeCheck"
                      checked={newRoleIsComposite}
                      onChange={e => setNewRoleIsComposite(e.target.checked)}
                      className="rounded bg-slate-950 border-slate-700 text-teal-500 focus:ring-0"
                    />
                    <label htmlFor="compositeCheck" className="text-xs font-bold text-slate-200 cursor-pointer">
                      Composite Role (Inherit and aggregate multiple sub-roles)
                    </label>
                  </div>

                  {newRoleIsComposite && (
                    <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 space-y-2">
                      <p className="text-[11px] text-slate-400">Select child roles to automatically inherit:</p>
                      <div className="flex flex-wrap gap-2">
                        {roles.map(r => {
                          const isSelected = selectedSubRoles.includes(r.name);
                          return (
                            <button
                              type="button"
                              key={r.name}
                              onClick={() => {
                                if (isSelected) {
                                  setSelectedSubRoles(selectedSubRoles.filter(s => s !== r.name));
                                } else {
                                  setSelectedSubRoles([...selectedSubRoles, r.name]);
                                }
                              }}
                              className={`px-2.5 py-1 rounded text-xs font-mono font-semibold border transition-all ${
                                isSelected
                                  ? 'bg-teal-500/20 border-teal-500 text-teal-300'
                                  : 'bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200'
                              }`}
                            >
                              {isSelected ? '✓ ' : '+ '}
                              {r.name}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsCreatingRole(false)}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-teal-600 hover:bg-teal-500 text-white rounded-lg text-xs font-semibold shadow-sm"
                >
                  Save Role to Keycloak
                </button>
              </div>
            </form>
          )}

          {/* Roles Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {roles.map(r => {
              const isDefaultRole = ['PATIENT', 'DOCTOR', 'NURSE', 'PHARMACIST', 'LAB_TECH', 'ADMIN'].includes(r.name);
              return (
                <div
                  key={r.id || r.name}
                  className="bg-slate-900 border border-slate-800 hover:border-slate-700 rounded-xl p-5 flex flex-col justify-between space-y-4 shadow-sm transition-all"
                >
                  <div className="space-y-2">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-sm text-slate-100">{r.name}</span>
                        {r.composite && (
                          <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-purple-500/10 border border-purple-500/30 text-purple-300 font-bold">
                            COMPOSITE
                          </span>
                        )}
                      </div>

                      {!isDefaultRole && (
                        <button
                          onClick={() => handleDeleteRole(r.name)}
                          className="p-1 rounded text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition-colors"
                          title="Delete Role"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>

                    <p className="text-xs text-slate-400 leading-relaxed min-h-[36px]">{r.description}</p>

                    {/* Composite sub-roles list */}
                    {r.composite && r.compositeSubRoles && r.compositeSubRoles.length > 0 && (
                      <div className="p-2.5 bg-slate-950 rounded-lg border border-slate-800/80 space-y-1.5">
                        <span className="text-[10px] uppercase font-mono tracking-wider text-slate-500 block">
                          Inherited Sub-Roles:
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {r.compositeSubRoles.map(sub => (
                            <span
                              key={sub}
                              className="text-[10px] font-mono px-2 py-0.5 rounded bg-purple-950/40 border border-purple-800/40 text-purple-300"
                            >
                              {sub}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Custom Attributes */}
                    {r.attributes && Object.keys(r.attributes).length > 0 && (
                      <div className="pt-2 flex flex-wrap gap-1.5">
                        {Object.entries(r.attributes).map(([k, v]) => (
                          <span
                            key={k}
                            className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-950 border border-slate-800 text-slate-400"
                          >
                            <strong className="text-teal-400">{k}</strong>: {Array.isArray(v) ? v.join(', ') : v}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-[11px] font-mono text-slate-500">
                    <span>Container: {r.containerId}</span>
                    <span className="text-emerald-400">ACTIVE</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* User Role Mapping & Effective Auditing Panel */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
              <div>
                <h4 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                  <Users className="w-4 h-4 text-teal-400" />
                  Live User &amp; Group Role Mapping Auditor
                </h4>
                <p className="text-xs text-slate-400">
                  Assign/revoke roles in Keycloak and compute expanded composite permissions hierarchy in real-time.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <select
                  value={auditUserId}
                  onChange={e => {
                    setAuditUserId(e.target.value);
                    fetchEffectivePermissions(e.target.value);
                  }}
                  className="bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                >
                  <option value="usr-doc-204">Dr. Emily Vance (usr-doc-204)</option>
                  <option value="usr-nr-101">Nurse Sarah Jenkins (usr-nr-101)</option>
                  <option value="usr-pat-101">Patient Eleanor Vance (usr-pat-101)</option>
                  <option value="usr-ph-301">Pharmacist Alex Mercer (usr-ph-301)</option>
                  <option value="usr-lab-401">Tech Kevin Park (usr-lab-401)</option>
                  <option value="usr-adm-001">System Administrator (usr-adm-001)</option>
                </select>
              </div>
            </div>

            {/* User Details & Permissions Breakdown */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Assign / Revoke Controls */}
              <div className="lg:col-span-5 space-y-4">
                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-3">
                  <h5 className="text-xs font-bold text-slate-200 flex items-center gap-1.5">
                    <Plus className="w-3.5 h-3.5 text-teal-400" />
                    Assign Role to User
                  </h5>
                  <div className="flex gap-2">
                    <select
                      value={mappingRoleToAssign}
                      onChange={e => setMappingRoleToAssign(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                    >
                      {roles.map(r => (
                        <option key={r.name} value={r.name}>
                          {r.name} {r.composite ? '(COMPOSITE)' : ''}
                        </option>
                      ))}
                    </select>

                    <button
                      onClick={handleAssignRoleToUser}
                      className="px-3.5 py-1.5 bg-teal-600 hover:bg-teal-500 text-white text-xs font-semibold rounded-lg shrink-0"
                    >
                      Assign
                    </button>
                  </div>
                </div>

                {/* Direct Roles List */}
                <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <h5 className="text-xs font-bold text-slate-200">Directly Assigned Realm Roles:</h5>
                    <span className="text-[11px] font-mono text-slate-400">
                      {effectiveAuditData?.directRealmRoles?.length || 0} roles
                    </span>
                  </div>

                  <div className="flex flex-wrap gap-1.5">
                    {effectiveAuditData?.directRealmRoles?.map(role => (
                      <span
                        key={role}
                        className="inline-flex items-center gap-1.5 text-xs font-mono px-2.5 py-1 rounded bg-blue-500/10 border border-blue-500/30 text-blue-300"
                      >
                        {role}
                        <button
                          onClick={() => handleRevokeRoleFromUser(role)}
                          className="text-slate-500 hover:text-rose-400 ml-1"
                          title="Revoke Role"
                        >
                          ✕
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Effective Composite Permissions Inspector */}
              <div className="lg:col-span-7 p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-3">
                <div className="flex items-center justify-between">
                  <h5 className="text-xs font-bold text-purple-300 flex items-center gap-1.5">
                    <Award className="w-3.5 h-3.5" />
                    Effective Composite Permissions Audit
                  </h5>
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                    Total Effective: {effectiveAuditData?.totalEffectiveRolesCount || 0}
                  </span>
                </div>

                <div className="space-y-3">
                  <div>
                    <span className="text-[11px] font-mono text-slate-400 block mb-1">
                      Expanded Realm Permissions (Direct + Composite Inclusions):
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {effectiveAuditData?.compositeEffectiveRealmRoles?.map(r => (
                        <span
                          key={r}
                          className="text-xs font-mono px-2.5 py-1 rounded bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 font-semibold"
                        >
                          ✓ {r}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div>
                    <span className="text-[11px] font-mono text-slate-400 block mb-1">
                      Client-Level Scopes (<code className="text-teal-400">healthcare-api-gateway</code>):
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {effectiveAuditData?.compositeEffectiveClientRoles?.['healthcare-api-gateway']?.map(scope => (
                        <span
                          key={scope}
                          className="text-xs font-mono px-2.5 py-1 rounded bg-slate-900 border border-slate-700 text-slate-300"
                        >
                          scope: {scope}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Subtab 2: Login & Logout Tester */}
      {activeSubTab === 'tester' && (
        <div className="space-y-6">
          {/* Role Preset Quick Selector */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-teal-400" />
                Select RBAC Persona Preset
              </h4>
              <span className="text-xs text-slate-400 font-mono">
                Populates credentials &amp; role policies
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
              {rolesList.map(r => {
                const isSelected = selectedRole === r;
                return (
                  <button
                    key={r}
                    onClick={() => handleRolePreset(r)}
                    className={`py-2.5 px-3 rounded-lg border text-xs font-bold font-mono transition-all text-center ${
                      isSelected
                        ? 'bg-blue-600 border-blue-500 text-white shadow-md'
                        : 'bg-slate-950 border-slate-800 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    {r}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Form & Actions Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Login & Logout Controls */}
            <div className="lg:col-span-5 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                  <Key className="w-4 h-4 text-teal-400" />
                  API Authentication Form
                </h4>
                {tokenResponse?.access_token ? (
                  <span className="px-2 py-0.5 text-[11px] font-mono rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                    SESSION ACTIVE
                  </span>
                ) : (
                  <span className="px-2 py-0.5 text-[11px] font-mono rounded bg-slate-800 text-slate-400 border border-slate-700">
                    LOGGED OUT
                  </span>
                )}
              </div>

              <div className="space-y-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Username / Email</label>
                  <input
                    type="text"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                    placeholder="doctor_emily"
                  />
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">Password</label>
                  <input
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-slate-200 focus:outline-none focus:border-teal-500"
                    placeholder="••••••••••••"
                  />
                </div>

                <div className="p-3 bg-slate-950/80 border border-slate-800 rounded-lg space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-purple-300 flex items-center gap-1.5">
                      <Lock className="w-3.5 h-3.5" />
                      TOTP 2FA Code (6 Digits)
                    </label>
                    <span className="text-[11px] text-slate-400 font-mono">HIPAA Mandatory</span>
                  </div>
                  <input
                    type="text"
                    maxLength={6}
                    value={totpCode}
                    onChange={e => setTotpCode(e.target.value)}
                    className="w-full text-center tracking-widest text-base font-mono font-bold bg-slate-900 border border-slate-700 rounded-lg py-1.5 text-teal-400 focus:outline-none focus:border-teal-500"
                    placeholder="849201"
                  />
                  <p className="text-[11px] text-slate-400">
                    Required for <code className="text-slate-300">doctor_emily</code> and <code className="text-slate-300">admin_sys</code>.
                  </p>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-2 space-y-2">
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={handleLogin}
                    disabled={loading}
                    className="w-full py-2.5 px-3 bg-teal-600 hover:bg-teal-500 text-white font-semibold text-xs rounded-lg transition-all flex items-center justify-center gap-1.5 shadow-sm disabled:opacity-50"
                  >
                    <LogIn className="w-4 h-4" />
                    {loading ? 'Authenticating...' : 'Call POST /api/v1/auth/login'}
                  </button>

                  <button
                    onClick={handleLogout}
                    disabled={loading || !tokenResponse?.access_token}
                    className="w-full py-2.5 px-3 bg-rose-600/90 hover:bg-rose-600 text-white font-semibold text-xs rounded-lg transition-all flex items-center justify-center gap-1.5 shadow-sm disabled:opacity-40"
                  >
                    <LogOut className="w-4 h-4" />
                    Call POST /api/v1/auth/logout
                  </button>
                </div>

                <button
                  onClick={handleRefreshToken}
                  disabled={loading || !tokenResponse?.refresh_token}
                  className="w-full py-2 px-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 font-semibold text-xs rounded-lg transition-all flex items-center justify-center gap-1.5 disabled:opacity-40"
                >
                  <RefreshCw className="w-3.5 h-3.5 text-teal-400" />
                  Call POST /api/v1/auth/refresh (Renew Access Token)
                </button>
              </div>

              {/* Notifications / Errors */}
              {loginError && (
                <div className="p-3 bg-rose-950/40 border border-rose-800 rounded-lg text-xs text-rose-300 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <div>{loginError}</div>
                </div>
              )}

              {logoutMessage && (
                <div className="p-3 bg-emerald-950/40 border border-emerald-800 rounded-lg text-xs text-emerald-300 flex items-start gap-2">
                  <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <div>{logoutMessage}</div>
                </div>
              )}
            </div>

            {/* Token & Session Details */}
            <div className="lg:col-span-7 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                  <Key className="w-4 h-4 text-yellow-400" />
                  Keycloak JWT Token &amp; Session Inspector
                </h4>
                {tokenResponse?.access_token && (
                  <button
                    onClick={copyToken}
                    className="text-xs font-mono px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 flex items-center gap-1"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    {copied ? 'Copied' : 'Copy JWT'}
                  </button>
                )}
              </div>

              {tokenResponse?.decoded ? (
                <div className="space-y-4">
                  <div>
                    <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
                      <span>Decoded Claims (RS256 Payload):</span>
                      <span className="text-[11px] font-mono text-emerald-400">
                        Expires in: {tokenResponse.expires_in}s
                      </span>
                    </div>
                    <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800 font-mono text-xs text-slate-200 max-h-56 overflow-y-auto">
                      <pre className="text-emerald-400 leading-relaxed text-[11px]">
                        {JSON.stringify(tokenResponse.decoded, null, 2)}
                      </pre>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div>
                      <span className="text-[11px] text-slate-400 font-mono">Authorization Header:</span>
                      <div className="bg-slate-950 p-2 rounded border border-slate-800 font-mono text-[11px] text-slate-300 truncate">
                        Bearer {tokenResponse.access_token}
                      </div>
                    </div>

                    {tokenResponse.refresh_token && (
                      <div>
                        <span className="text-[11px] text-slate-400 font-mono">Keycloak Refresh Token:</span>
                        <div className="bg-slate-950 p-2 rounded border border-slate-800 font-mono text-[11px] text-purple-300 truncate">
                          {tokenResponse.refresh_token}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="h-64 flex flex-col items-center justify-center text-center p-6 text-slate-500 space-y-2 bg-slate-950/50 rounded-lg border border-dashed border-slate-800">
                  <Lock className="w-8 h-8 text-slate-600" />
                  <p className="text-xs">No active Keycloak JWT session.</p>
                  <p className="text-[11px] text-slate-600">
                    Click "Call POST /api/v1/auth/login" on the left to authenticate and receive an RS256 Bearer Token.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Subtab 3: Audit Logs */}
      {activeSubTab === 'audit' && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                <Activity className="w-4 h-4 text-teal-400" />
                HIPAA Auth Security &amp; Access Audit Trail
              </h4>
              <p className="text-xs text-slate-400">
                Audited access events logged to PostgreSQL <code className="text-teal-400">auth_audit_logs</code> table.
              </p>
            </div>

            <button
              onClick={fetchAuditLogs}
              className="py-1.5 px-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 rounded-lg flex items-center gap-1"
            >
              <RefreshCw className="w-3.5 h-3.5 text-teal-400" />
              Refresh Logs
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border border-slate-800 rounded-lg overflow-hidden">
              <thead className="bg-slate-950 text-slate-400 uppercase text-[10px] tracking-wider font-mono">
                <tr>
                  <th className="p-3">ID</th>
                  <th className="p-3">User ID</th>
                  <th className="p-3">Action</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">HIPAA Event Type</th>
                  <th className="p-3">Client IP</th>
                  <th className="p-3">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/60 font-mono text-[11px]">
                {auditLogs.length > 0 ? (
                  auditLogs.map(log => (
                    <tr key={log.id} className="hover:bg-slate-800/50 transition-colors">
                      <td className="p-3 text-slate-500">#{log.id}</td>
                      <td className="p-3 text-slate-200 font-semibold">{log.userId}</td>
                      <td className="p-3">
                        <span className={`px-2 py-0.5 rounded font-bold ${
                          log.action.includes('SUCCESS')
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : log.action.includes('LOGOUT')
                            ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                            : log.action.includes('CREATED') || log.action.includes('CHALLENGE')
                            ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                            : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}>
                          {log.action}
                        </span>
                      </td>
                      <td className="p-3">
                        <span className={log.status === 'SUCCESS' ? 'text-emerald-400' : 'text-amber-400'}>
                          {log.status}
                        </span>
                      </td>
                      <td className="p-3 text-slate-400">{log.hipaaEventType}</td>
                      <td className="p-3 text-slate-500">{log.ipAddress}</td>
                      <td className="p-3 text-slate-400">{new Date(log.timestamp).toLocaleTimeString()}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="p-6 text-center text-slate-500">
                      No audit logs found. Try logging in or logging out above.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Subtab 4: Java Code & Architecture Viewer */}
      {activeSubTab === 'code' && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
            <div>
              <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
                <FileCode className="w-4 h-4 text-yellow-400" />
                Spring Boot 3.4 &amp; Keycloak 24 Admin Client SDK Implementation
              </h4>
              <p className="text-xs text-slate-400">
                Inspect Java source files for Role CRUD, Composite Hierarchies, Configuration, and REST Controllers.
              </p>
            </div>

            <div className="flex flex-wrap gap-1.5">
              {[
                { id: 'controller', label: 'RolePermissionController.java' },
                { id: 'serviceImpl', label: 'KeycloakRolePermissionServiceImpl.java' },
                { id: 'service', label: 'KeycloakRolePermissionService.java' },
                { id: 'config', label: 'KeycloakAdminClientConfig.java' },
                { id: 'props', label: 'KeycloakAdminConfigProperties.java' },
                { id: 'dto', label: 'RoleRequestDTO.java' },
                { id: 'exceptions', label: 'GlobalExceptionHandler.java' },
                { id: 'yaml', label: 'application.yml' }
              ].map(file => (
                <button
                  key={file.id}
                  onClick={() => setActiveCodeFile(file.id as any)}
                  className={`px-2.5 py-1 rounded text-xs font-mono font-semibold border transition-all ${
                    activeCodeFile === file.id
                      ? 'bg-blue-600 border-blue-500 text-white'
                      : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {file.label}
                </button>
              ))}
            </div>
          </div>

          <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs overflow-x-auto max-h-[500px]">
            {activeCodeFile === 'controller' && (
              <pre className="text-slate-300 leading-relaxed text-[11px]">
{`package com.healthcare.user.controller;

import com.healthcare.user.dto.*;
import com.healthcare.user.service.KeycloakRolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Keycloak 24 IAM Role, Permission, and Composite Hierarchy Management.
 */
@RestController
@RequestMapping("/api/v1/iam")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionController {

    private final KeycloakRolePermissionService rolePermissionService;

    // 1. Role CRUD Endpoints
    @PostMapping("/roles")
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to create role '{}'", request.getName());
        RoleResponseDTO createdRole = rolePermissionService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getRealmRoles(
            @RequestParam(required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, false, null));
        }
        return ResponseEntity.ok(rolePermissionService.getAllRealmRoles());
    }

    @GetMapping("/roles/client/{clientId}")
    public ResponseEntity<List<RoleResponseDTO>> getClientRoles(
            @PathVariable String clientId,
            @RequestParam(required = false) String query) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(rolePermissionService.searchRoles(query, true, clientId));
        }
        return ResponseEntity.ok(rolePermissionService.getAllClientRoles(clientId));
    }

    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> getRoleByName(
            @PathVariable String roleName,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        return ResponseEntity.ok(rolePermissionService.getRoleByName(roleName, clientRole, clientId));
    }

    @PutMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @PathVariable String roleName,
            @Valid @RequestBody RoleRequestDTO request) {
        log.info("REST: Request to update role '{}'", roleName);
        return ResponseEntity.ok(rolePermissionService.updateRole(roleName, request));
    }

    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Map<String, String>> deleteRole(
            @PathVariable String roleName,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Request to delete role '{}'", roleName);
        rolePermissionService.deleteRole(roleName, clientRole, clientId);
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "Role '" + roleName + "' successfully deleted from Keycloak."
        ));
    }

    // 2. Composite Roles Endpoints
    @PostMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> addSubRolesToComposite(
            @PathVariable String roleName,
            @RequestBody List<String> subRoleNames,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Adding sub-roles {} to parent role '{}'", subRoleNames, roleName);
        return ResponseEntity.ok(rolePermissionService.addSubRolesToComposite(roleName, subRoleNames, clientRole, clientId));
    }

    @DeleteMapping("/roles/{roleName}/composites")
    public ResponseEntity<RoleResponseDTO> removeSubRolesFromComposite(
            @PathVariable String roleName,
            @RequestBody List<String> subRoleNames,
            @RequestParam(defaultValue = "false") boolean clientRole,
            @RequestParam(required = false) String clientId) {
        log.info("REST: Removing sub-roles {} from parent role '{}'", subRoleNames, roleName);
        return ResponseEntity.ok(rolePermissionService.removeSubRolesFromComposite(roleName, subRoleNames, clientRole, clientId));
    }

    // 3. User & Group Role Mappings Endpoints
    @PostMapping("/mappings/users")
    public ResponseEntity<Map<String, Object>> assignRolesToUser(@Valid @RequestBody UserRoleMappingRequestDTO request) {
        log.info("REST: Assigning roles {} to user '{}'", request.getRoleNames(), request.getUserId());
        rolePermissionService.assignRolesToUser(request);
        return ResponseEntity.ok(Map.of(
                "status", "ASSIGNED",
                "userId", request.getUserId(),
                "roles", request.getRoleNames()
        ));
    }

    @DeleteMapping("/mappings/users")
    public ResponseEntity<Map<String, Object>> revokeRolesFromUser(@Valid @RequestBody UserRoleMappingRequestDTO request) {
        log.info("REST: Revoking roles {} from user '{}'", request.getRoleNames(), request.getUserId());
        rolePermissionService.revokeRolesFromUser(request);
        return ResponseEntity.ok(Map.of(
                "status", "REVOKED",
                "userId", request.getUserId(),
                "roles", request.getRoleNames()
        ));
    }

    @GetMapping("/mappings/users/{userId}/effective")
    public ResponseEntity<EffectiveUserPermissionsDTO> getEffectiveUserPermissions(@PathVariable String userId) {
        log.info("REST: Auditing effective permissions for user '{}'", userId);
        return ResponseEntity.ok(rolePermissionService.getEffectiveUserPermissions(userId));
    }
}`}
              </pre>
            )}

            {activeCodeFile === 'serviceImpl' && (
              <pre className="text-slate-300 leading-relaxed text-[11px]">
{`package com.healthcare.user.service.impl;

import com.healthcare.user.config.KeycloakAdminConfigProperties;
import com.healthcare.user.dto.*;
import com.healthcare.user.exception.*;
import com.healthcare.user.service.KeycloakRolePermissionService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakRolePermissionServiceImpl implements KeycloakRolePermissionService {

    private final Keycloak keycloak;
    private final KeycloakAdminConfigProperties properties;

    private RealmResource getRealm() {
        return keycloak.realm(properties.getRealm());
    }

    @Override
    public RoleResponseDTO createRole(RoleRequestDTO request) {
        log.info("Creating role '{}' in Keycloak (ClientRole: {})", request.getName(), request.isClientRole());
        RolesResource rolesResource = getRolesResource(request.isClientRole(), request.getClientId());

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName(request.getName());
        roleRep.setDescription(request.getDescription());
        roleRep.setComposite(request.isComposite());
        roleRep.setClientRole(request.isClientRole());
        if (request.getAttributes() != null) {
            roleRep.setAttributes(request.getAttributes());
        }

        rolesResource.create(roleRep);

        if (request.isComposite() && request.getSubRoleNames() != null && !request.getSubRoleNames().isEmpty()) {
            addSubRolesToComposite(request.getName(), request.getSubRoleNames(), request.isClientRole(), request.getClientId());
        }

        return getRoleByName(request.getName(), request.isClientRole(), request.getClientId());
    }

    @Override
    public List<RoleResponseDTO> getAllRealmRoles() {
        return getRealm().roles().list().stream().map(this::toRoleResponseDTO).collect(Collectors.toList());
    }

    @Override
    public EffectiveUserPermissionsDTO getEffectiveUserPermissions(String userId) {
        UserResource userResource = getRealm().users().get(userId);
        UserRepresentation userRep = userResource.toRepresentation();

        List<String> directRealmRoles = userResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName).collect(Collectors.toList());
        List<String> effectiveRealmRoles = userResource.roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName).collect(Collectors.toList());

        return EffectiveUserPermissionsDTO.builder()
                .userId(userRep.getId())
                .username(userRep.getUsername())
                .email(userRep.getEmail())
                .directRealmRoles(directRealmRoles)
                .compositeEffectiveRealmRoles(effectiveRealmRoles)
                .totalEffectiveRolesCount(effectiveRealmRoles.size())
                .build();
    }
}`}
              </pre>
            )}

            {activeCodeFile === 'config' && (
              <pre className="text-slate-300 leading-relaxed text-[11px]">
{`package com.healthcare.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClientConfig {

    private final KeycloakAdminConfigProperties properties;

    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak Admin Client for server: {}, realm: {}, clientId: {}",
                properties.getServerUrl(), properties.getRealm(), properties.getClientId());

        return KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientSecret(properties.getClientSecret())
                .poolSize(properties.getConnectionPoolSize())
                .build();
    }
}`}
              </pre>
            )}

            {activeCodeFile === 'yaml' && (
              <pre className="text-teal-400 leading-relaxed text-[11px]">
{`# microservices/user-auth-service/src/main/resources/application.yml
keycloak:
  auth-server-url: \${KEYCLOAK_AUTH_SERVER_URL:http://localhost:8080}
  realm: \${KEYCLOAK_REALM:healthcare-realm}
  resource: \${KEYCLOAK_CLIENT_ID:healthcare-api-gateway}
  credentials:
    secret: \${KEYCLOAK_CLIENT_SECRET:healthcare-keycloak-client-secret-2026}
  admin:
    server-url: \${KEYCLOAK_AUTH_SERVER_URL:http://localhost:8080}
    realm: \${KEYCLOAK_REALM:healthcare-realm}
    client-id: \${KEYCLOAK_ADMIN_CLIENT_ID:healthcare-api-gateway}
    client-secret: \${KEYCLOAK_ADMIN_CLIENT_SECRET:gateway-client-secret-rpm-2026}
    master-realm: master
    connection-pool-size: 20`}
              </pre>
            )}

            {activeCodeFile === 'dto' && (
              <pre className="text-slate-300 leading-relaxed text-[11px]">
{`package com.healthcare.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDTO {
    @NotBlank(message = "Role name is required")
    private String name;
    private String description;
    private boolean composite;
    private List<String> subRoleNames;
    private boolean clientRole;
    private String clientId;
    private Map<String, List<String>> attributes;
}`}
              </pre>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
