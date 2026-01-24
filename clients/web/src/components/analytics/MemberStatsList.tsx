import type { MemberStats } from '../../types/api';

interface MemberStatsListProps {
  members: MemberStats[];
}

export function MemberStatsList({ members }: MemberStatsListProps) {
  if (members.length === 0) {
    return (
      <div className="card">
        <h3>Member Contributions</h3>
        <p className="analytics-empty">No member data available.</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h3>Member Contributions</h3>
      <table className="analytics-table">
        <thead>
          <tr>
            <th>Member</th>
            <th>Completed</th>
            <th>Open</th>
            <th>Overdue</th>
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
