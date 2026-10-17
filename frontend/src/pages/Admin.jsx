import { Fragment, useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { date, dateTime, money, titleCase } from '../format.js';
import Alert from '../components/Alert.jsx';

export default function Admin() {
  const [stats, setStats] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [open, setOpen] = useState(null);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    try {
      const [s, c, t] = await Promise.all([api('/admin/stats'), api('/admin/customers'), api('/admin/transactions?limit=15')]);
      setStats(s);
      setCustomers(c);
      setTransactions(t);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const changeStatus = async (accountNumber, status) => {
    if (status === 'CLOSED' && !window.confirm(`Close account ${accountNumber}? This cannot be undone.`)) return;
    setError('');
    try {
      await api(`/accounts/${accountNumber}/status`, { method: 'PATCH', body: { status } });
      setNotice(`Account ${accountNumber} is now ${status.toLowerCase()}.`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  const q = query.trim().toLowerCase();
  const filtered = customers.filter(
    (c) =>
      !q ||
      c.fullName.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q) ||
      (c.username || '').includes(q) ||
      c.accounts.some((a) => a.accountNumber.includes(q)),
  );

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Admin console</h1>
          <p className="muted">Customers, accounts and live transactions.</p>
        </div>
        <button className="btn btn-secondary" onClick={load}>
          Refresh
        </button>
      </div>

      <Alert>{error}</Alert>
      <Alert type="success">{notice}</Alert>

      {stats && (
        <section className="stat-grid">
          <div className="stat-card">
            <span>Customers</span>
            <strong>{stats.customers}</strong>
          </div>
          <div className="stat-card">
            <span>Accounts</span>
            <strong>{stats.accounts}</strong>
          </div>
          <div className="stat-card">
            <span>Total deposits</span>
            <strong>{money(stats.totalDeposits)}</strong>
          </div>
          <div className="stat-card">
            <span>Transactions today</span>
            <strong>{stats.transactionsToday}</strong>
          </div>
          <div className="stat-card">
            <span>Frozen accounts</span>
            <strong className={stats.frozenAccounts ? 'amount-out' : ''}>{stats.frozenAccounts}</strong>
          </div>
        </section>
      )}

      <section className="panel">
        <div className="panel-head">
          <h2>Customers</h2>
          <input className="search" placeholder="Search name, email, username or account" value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Username</th>
                <th>Email</th>
                <th>Joined</th>
                <th className="num">Accounts</th>
                <th className="num">Total balance</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <Fragment key={c.id}>
                  <tr className="clickable" onClick={() => setOpen(open === c.id ? null : c.id)}>
                    <td>
                      <span className="caret">{open === c.id ? '▾' : '▸'}</span> {c.fullName}
                    </td>
                    <td>{c.username ? `@${c.username}` : '—'}</td>
                    <td>{c.email}</td>
                    <td>{date(c.createdAt)}</td>
                    <td className="num">{c.accounts.length}</td>
                    <td className="num">{money(c.accounts.reduce((s, a) => s + Number(a.balance), 0))}</td>
                  </tr>
                  {open === c.id && (
                    <tr className="expand-row">
                      <td colSpan={6}>
                        {c.accounts.length === 0 ? (
                          <span className="muted">No accounts.</span>
                        ) : (
                          c.accounts.map((a) => (
                            <div key={a.accountNumber} className="admin-account">
                              <span className="mono">{a.accountNumber}</span>
                              <span>{titleCase(a.accountType)}</span>
                              <span className={`pill pill-${a.status.toLowerCase()}`}>{titleCase(a.status)}</span>
                              <strong>{money(a.balance)}</strong>
                              <span className="admin-actions">
                                {a.status === 'ACTIVE' && (
                                  <button className="btn btn-sm btn-warning" onClick={() => changeStatus(a.accountNumber, 'FROZEN')}>
                                    Freeze
                                  </button>
                                )}
                                {a.status === 'FROZEN' && (
                                  <button className="btn btn-sm btn-secondary" onClick={() => changeStatus(a.accountNumber, 'ACTIVE')}>
                                    Unfreeze
                                  </button>
                                )}
                                {a.status !== 'CLOSED' && (
                                  <button
                                    className="btn btn-sm btn-ghost"
                                    onClick={() => changeStatus(a.accountNumber, 'CLOSED')}
                                    title="Only accounts with zero balance can be closed"
                                  >
                                    Close
                                  </button>
                                )}
                              </span>
                            </div>
                          ))
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted empty">
                    No customers found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>Latest transactions</h2>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Type</th>
                <th>From</th>
                <th>To</th>
                <th>Reference</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.referenceNo}>
                  <td>{dateTime(t.createdAt)}</td>
                  <td>
                    <span className={`pill pill-${t.type.toLowerCase()}`}>{titleCase(t.type)}</span>
                  </td>
                  <td className="mono">{t.fromAccountNumber || '—'}</td>
                  <td className="mono">{t.toAccountNumber || '—'}</td>
                  <td className="mono small">{t.referenceNo}</td>
                  <td className="num">{money(t.amount)}</td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={6} className="muted empty">
                    No transactions yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
