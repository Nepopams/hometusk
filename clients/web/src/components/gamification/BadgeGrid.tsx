import type { Badge } from '../../types/api';
import { useI18n } from '../../i18n';

interface BadgeGridProps {
  badges: Badge[];
  title?: string;
  emptyLabel?: string;
}

const ICONS: Record<string, string> = {
  star: 'STAR',
  trophy: 'TROPHY',
  fire: 'FIRE',
  target: 'TARGET',
  clock: 'CLOCK',
  check: 'CHECK',
  lightning: 'BOLT',
};

export function BadgeGrid({ badges, title, emptyLabel = 'No badges yet' }: BadgeGridProps) {
  const { t } = useI18n();
  const resolvedEmptyLabel = emptyLabel === 'No badges yet' ? t('progress.noBadgesYet') : emptyLabel;

  return (
    <div className="badge-grid">
      {title && <h3 className="badge-grid__title">{title}</h3>}
      {badges.length === 0 ? (
        <p className="badge-grid__empty">{resolvedEmptyLabel}</p>
      ) : (
        <div className="badge-grid__items">
          {badges.map((badge) => (
            <div
              key={badge.code}
              className={`badge-card ${badge.earned ? 'badge-card--earned' : ''}`}
              title={badge.description}
            >
              <span className="badge-card__icon">{ICONS[badge.iconName] ?? 'STAR'}</span>
              <span className="badge-card__name">{badge.name}</span>
              {badge.criteria && <span className="badge-card__criteria">{badge.criteria}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
