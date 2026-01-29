import { useState } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import InviteModal from '../InviteModal';

const getLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link is-active' : 'nav-link';

export default function Sidebar() {
  const { householdId } = useParams();
  const basePath = `/households/${householdId ?? 'demo'}`;
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  return (
    <aside className="app-sidebar">
      <div className="app-sidebar__section">
        <div className="app-sidebar__title">Navigation</div>
        <nav className="app-nav">
          <NavLink className={getLinkClass} to={`${basePath}/tasks`}>
            Tasks
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/routines`}>
            Routines
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/analytics`}>
            Analytics
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/progress`}>
            Progress
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/zones`}>
            Zones
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/notifications`}>
            Notifications
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/members`}>
            Members
          </NavLink>
        </nav>
      </div>

      <div className="app-sidebar__section">
        <div className="app-sidebar__title">Actions</div>
        <div className="app-sidebar__actions">
          <button
            type="button"
            className="ghost-button"
            onClick={() => setIsInviteOpen(true)}
            disabled={!householdId}
          >
            + Invite Member
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
