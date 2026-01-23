import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMembers } from '../hooks/useMembers';
import InviteModal from '../components/InviteModal';
import { ApiError } from '../lib/errors';

function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export default function Members() {
  const { householdId } = useParams();
  const { members, isLoading, error } = useMembers(householdId);
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="page members">
        <div className="members__loading">Loading members...</div>
      </div>
    );
  }

  if (error) {
    const is403 = error instanceof ApiError && error.status === 403;
    return (
      <div className="page members">
        <div className="members__error">
          <h2>{is403 ? 'Access Denied' : 'Error'}</h2>
          <p>{is403 ? 'You are not a member of this household.' : 'Failed to load members.'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page members">
      <div className="members__header">
        <h1>Members ({members.length})</h1>
        <button type="button" className="button" onClick={() => setIsInviteOpen(true)}>
          Invite Member
        </button>
      </div>

      {members.length === 0 ? (
        <div className="members__empty">
          <p>No members found.</p>
        </div>
      ) : (
        <div className="members__table-wrapper">
          <table className="members__table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Joined</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => (
                <tr key={member.userId}>
                  <td>{member.displayName}</td>
                  <td>{member.email}</td>
                  <td>
                    <span className={`members__role members__role--${member.role}`}>
                      {member.role}
                    </span>
                  </td>
                  <td>{formatDate(member.joinedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <InviteModal
        householdId={householdId ?? null}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </div>
  );
}
