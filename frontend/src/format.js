const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

export const money = (value) => inr.format(Number(value || 0));

export const dateTime = (value) =>
  value
    ? new Date(value).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : '';

export const date = (value) =>
  value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '';

/** "100000000001" -> "XXXX XXXX 0001" style masking for display. */
export const maskAccount = (number) => (number ? `•••• ${number.slice(-4)}` : '');

export const titleCase = (text) => (text ? text.charAt(0) + text.slice(1).toLowerCase() : '');

/** yyyy-mm-dd in local time, for <input type="date">. */
export const isoDate = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};
