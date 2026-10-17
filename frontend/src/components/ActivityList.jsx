import { dateTime, money, titleCase } from '../format.js';

/** Ledger lines: money in (CREDIT) shown green with +, money out (DEBIT) with −. */
export default function ActivityList({ lines, empty = 'No transactions yet.' }) {
  if (!lines?.length) return <p className="muted empty">{empty}</p>;
  return (
    <ul className="activity">
      {lines.map((line) => {
        const credit = line.entryType === 'CREDIT';
        return (
          <li key={`${line.referenceNo}-${line.entryType}`} className="activity-row">
            <div className={credit ? 'activity-icon in' : 'activity-icon out'}>{credit ? '↓' : '↑'}</div>
            <div className="activity-main">
              <span className="activity-title">{line.description || titleCase(line.transactionType)}</span>
              <span className="activity-sub">
                {titleCase(line.transactionType)} · {dateTime(line.date)}
              </span>
            </div>
            <div className="activity-amount">
              <span className={credit ? 'amount-in' : 'amount-out'}>
                {credit ? '+' : '−'} {money(line.amount)}
              </span>
              <span className="activity-sub">Bal {money(line.balanceAfter)}</span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
