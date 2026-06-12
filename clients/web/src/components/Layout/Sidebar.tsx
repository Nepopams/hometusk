import { useState } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import { useI18n } from '../../i18n';
import InviteModal from '../InviteModal';

const getLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link is-active' : 'nav-link';

export default function Sidebar() {
  const { householdId } = useParams();
  const { t } = useI18n();
  const basePath = `/households/${householdId ?? 'demo'}`;
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  return (
    <aside className="app-sidebar">
      <div className="app-sidebar__section">
        <div className="app-sidebar__title">{t('nav.navigation')}</div>
        <nav className="app-nav">
          <NavLink className={getLinkClass} to={`${basePath}/tasks`}>
            {t('nav.tasks')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/routines`}>
            {t('nav.routines')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/analytics`}>
            {t('nav.analytics')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/progress`}>
            {t('nav.progress')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/zones`}>
            {t('nav.zones')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/notifications`}>
            {t('nav.notifications')}
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/members`}>
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
            onClick={() => setIsInviteOpen(true)}
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
