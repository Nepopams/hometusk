import { forwardRef } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useI18n } from '../../i18n';
import HouseholdSwitcher from '../HouseholdSwitcher';
import LanguageSwitcher from '../LanguageSwitcher';
import NotificationBell from '../notifications/NotificationBell';

type HeaderProps = {
  isNavigationOpen: boolean;
  onNavigationToggle: () => void;
};

const Header = forwardRef<HTMLElement, HeaderProps>(function Header(
  { isNavigationOpen, onNavigationToggle },
  ref
) {
  const { user, logout } = useAuth();
  const { t } = useI18n();

  return (
    <header ref={ref} className="app-header">
      <div className="app-header__brand">
        <button
          type="button"
          className="app-header__menu-button"
          aria-label={t('nav.navigation')}
          aria-controls="app-household-navigation"
          aria-expanded={isNavigationOpen}
          title={t('nav.navigation')}
          onClick={onNavigationToggle}
        >
          <span className="app-header__menu-line" aria-hidden="true" />
          <span className="app-header__menu-line" aria-hidden="true" />
          <span className="app-header__menu-line" aria-hidden="true" />
        </button>
        <div className="app-header__title">HomeTusk</div>
      </div>
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
});

export default Header;
