import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import HouseholdCard from '../components/HouseholdCard';
import { useAuth } from '../hooks/useAuth';

export default function HouseholdSelector() {
  const { user, selectHousehold } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user && user.households.length === 1) {
      const hid = user.households[0].id;
      selectHousehold(hid);
      navigate(`/households/${hid}/tasks`, { replace: true });
    }
  }, [user, selectHousehold, navigate]);

  if (!user || user.households.length === 0) {
    return (
      <div className="page empty-state">
        <h1>Welcome to HomeTusk!</h1>
        <p>You don&apos;t have any households yet.</p>
        <button className="button" type="button" onClick={() => navigate('/households/new')}>
          Create your first household
        </button>
      </div>
    );
  }

  const handleSelect = (id: string) => {
    selectHousehold(id);
    navigate(`/households/${id}/tasks`);
  };

  return (
    <div className="page">
      <h1>Select Household</h1>
      <p>Choose a household to continue:</p>

      <div className="household-list">
        {user.households.map((household) => (
          <HouseholdCard
            key={household.id}
            household={household}
            onSelect={() => handleSelect(household.id)}
          />
        ))}
      </div>
    </div>
  );
}
