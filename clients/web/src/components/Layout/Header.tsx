import { useAuth } from '../../hooks/useAuth';
import HouseholdSwitcher from '../HouseholdSwitcher';
import NotificationBell from '../notifications/NotificationBell';

export default function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="app-header">
      <div className="app-header__title">HomeTusk</div>
      <div className="app-header__meta">
        <HouseholdSwitcher />
        <NotificationBell />
        <span className="chip">{user?.displayName ?? 'User'}</span>
        <button className="ghost-button" type="button" onClick={() => void logout()}>
          Logout
        </button>
      </div>
    </header>
  );
}
