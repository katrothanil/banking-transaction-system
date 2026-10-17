import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import Logo from './Logo.jsx';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const initials = user.fullName
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const links =
    user.role === 'ADMIN'
      ? [{ to: '/admin', label: 'Admin console' }]
      : [
          { to: '/dashboard', label: 'Accounts' },
          { to: '/transfer', label: 'Send money' },
          { to: '/statement', label: 'Statements' },
        ];

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <Logo />
          <nav className="nav">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                {l.label}
              </NavLink>
            ))}
          </nav>
          <div className="user-menu">
            <div className="avatar">{initials}</div>
            <div className="user-text">
              <span className="user-name">{user.fullName}</span>
              <span className="user-role">{user.role === 'ADMIN' ? 'Administrator' : `@${user.username}`}</span>
            </div>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
