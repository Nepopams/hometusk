import { useAuth } from '../../hooks/useAuth';
import { useI18n } from '../../i18n';
import HouseholdSwitcher from '../HouseholdSwitcher';
import LanguageSwitcher from '../LanguageSwitcher';
import NotificationBell from '../notifications/NotificationBell';

export default function Header() {
  const { user, logout } = useAuth();
  const { t } = useI18n();

  return (
    <header className="app-header">
      <div className="app-header__title">HomeTusk</div>
      <div className="app-header__meta">
        <HouseholdSwitcher />
        <LanguageSwitcher />
        <NotificationBell />
        <span className="chip">{user?.displayName ?? t('header.user')}</span>
        <button className="ghost-button" type="button" onClick={() => void logout()}>
          {t('header.logout')}
        </button>
      </div>
    </header>
  );
}
