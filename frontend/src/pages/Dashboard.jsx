import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, newReference } from '../api.js';
import { useAuth } from '../auth.jsx';
import { money, titleCase } from '../format.js';
import ActivityList from '../components/ActivityList.jsx';
import Alert from '../components/Alert.jsx';
import Modal from '../components/Modal.jsx';

export default function Dashboard() {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [selected, setSelected] = useState(null);
  const [activity, setActivity] = useState([]);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null); // 'deposit' | 'withdraw' | 'open'
  const [notice, setNotice] = useState('');

  const loadAccounts = useCallback(async () => {
    try {
      const list = await api('/me/accounts');
      setAccounts(list);
      setSelected((current) => list.find((a) => a.accountNumber === current?.accountNumber) || list[0] || null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  const loadActivity = useCallback(async (accountNumber) => {
    try {
      setActivity(await api(`/accounts/${accountNumber}/transactions?limit=8`));
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    if (selected) loadActivity(selected.accountNumber);
  }, [selected, loadActivity]);

  const refresh = async (message) => {
    setModal(null);
    setNotice(message);
    await loadAccounts(); // the selected account object changes, which reloads its activity
  };

  const total = accounts.reduce((sum, a) => sum + Number(a.balance), 0);
  const firstName = user.fullName.split(' ')[0];

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Hello, {firstName}</h1>
          <p className="muted">Here is an overview of your money.</p>
        </div>
        <button className="btn btn-secondary" onClick={() => setModal('open')}>
          + Open new account
        </button>
      </div>

      <Alert>{error}</Alert>
      <Alert type="success">{notice}</Alert>

      <section className="hero-card">
        <span className="hero-label">Total balance</span>
        <span className="hero-value">{money(total)}</span>
        <span className="hero-sub">
          across {accounts.length} account{accounts.length === 1 ? '' : 's'}
        </span>
      </section>

      <section className="account-grid">
        {accounts.map((a) => (
          <button
            key={a.accountNumber}
            className={selected?.accountNumber === a.accountNumber ? 'account-card selected' : 'account-card'}
            onClick={() => setSelected(a)}
          >
            <div className="account-card-top">
              <span className="pill">{titleCase(a.accountType)}</span>
              {a.status !== 'ACTIVE' && <span className={`pill pill-${a.status.toLowerCase()}`}>{titleCase(a.status)}</span>}
            </div>
            <span className="account-number">{a.accountNumber.replace(/(\d{4})(?=\d)/g, '$1 ')}</span>
            <span className="account-balance">{money(a.balance)}</span>
          </button>
        ))}
      </section>

      {selected && (
        <section className="panel">
          <div className="panel-head">
            <div>
              <h2>{titleCase(selected.accountType)} account</h2>
              <span className="muted">No. {selected.accountNumber}</span>
            </div>
            <div className="actions">
              <button className="btn btn-secondary" onClick={() => setModal('deposit')} disabled={selected.status !== 'ACTIVE'}>
                Add money
              </button>
              <button className="btn btn-secondary" onClick={() => setModal('withdraw')} disabled={selected.status !== 'ACTIVE'}>
                Withdraw
              </button>
              <Link className="btn btn-primary" to={`/transfer?from=${selected.accountNumber}`}>
                Send money
              </Link>
            </div>
          </div>
          {selected.status !== 'ACTIVE' && (
            <Alert type="warning">This account is {selected.status.toLowerCase()} by the bank. Money cannot move in or out.</Alert>
          )}
          <div className="panel-sub">
            <h3>Recent activity</h3>
            <Link to={`/statement?account=${selected.accountNumber}`}>View statement →</Link>
          </div>
          <ActivityList lines={activity} />
        </section>
      )}

      {(modal === 'deposit' || modal === 'withdraw') && selected && (
        <AmountModal
          kind={modal}
          account={selected}
          onClose={() => setModal(null)}
          onDone={(txn) =>
            refresh(`${modal === 'deposit' ? 'Added' : 'Withdrew'} ${money(txn.amount)} ${modal === 'deposit' ? 'to' : 'from'} ${selected.accountNumber}.`)
          }
        />
      )}
      {modal === 'open' && (
        <OpenAccountModal onClose={() => setModal(null)} onDone={(acc) => refresh(`New ${titleCase(acc.accountType)} account ${acc.accountNumber} opened.`)} />
      )}
    </div>
  );
}

function AmountModal({ kind, account, onClose, onDone }) {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  // One reference per modal: double-clicking "Confirm" can never deposit twice
  const [reference] = useState(() => newReference(kind === 'deposit' ? 'DEP' : 'WDL'));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const txn = await api(`/accounts/${account.accountNumber}/${kind}`, {
        method: 'POST',
        body: { amount: Number(amount), referenceNo: reference, description: description || undefined },
      });
      onDone(txn);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <Modal title={kind === 'deposit' ? 'Add money' : 'Withdraw money'} onClose={onClose}>
      <form onSubmit={submit} className="form">
        <Alert>{error}</Alert>
        <p className="muted">
          {kind === 'deposit' ? 'To' : 'From'} account {account.accountNumber} · Balance {money(account.balance)}
        </p>
        <label className="field">
          <span>Amount (₹)</span>
          <input type="number" min="0.01" step="0.01" autoFocus value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </label>
        <label className="field">
          <span>Note (optional)</span>
          <input value={description} maxLength={255} onChange={(e) => setDescription(e.target.value)} placeholder={kind === 'deposit' ? 'Salary' : 'ATM cash'} />
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? 'Processing…' : kind === 'deposit' ? 'Add money' : 'Withdraw'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function OpenAccountModal({ onClose, onDone }) {
  const [type, setType] = useState('SAVINGS');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      onDone(await api('/me/accounts', { method: 'POST', body: { accountType: type } }));
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <Modal title="Open a new account" onClose={onClose}>
      <form onSubmit={submit} className="form">
        <Alert>{error}</Alert>
        <div className="choice-grid">
          {[
            ['SAVINGS', 'Savings', 'For saving money and everyday use'],
            ['CURRENT', 'Current', 'For business and frequent transactions'],
          ].map(([value, label, text]) => (
            <label key={value} className={type === value ? 'choice selected' : 'choice'}>
              <input type="radio" name="type" value={value} checked={type === value} onChange={() => setType(value)} />
              <strong>{label}</strong>
              <span className="muted small">{text}</span>
            </label>
          ))}
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? 'Opening…' : 'Open account'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
