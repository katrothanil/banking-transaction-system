import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { date, dateTime, isoDate, money, titleCase } from '../format.js';
import Alert from '../components/Alert.jsx';

export default function Statement() {
  const [params] = useSearchParams();
  const today = new Date();
  const [accounts, setAccounts] = useState([]);
  const [account, setAccount] = useState(params.get('account') || '');
  const [from, setFrom] = useState(isoDate(new Date(today.getFullYear(), today.getMonth(), 1)));
  const [to, setTo] = useState(isoDate(today));
  const [statement, setStatement] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api('/me/accounts')
      .then((list) => {
        setAccounts(list);
        setAccount((current) => current || list[0]?.accountNumber || '');
      })
      .catch((err) => setError(err.message));
  }, []);

  const load = async (e) => {
    e?.preventDefault();
    if (!account) return;
    setBusy(true);
    setError('');
    try {
      setStatement(await api(`/accounts/${account}/statement?from=${from}&to=${to}`));
    } catch (err) {
      setError(err.message);
      setStatement(null);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (account) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account]);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Account statement</h1>
          <p className="muted">Every credit and debit with the running balance.</p>
        </div>
        {statement && (
          <button className="btn btn-secondary no-print" onClick={() => window.print()}>
            Print / Save PDF
          </button>
        )}
      </div>

      <form className="panel filter-bar no-print" onSubmit={load}>
        <label className="field">
          <span>Account</span>
          <select value={account} onChange={(e) => setAccount(e.target.value)}>
            {accounts.map((a) => (
              <option key={a.accountNumber} value={a.accountNumber}>
                {titleCase(a.accountType)} · {a.accountNumber}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>From</span>
          <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="field">
          <span>To</span>
          <input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} />
        </label>
        <button className="btn btn-primary" disabled={busy || !account}>
          {busy ? 'Loading…' : 'Show'}
        </button>
      </form>

      <Alert>{error}</Alert>

      {statement && (
        <section className="panel statement">
          <div className="statement-head">
            <div>
              <h2>Account {statement.accountNumber}</h2>
              <span className="muted">
                {date(statement.from)} – {date(statement.to)}
              </span>
            </div>
          </div>
          <div className="stat-row">
            <div className="stat">
              <span>Opening balance</span>
              <strong>{money(statement.openingBalance)}</strong>
            </div>
            <div className="stat">
              <span>Money in</span>
              <strong className="amount-in">+ {money(statement.totalCredits)}</strong>
            </div>
            <div className="stat">
              <span>Money out</span>
              <strong className="amount-out">− {money(statement.totalDebits)}</strong>
            </div>
            <div className="stat">
              <span>Closing balance</span>
              <strong>{money(statement.closingBalance)}</strong>
            </div>
          </div>

          {statement.entries.length === 0 ? (
            <p className="muted empty">No transactions in this period.</p>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Reference</th>
                    <th className="num">Debit</th>
                    <th className="num">Credit</th>
                    <th className="num">Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {statement.entries.map((line) => (
                    <tr key={`${line.referenceNo}-${line.entryType}`}>
                      <td>{dateTime(line.date)}</td>
                      <td>{line.description || titleCase(line.transactionType)}</td>
                      <td className="mono small">{line.referenceNo}</td>
                      <td className="num amount-out">{line.entryType === 'DEBIT' ? money(line.amount) : ''}</td>
                      <td className="num amount-in">{line.entryType === 'CREDIT' ? money(line.amount) : ''}</td>
                      <td className="num">{money(line.balanceAfter)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
