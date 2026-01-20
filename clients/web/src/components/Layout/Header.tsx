import { useParams } from 'react-router-dom';

export default function Header() {
  const { householdId } = useParams();

  return (
    <header className="app-header">
      <div className="app-header__title">HomeTusk</div>
      <div className="app-header__meta">
        <span className="chip">Household: {householdId ?? 'demo'}</span>
        <span className="chip">User: demo</span>
        <button className="ghost-button" type="button" disabled>
          Logout
        </button>
      </div>
    </header>
  );
}
