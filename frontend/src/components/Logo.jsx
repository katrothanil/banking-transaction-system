export default function Logo({ light = false }) {
  return (
    <div className={light ? 'logo logo-light' : 'logo'}>
      <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true">
        <rect width="32" height="32" rx="8" fill={light ? '#ffffff' : '#1e3a8a'} />
        <path
          d="M8 13h16L16 7zM9 15h3v8H9zm5.5 0h3v8h-3zM20 15h3v8h-3zM7 24h18v2H7z"
          fill={light ? '#1e3a8a' : '#ffffff'}
        />
      </svg>
      <span>LedgerBank</span>
    </div>
  );
}
