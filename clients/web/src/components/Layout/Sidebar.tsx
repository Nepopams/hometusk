import { useState } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import { useI18n } from '../../i18n';
import InviteModal from '../InviteModal';

const getLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link is-active' : 'nav-link';

type SidebarProps = {
  isCompactHidden?: boolean;
  onNavigate?: () => void;
};

export default function Sidebar({ isCompactHidden = false, onNavigate }: SidebarProps) {
  const { householdId } = useParams();
  const { t } = useI18n();
  const basePath = `/households/${householdId ?? 'demo'}`;
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  return (
    <aside
      id="app-household-navigation"
      className="app-sidebar"
      aria-label={t('nav.navigation')}
      aria-hidden={isCompactHidden}
    >
      <div className="app-sidebar__section">
        <div className="app-sidebar__title">{t('nav.navigation')}</div>
        <nav className="app-nav">
          <NavLink end className={getLinkClass} to={basePath} onClick={onNavigate}>
            {t('nav.home')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/tasks`} onClick={onNavigate}>
            {t('nav.tasks')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/routines`} onClick={onNavigate}>
            {t('nav.routines')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/shopping`} onClick={onNavigate}>
            {t('nav.shopping')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/analytics`} onClick={onNavigate}>
            {t('nav.analytics')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/progress`} onClick={onNavigate}>
            {t('nav.progress')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/zones`} onClick={onNavigate}>
            {t('nav.zones')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/notifications`} onClick={onNavigate}>
            {t('nav.notifications')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/members`} onClick={onNavigate}>
            {t('nav.members')}
          </NavLink>
        </nav>
      </div>

      <div className="app-sidebar__section">
        <div className="app-sidebar__title">{t('nav.actions')}</div>
        <div className="app-sidebar__actions">
          <button
            type="button"
            className="ghost-button"
            onClick={() => {
              setIsInviteOpen(true);
              onNavigate?.();
            }}
            disabled={!householdId}
          >
            {t('nav.inviteMember')}
          </button>
        </div>
      </div>

      <InviteModal
        householdId={householdId ?? null}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </aside>
  );
}
