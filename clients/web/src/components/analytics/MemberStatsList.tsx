import type { MemberStats } from '../../types/api';
import { useI18n } from '../../i18n';

interface MemberStatsListProps {
  members: MemberStats[];
}

export function MemberStatsList({ members }: MemberStatsListProps) {
  const { t } = useI18n();

  if (members.length === 0) {
    return (
      <div className="card">
        <h3>{t('analytics.memberContributions')}</h3>
        <p className="analytics-empty">{t('analytics.noMemberData')}</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>{t('analytics.memberContributions')}</h3>
      <table className="analytics-table">
        <thead>
          <tr>
            <th>{t('analytics.member')}</th>
            <th>{t('analytics.completed')}</th>
            <th>{t('analytics.open')}</th>
            <th>{t('analytics.overdue')}</th>
          </tr>
        </thead>
        <tbody>
          {members.map((member) => (
            <tr key={member.memberId}>
              <td>{member.memberName}</td>
              <td>{member.completedCount}</td>
              <td>{member.openCount}</td>
              <td className={member.overdueCount > 0 ? 'overdue-highlight' : ''}>
                {member.overdueCount}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
