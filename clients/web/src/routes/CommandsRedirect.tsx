import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function CommandsRedirect() {
  const { householdId } = useAuth();

  return (
    <Navigate
      to={householdId ? `/households/${householdId}/commands` : '/households'}
      replace
    />
  );
}
