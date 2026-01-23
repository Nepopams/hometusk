import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { STORAGE_KEYS } from '../lib/constants';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  requireHousehold?: boolean;
}

export function ProtectedRoute({ requireHousehold = false }: ProtectedRouteProps) {
  const { status, isAuthenticated, householdId, error } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <div className="page">Loading...</div>;
  }

  if (!isAuthenticated) {
    const intendedPath = location.pathname + location.search;
    if (intendedPath !== '/login' && intendedPath !== '/') {
      sessionStorage.setItem(STORAGE_KEYS.POST_LOGIN_REDIRECT, intendedPath);
    }

    const redirectPath = error ? `/login?error=${error}` : '/login';
    return <Navigate to={redirectPath} state={{ from: location }} replace />;
  }

  if (requireHousehold && !householdId) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}

export { STORAGE_KEYS };
