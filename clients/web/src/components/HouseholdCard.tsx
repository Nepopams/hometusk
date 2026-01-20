import type { HouseholdSummary } from '../types/api';

interface HouseholdCardProps {
  household: HouseholdSummary;
  onSelect: () => void;
}

export default function HouseholdCard({ household, onSelect }: HouseholdCardProps) {
  return (
    <div className="card household-card" onClick={onSelect}>
      <h3>{household.name}</h3>
      <p className="chip">{household.role}</p>
      <button className="button" type="button">
        Select
      </button>
    </div>
  );
}
