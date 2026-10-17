import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, newReference } from '../api.js';
import { dateTime, money, titleCase } from '../format.js';
import Alert from '../components/Alert.jsx';

/** Three steps, like a real banking app: fill in details → review → done. */
export default function Transfer() {
  const [params] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [step, setStep] = useState('form');
  const [form, setForm] = useState({ from: params.get('from') || '', to: '', amount: '', description: '' });
  const [receiver, setReceiver] = useState(null);
  const [reference, setReference] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api('/me/accounts')
      .then((list) => {
        const active = list.filter((a) => a.status === 'ACTIVE');
        setAccounts(active);
        setForm((f) => ({ ...f, from: f.from && active.some((a) => a.accountNumber === f.from) ? f.from : active[0]?.accountNumber || '' }));
      })
      .catch((err) => setError(err.message));
  }, []);

  const source = accounts.find((a) => a.accountNumber === form.from);
  const ownOthers = accounts.filter((a) => a.accountNumber !== form.from);

  const lookupReceiver = async (number = form.to) => {
    setReceiver(null);
    const to = number.trim();
    if (!/^\d{12}$/.test(to)) {
      if (to) setError('Account numbers have 12 digits.');
      return null;
    }
    if (to === form.from) {
      setError('Choose a different account to send to.');
      return null;
    }
    try {
      const holder = await api(`/accounts/${to}/holder`);
      setReceiver(holder);
      setError('');
      return holder;
    } catch (err) {
      setError(err.status === 404 ? `No account found with number ${to}.` : err.message);
      return null;
    }
  };

  const review = async (e) => {
    e.preventDefault();
    setError('');
    const holder = receiver?.accountNumber === form.to.trim() ? receiver : await lookupReceiver();
    if (!holder) return;
    if (Number(form.amount) > Number(source?.balance || 0)) {
      setError(`Amount is more than the available balance of ${money(source.balance)}.`);
      return;
    }
    setReference(newReference('TRF')); // fixed for this review, so retries cannot pay twice
    setStep('review');
  };

  const confirm = async () => {
    setBusy(true);
    setError('');
    try {
      const txn = await api('/transfers', {
        method: 'POST',
        body: {
          fromAccountNumber: form.from,
          toAccountNumber: form.to.trim(),
          amount: Number(form.amount),
          referenceNo: reference,
          description: form.description || undefined,
        },
      });
      const updated = await api(`/accounts/${form.from}`);
      setResult({ txn, balance: updated.balance });
      setStep('done');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const restart = () => {
    setForm({ ...form, to: '', amount: '', description: '' });
    setReceiver(null);
    setResult(null);
    setStep('form');
    api('/me/accounts').then((list) => setAccounts(list.filter((a) => a.status === 'ACTIVE')));
  };

  return (
    <div className="page narrow">
      <div className="page-head">
        <div>
          <h1>Send money</h1>
          <p className="muted">Transfer to any LedgerBank account, instantly.</p>
        </div>
      </div>

      <ol className="steps">
        {['Details', 'Review', 'Done'].map((label, i) => (
          <li key={label} className={['form', 'review', 'done'].indexOf(step) >= i ? 'active' : ''}>
            <span>{i + 1}</span> {label}
          </li>
        ))}
      </ol>

      <Alert>{error}</Alert>

      {step === 'form' && (
        <form className="panel form" onSubmit={review}>
          {accounts.length === 0 && <Alert type="warning">You have no active account to send from.</Alert>}
          <label className="field">
            <span>From</span>
            <select value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} required>
              {accounts.map((a) => (
                <option key={a.accountNumber} value={a.accountNumber}>
                  {titleCase(a.accountType)} · {a.accountNumber} · {money(a.balance)}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>To account number</span>
            <input
              inputMode="numeric"
              maxLength={12}
              placeholder="12-digit account number"
              value={form.to}
              onChange={(e) => {
                setForm({ ...form, to: e.target.value.replace(/\D/g, '') });
                setReceiver(null);
              }}
              onBlur={() => form.to && lookupReceiver()}
              required
            />
            {receiver && <small className="field-ok">✓ Account holder: {receiver.holderName}</small>}
          </label>

          {ownOthers.length > 0 && (
            <div className="quick-pick">
              <span className="muted small">Between your accounts:</span>
              {ownOthers.map((a) => (
                <button
                  type="button"
                  key={a.accountNumber}
                  className="chip"
                  onClick={() => {
                    setForm({ ...form, to: a.accountNumber });
                    lookupReceiver(a.accountNumber);
                  }}
                >
                  {titleCase(a.accountType)} {a.accountNumber.slice(-4)}
                </button>
              ))}
            </div>
          )}

          <div className="row-2">
            <label className="field">
              <span>Amount (₹)</span>
              <input type="number" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required />
            </label>
            <label className="field">
              <span>Note (optional)</span>
              <input maxLength={255} value={form.description} placeholder="Rent for October" onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
          </div>
          <button className="btn btn-primary btn-block" disabled={!accounts.length}>
            Continue
          </button>
        </form>
      )}

      {step === 'review' && (
        <div className="panel">
          <h2>Review transfer</h2>
          <dl className="summary">
            <dt>From</dt>
            <dd>
              {titleCase(source?.accountType)} · {form.from}
            </dd>
            <dt>To</dt>
            <dd>
              {receiver?.holderName} · {form.to}
            </dd>
            <dt>Amount</dt>
            <dd className="summary-amount">{money(form.amount)}</dd>
            {form.description && (
              <>
                <dt>Note</dt>
                <dd>{form.description}</dd>
              </>
            )}
            <dt>Reference</dt>
            <dd className="mono small">{reference}</dd>
          </dl>
          <div className="modal-actions">
            <button className="btn btn-ghost" onClick={() => setStep('form')} disabled={busy}>
              Back
            </button>
            <button className="btn btn-primary" onClick={confirm} disabled={busy}>
              {busy ? 'Sending…' : `Confirm and send ${money(form.amount)}`}
            </button>
          </div>
        </div>
      )}

      {step === 'done' && result && (
        <div className="panel success-panel">
          <div className="success-icon">✓</div>
          <h2>{money(result.txn.amount)} sent</h2>
          <p className="muted">
            To {receiver?.holderName} · {result.txn.toAccountNumber}
          </p>
          <dl className="summary">
            <dt>Reference</dt>
            <dd className="mono small">{result.txn.referenceNo}</dd>
            <dt>Date</dt>
            <dd>{dateTime(result.txn.createdAt)}</dd>
            <dt>New balance</dt>
            <dd>{money(result.balance)}</dd>
          </dl>
          <div className="modal-actions center">
            <Link className="btn btn-ghost" to="/dashboard">
              Back to accounts
            </Link>
            <button className="btn btn-primary" onClick={restart}>
              Send another
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
