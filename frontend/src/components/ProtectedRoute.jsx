import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

/** Sends visitors who are not logged in (or have the wrong role) back to the right page. */
export default function ProtectedRoute({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return children;
}
