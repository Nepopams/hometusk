import type { ZoneStats } from '../../types/api';

interface ZoneStatsListProps {
  zones: ZoneStats[];
}

export function ZoneStatsList({ zones }: ZoneStatsListProps) {
  if (zones.length === 0) {
    return (
      <div className="card">
        <h3>Zone Breakdown</h3>
        <p className="analytics-empty">No zone data available.</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Zone Breakdown</h3>
      <table className="analytics-table">
        <thead>
          <tr>
            <th>Zone</th>
            <th>Completed</th>
            <th>Overdue</th>
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
