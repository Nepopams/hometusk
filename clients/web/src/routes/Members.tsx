import { useState, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useMembers } from '../hooks/useMembers';
import { useI18n } from '../i18n';
import { Button } from '../components/ui';
import InviteModal from '../components/InviteModal';
import { ApiError } from '../lib/errors';
import type { HouseholdMember } from '../types/api';
import './Members.css';

/**
 * Members page for viewing household members and inviting new ones.
 *
 * States:
 * - Loading: skeleton placeholders in card
 * - Empty: centered icon + title + desc + CTA
 * - Error: warning banner with retry
 * - Normal: members list card with dividers
 *
 * Pattern follows Invites page (TmZLg, gCEwX, ZWWj9).
 * @see Pencil frames: TmZLg (list), gCEwX (empty), ZWWj9 (mobile)
 */
export default function Members() {
  const { t, formatRelativeTime } = useI18n();
  const { householdId } = useAuth();
  const { members, isLoading, error, refetch } = useMembers(householdId);

  const [isInviteOpen, setIsInviteOpen] = useState(false);

  const handleRetry = useCallback(() => {
    refetch();
  }, [refetch]);

  if (!householdId) {
    return (
      <div className="members">
        <div className="members__wrapper">
          <div className="members__empty-page">
            <p>{t('members.noHousehold')}</p>
          </div>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="members">
        <div className="members__wrapper">
          <section className="members__section">
            <div className="members__section-header">
              <h2 className="members__section-title">{t('members.title')}</h2>
              <Button variant="primary" size="sm" disabled>
                {t('members.invite')}
              </Button>
            </div>
            <div className="members__card">
              {[1, 2, 3].map((i, idx) => (
                <div key={i}>
                  {idx > 0 && <div className="members__divider" />}
                  <div className="members__skeleton">
                    <div className="members__skeleton-avatar" />
                    <div className="members__skeleton-content">
                      <div className="members__skeleton-line" />
                      <div className="members__skeleton-line members__skeleton-line--short" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    const is403 = error instanceof ApiError && error.status === 403;
    return (
      <div className="members">
        <div className="members__wrapper">
          <section className="members__section">
            <div className="members__section-header">
              <h2 className="members__section-title">{t('members.title')}</h2>
            </div>
            <div className="members__card">
              <div className="members__error">
                <svg
                  className="members__error-icon"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <div className="members__error-content">
                  <h3 className="members__error-title">
                    {is403 ? t('common.accessDenied') : t('members.unableLoad')}
                  </h3>
                  <p className="members__error-message">
                    {is403
                      ? t('members.notMember')
                      : t('common.checkConnection')}
                  </p>
                </div>
                {!is403 && (
                  <Button variant="primary" size="sm" onClick={handleRetry}>
                    {t('common.retry')}
                  </Button>
                )}
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  // Empty state
  if (members.length === 0) {
    return (
      <div className="members">
        <div className="members__wrapper">
          <section className="members__section">
            <div className="members__section-header">
              <h2 className="members__section-title">{t('members.title')}</h2>
            </div>
            <div className="members__card">
              <div className="members__empty">
                <svg
                  className="members__empty-icon"
                  width="40"
                  height="40"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                <h3 className="members__empty-title">{t('members.noMembers')}</h3>
                <p className="members__empty-desc">
                  {t('members.emptyDesc')}
                </p>
                <Button variant="primary" size="md" onClick={() => setIsInviteOpen(true)}>
                  {t('members.invite')}
                </Button>
              </div>
            </div>
          </section>
        </div>

        <InviteModal
          householdId={householdId}
          isOpen={isInviteOpen}
          onClose={() => setIsInviteOpen(false)}
        />
      </div>
    );
  }

  // Normal state with members
  return (
    <div className="members">
      <div className="members__wrapper">
        <section className="members__section">
          <div className="members__section-header">
            <h2 className="members__section-title">{t('members.title')}</h2>
            <Button variant="primary" size="sm" onClick={() => setIsInviteOpen(true)}>
              {t('members.invite')}
            </Button>
          </div>
          <div className="members__card">
            {members.map((member: HouseholdMember, idx: number) => (
              <div key={member.userId}>
                {idx > 0 && <div className="members__divider" />}
                <div className="members__item">
                  <div className="members__avatar">
                    {getInitials(member.displayName)}
                  </div>
                  <div className="members__info">
                    <span className="members__name" title={member.displayName}>
                      {member.displayName}
                    </span>
                    <div className="members__meta">
                      <span className="members__email" title={member.email}>
                        {member.email}
                      </span>
                      <span className={`members__role members__role--${member.role}`}>
                        {getRoleLabel(member.role, t)}
                      </span>
                      <span className="members__joined">
                        {t('members.joined', { time: formatRelativeTime(member.joinedAt) })}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          {members.length >= 5 && (
            <p className="members__hint">
              {t('members.countHint', { count: members.length })}
            </p>
          )}
        </section>
      </div>

      <InviteModal
        householdId={householdId}
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
      />
    </div>
  );
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

function getRoleLabel(role: HouseholdMember['role'], t: ReturnType<typeof useI18n>['t']): string {
  return role === 'admin' ? t('members.roleAdmin') : t('members.roleMember');
}
