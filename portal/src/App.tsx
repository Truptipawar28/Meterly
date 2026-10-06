import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from './api';
import { useAuth } from './auth';
import { Login } from './components/Login';
import { Dashboard } from './components/Dashboard';
import { Invoices } from './components/Invoices';

type Tab = 'usage' | 'invoices';

export function App() {
  const { session, logout } = useAuth();
  if (!session) return <Login />;

  return (
    <div className="layout">
      <header className="topbar">
        <div className="brand">
          <span className="logo">◔</span> Meterly
        </div>
        <div className="user">
          <span>
            {session.username} <span className="badge">{session.roles.join(', ')}</span>
          </span>
          <button className="link" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>
      <Main />
    </div>
  );
}

function Main() {
  const { session } = useAuth();
  const isAdmin = session!.roles.includes('ADMIN');
  const [tab, setTab] = useState<Tab>('usage');
  const [pickedAccount, setPickedAccount] = useState<number | null>(null);

  // Admins choose an account; a READER is fixed to the account in their token.
  const accounts = useQuery({
    queryKey: ['accounts'],
    queryFn: () => api.accounts(session!.token),
    enabled: isAdmin,
  });
  const accountId = isAdmin ? (pickedAccount ?? accounts.data?.[0]?.id) : session!.accountId;

  if (!isAdmin && !session!.roles.includes('READER')) {
    return <p className="notice">This user ({session!.roles.join(', ')}) can record usage but has no dashboard.</p>;
  }

  return (
    <main className="content">
      <div className="toolbar">
        <nav className="tabs">
          <button className={tab === 'usage' ? 'active' : ''} onClick={() => setTab('usage')}>
            Usage &amp; cost
          </button>
          <button className={tab === 'invoices' ? 'active' : ''} onClick={() => setTab('invoices')}>
            Invoices
          </button>
        </nav>
        {isAdmin && accounts.data && (
          <select value={accountId ?? ''} onChange={(e) => setPickedAccount(Number(e.target.value))}>
            {accounts.data.map((a) => (
              <option key={a.id} value={a.id}>
                #{a.id} {a.name}
              </option>
            ))}
          </select>
        )}
      </div>

      {accountId == null ? (
        <p className="notice">{accounts.isLoading ? 'Loading accounts…' : 'No accounts yet.'}</p>
      ) : tab === 'usage' ? (
        <Dashboard accountId={accountId} />
      ) : (
        <Invoices accountId={accountId} />
      )}
    </main>
  );
}
