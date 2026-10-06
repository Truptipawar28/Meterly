import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';

const demoUsers = [
  { username: 'client', password: 'client123', label: 'Customer (READER, account 1)' },
  { username: 'admin', password: 'admin123', label: 'Operator (ADMIN)' },
];

export function Login() {
  const { login } = useAuth();
  const [username, setUsername] = useState('client');
  const [password, setPassword] = useState('client123');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(username, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <form className="card login-card" onSubmit={submit}>
        <h1>
          <span className="logo">◔</span> Meterly
        </h1>
        <p className="muted">Billing portal</p>
        <label>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" className="primary" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
        <div className="demo">
          <span className="muted">Demo users:</span>
          {demoUsers.map((u) => (
            <button
              type="button"
              key={u.username}
              className="link"
              onClick={() => {
                setUsername(u.username);
                setPassword(u.password);
              }}
            >
              {u.label}
            </button>
          ))}
        </div>
      </form>
    </div>
  );
}
