import type { ZoneStats } from '../../types/api';
import { useI18n } from '../../i18n';

interface ZoneStatsListProps {
  zones: ZoneStats[];
}

export function ZoneStatsList({ zones }: ZoneStatsListProps) {
  const { t } = useI18n();

  if (zones.length === 0) {
    return (
      <div className="card">
        <h3>{t('analytics.zoneBreakdown')}</h3>
        <p className="analytics-empty">{t('analytics.noZoneData')}</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>{t('analytics.zoneBreakdown')}</h3>
      <table className="analytics-table">
        <thead>
          <tr>
            <th>{t('common.zone')}</th>
            <th>{t('analytics.completed')}</th>
            <th>{t('analytics.overdue')}</th>
          </tr>
        </thead>
        <tbody>
          {zones.map((zone) => (
            <tr key={zone.zoneId}>
              <td>{zone.zoneName}</td>
              <td>{zone.completedCount}</td>
              <td className={zone.overdueCount > 0 ? 'overdue-highlight' : ''}>
                {zone.overdueCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
