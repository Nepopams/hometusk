import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  requireHousehold?: boolean;
}

export function ProtectedRoute({ requireHousehold = false }: ProtectedRouteProps) {
  const { status, isAuthenticated, householdId } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <div className="page">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requireHousehold && !householdId) {
    return <Navigate to="/households" replace />;
  }

  return <Outlet />;
}
