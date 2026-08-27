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
  AlertCircle
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
  const [activeSubTab, setActiveSubTab] = useState<'tester' | 'audit' | 'code'>('tester');

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
    handleLogin();
    fetchAuditLogs();
  }, []);

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
                User &amp; IAM Service (<code className="text-teal-400 font-mono">user-auth-service</code>)
              </h3>
              <p className="text-xs text-slate-400">
                Keycloak 24 OIDC Login &amp; Logout APIs, JWT claim extraction, TOTP 2FA multi-factor authentication, and HIPAA audit logging.
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
      <div className="flex border-b border-slate-800 space-x-4">
        <button
          onClick={() => setActiveSubTab('tester')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 ${
            activeSubTab === 'tester'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <LogIn className="w-4 h-4" />
          Interactive Login &amp; Logout API Tester
        </button>
        <button
          onClick={() => setActiveSubTab('audit')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 ${
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
          className={`pb-3 text-sm font-semibold flex items-center gap-2 transition-colors border-b-2 ${
            activeSubTab === 'code'
              ? 'border-teal-500 text-teal-400'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <FileCode className="w-4 h-4" />
          Spring Boot Java Implementation
        </button>
      </div>

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
                  {/* Decoded Claims */}
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

                  {/* Tokens Info */}
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

      {/* Audit Logs Sub Tab */}
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
                            : log.action.includes('CHALLENGE')
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

      {/* Code Viewer Sub Tab */}
      {activeSubTab === 'code' && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div>
            <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <FileCode className="w-4 h-4 text-yellow-400" />
              Spring Boot 3.4 &amp; Keycloak 24 REST Controller Implementation
            </h4>
            <p className="text-xs text-slate-400">
              Complete source code for <code className="text-teal-400">AuthController.java</code> and <code className="text-teal-400">KeycloakAuthService.java</code> in <code className="text-slate-300">microservices/user-auth-service/</code>.
            </p>
          </div>

          <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs overflow-x-auto">
            <pre className="text-slate-300 leading-relaxed text-[11px]">
{`// microservices/user-auth-service/src/main/java/com/healthcare/user/controller/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final TotpService totpService;
    private final AuthAuditLogRepository authAuditLogRepository;

    /**
     * Authenticate user with Keycloak Direct Access Grants & TOTP 2FA enforcement
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest) {
        
        loginRequest.setClientIp(httpRequest.getRemoteAddr());
        loginRequest.setUserAgent(httpRequest.getHeader("User-Agent"));

        LoginResponse response = keycloakAuthService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Keycloak Single Sign-Out: Revokes the refresh token and terminates the Keycloak session
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @Valid @RequestBody LogoutRequest logoutRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        String userId = (jwt != null) ? jwt.getSubject() : null;
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        Map<String, Object> result = keycloakAuthService.logout(logoutRequest, userId, clientIp, userAgent);
        return ResponseEntity.ok(result);
    }

    /**
     * Refresh token endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = keycloakAuthService.refreshToken(refreshRequest, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }
}`}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
};
