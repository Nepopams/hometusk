import type { HouseholdSummary } from '../types/api';
import { useI18n } from '../i18n';

interface HouseholdCardProps {
  household: HouseholdSummary;
  onSelect: () => void;
}

export default function HouseholdCard({ household, onSelect }: HouseholdCardProps) {
  const { t } = useI18n();

  return (
    <div className="card household-card" onClick={onSelect}>
      <h3>{household.name}</h3>
      <p className="chip">{household.role}</p>
      <button className="button" type="button">
        {t('household.select')}
      </button>
    </div>
  );
}
