import Logo from './Logo.jsx';

/** Split screen used by the login and sign-up pages. */
export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth">
      <aside className="auth-brand">
        <Logo light />
        <div className="auth-brand-copy">
          <h1>Banking that never loses a rupee.</h1>
          <p>
            Every transfer runs in a single database transaction with row-level locking, a double-entry ledger and a
            tamper-proof audit trail.
          </p>
          <ul className="auth-points">
            <li>Instant transfers between accounts</li>
            <li>Safe retries: a payment is never sent twice</li>
            <li>Statements with opening and closing balances</li>
          </ul>
        </div>
        <span className="auth-foot">Demo project · Spring Boot · PostgreSQL · React</span>
      </aside>
      <section className="auth-panel">
        <div className="auth-card">
          <h2>{title}</h2>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </section>
    </div>
  );
}
