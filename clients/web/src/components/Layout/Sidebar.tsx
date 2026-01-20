import { NavLink, useParams } from 'react-router-dom';

const getLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link is-active' : 'nav-link';

export default function Sidebar() {
  const { householdId } = useParams();
  const basePath = `/households/${householdId ?? 'demo'}`;

  return (
    <aside className="app-sidebar">
      <div className="app-sidebar__section">
        <div className="app-sidebar__title">Navigation</div>
        <nav className="app-nav">
          <NavLink className={getLinkClass} to={`${basePath}/tasks`}>
            Tasks
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/zones`}>
            Zones
          </NavLink>
          <NavLink className={getLinkClass} to={`${basePath}/notifications`}>
            Notifications
          </NavLink>
        </nav>
      </div>
    </aside>
  );
}
