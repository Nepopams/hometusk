import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n';
import { createInvite, acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { Button, TextField } from '../components/ui';
import type { CreateInviteResponse, InviteStatus } from '../types/api';
import './Invites.css';

interface InviteEntry {
  token: string;
  link: string;
  expiresAt: string;
  status: InviteStatus;
  createdAt: string;
}

/**
 * Invites page for creating and managing household invites.
 *
 * Sections:
 * - CREATE INVITE: Create new invite with code/link copy
 * - ACTIVE INVITES: List of created invites with status
 * - JOIN HOUSEHOLD: Accept invite via code input
 *
 * States: loading, empty, error, normal
 *
 * @see Pencil frames: zx8XB, yFgZQ, K2bC1, z8yur, ZWWj9
 */
export default function Invites() {
  const { t, formatRelativeTime } = useI18n();
  const { householdId, refetchUser } = useAuth();
  const navigate = useNavigate();

  // Create invite state
  const [isCreating, setIsCreating] = useState(false);
  const [createdInvite, setCreatedInvite] = useState<CreateInviteResponse | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  // Invites list state (localStorage for MVP since no list API)
  const [invites, setInvites] = useState<InviteEntry[]>(() => {
    if (!householdId) return [];
    const stored = localStorage.getItem(`invites_${householdId}`);
    if (stored) {
      try {
        return JSON.parse(stored) as InviteEntry[];
      } catch {
        return [];
      }
    }
    return [];
  });

  // Accept invite state
  const [inviteCode, setInviteCode] = useState('');
  const [isAccepting, setIsAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);

  const inviteLink = createdInvite
    ? createdInvite.inviteLink ?? `${window.location.origin}/invite?token=${createdInvite.inviteToken}`
    : '';

  const handleCreateInvite = async () => {
    if (!householdId || isCreating) return;

    setIsCreating(true);
    setCreateError(null);
    setCreatedInvite(null);
    setCopiedCode(false);
    setCopiedLink(false);

    try {
      const response = await createInvite(householdId);
      setCreatedInvite(response);

      // Store in local list
      const newEntry: InviteEntry = {
        token: response.inviteToken,
        link: response.inviteLink ?? `${window.location.origin}/invite?token=${response.inviteToken}`,
        expiresAt: response.expiresAt,
        status: response.status,
        createdAt: new Date().toISOString(),
      };
      const updated = [newEntry, ...invites].slice(0, 10);
      setInvites(updated);
      localStorage.setItem(`invites_${householdId}`, JSON.stringify(updated));
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setCreateError(t('invite.createUnauthorized'));
        } else {
          const msg =
            typeof err.body === 'object' && err.body && 'message' in err.body
              ? (err.body as { message?: string }).message
              : undefined;
          setCreateError(msg || t('invite.failedCreate'));
        }
      } else {
        setCreateError(t('household.unexpectedError'));
      }
    } finally {
      setIsCreating(false);
    }
  };

  const handleCopyCode = async () => {
    if (!createdInvite || copiedCode) return;
    try {
      await navigator.clipboard.writeText(createdInvite.inviteToken);
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
    } catch {
      setCreateError(t('invite.copyUnsupportedShort'));
    }
  };

  const handleCopyLink = async () => {
    if (!inviteLink || copiedLink) return;
    try {
      await navigator.clipboard.writeText(inviteLink);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    } catch {
      setCreateError(t('invite.copyUnsupportedShort'));
    }
  };

  const handleCopyInviteCode = async (token: string) => {
    try {
      await navigator.clipboard.writeText(token);
    } catch {
      // Ignore copy failures
    }
  };

  const handleAcceptInvite = async (e: FormEvent) => {
    e.preventDefault();
    if (!inviteCode.trim() || isAccepting) return;

    setIsAccepting(true);
    setAcceptError(null);

    try {
      const response = await acceptInvite(inviteCode.trim());
      await refetchUser();
      navigate(`/households/${response.household.id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 404 || err.status === 410) {
          setAcceptError(t('invite.notFoundOrExpired'));
        } else if (err.status === 409) {
          setAcceptError(t('invite.alreadyMemberError'));
        } else {
          const msg =
            typeof err.body === 'object' && err.body && 'message' in err.body
              ? (err.body as { message?: string }).message
              : undefined;
          setAcceptError(msg || t('invite.failedAccept'));
        }
      } else {
        setAcceptError(t('household.unexpectedError'));
      }
    } finally {
      setIsAccepting(false);
    }
  };

  const formatExpiry = (expiresAt: string): string => {
    const expiry = new Date(expiresAt);
    const now = new Date();
    const diffMs = expiry.getTime() - now.getTime();
    const diffHours = Math.ceil(diffMs / (1000 * 60 * 60));

    if (diffHours <= 0) return t('common.expired');
    if (diffHours < 24) return t('invite.hoursLeft', { count: diffHours });
    const diffDays = Math.ceil(diffHours / 24);
    return t('invite.daysLeft', { count: diffDays });
  };

  const getStatusDisplay = (entry: InviteEntry): { label: string; variant: 'active' | 'used' | 'expired' } => {
    const now = new Date();
    const expiry = new Date(entry.expiresAt);

    if (entry.status === 'redeemed') return { label: t('invite.used'), variant: 'used' };
    if (entry.status === 'revoked') return { label: t('invite.revoked'), variant: 'expired' };
    if (expiry < now) return { label: t('common.expired'), variant: 'expired' };
    return { label: t('invite.active'), variant: 'active' };
  };

  if (!householdId) {
    return (
      <div className="invites">
        <div className="invites__wrapper">
          <div className="invites__empty">
            <p>{t('invite.manageNoHousehold')}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="invites">
      <div className="invites__wrapper">
        {/* CREATE INVITE Section */}
        <section className="invites__section">
          <h2 className="invites__section-title">{t('invite.createInviteTitle')}</h2>
          <div className="invites__card">
            <Button
              variant="primary"
              size="md"
              onClick={handleCreateInvite}
              disabled={isCreating}
            >
              {isCreating ? t('invite.creating') : t('invite.createInvite')}
            </Button>

            {createError && (
              <div className="invites__error" role="alert">
                {createError}
              </div>
            )}

            {createdInvite && (
              <div className="invites__result">
                <div className="invites__result-row">
                  <span className="invites__result-label">{t('invite.codeLabel')}</span>
                  <code className="invites__result-code">{createdInvite.inviteToken}</code>
                  <Button variant="secondary" size="sm" onClick={handleCopyCode}>
                    {copiedCode ? t('common.copiedBang') : t('common.copy')}
                  </Button>
                </div>
                <div className="invites__result-row">
                  <span className="invites__result-label">{t('invite.linkLabel')}</span>
                  <span className="invites__result-link">{inviteLink}</span>
                  <Button variant="secondary" size="sm" onClick={handleCopyLink}>
                    {copiedLink ? t('common.copiedBang') : t('common.copy')}
                  </Button>
                </div>
                <div className="invites__result-meta">
                  <span className="invites__badge invites__badge--warning">
                    {formatExpiry(createdInvite.expiresAt)}
                  </span>
                  <span className="invites__badge invites__badge--success">{t('invite.active')}</span>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* ACTIVE INVITES Section */}
        <section className="invites__section">
          <h2 className="invites__section-title">{t('invite.activeInvites')}</h2>
          <div className="invites__card">
            {invites.length === 0 ? (
              <div className="invites__list-empty">
                <svg className="invites__list-empty-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                  <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
                <h3 className="invites__list-empty-title">{t('invite.noActiveInvites')}</h3>
                <p className="invites__list-empty-desc">{t('invite.shareCodeDesc')}</p>
              </div>
            ) : (
              <div className="invites__list">
                {invites.map((entry, index) => {
                  const status = getStatusDisplay(entry);
                  const isActive = status.variant === 'active';

                  return (
                    <div key={entry.token} className="invites__list-item">
                      {index > 0 && <div className="invites__list-divider" />}
                      <div className="invites__list-row">
                        <div className="invites__list-info">
                          <code className={`invites__list-code ${!isActive ? 'invites__list-code--muted' : ''}`}>
                            {entry.token}
                          </code>
                          <div className="invites__list-meta">
                            <span className="invites__list-created">
                              {t('invite.created', { time: formatRelativeTime(entry.createdAt) })}
                            </span>
                            {isActive && (
                              <span className="invites__badge invites__badge--warning">
                                {formatExpiry(entry.expiresAt)}
                              </span>
                            )}
                            <span className={`invites__badge invites__badge--${status.variant}`}>
                              {status.label}
                            </span>
                          </div>
                        </div>
                        {isActive && (
                          <div className="invites__list-actions">
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() => handleCopyInviteCode(entry.token)}
                            >
                              {t('common.copy')}
                            </Button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>

        {/* JOIN HOUSEHOLD Section */}
        <section className="invites__section">
          <h2 className="invites__section-title">{t('invite.joinHouseholdTitle')}</h2>
          <form className="invites__card" onSubmit={handleAcceptInvite}>
            <TextField
              label={t('common.pasteCode')}
              type="text"
              placeholder={t('invite.enterCodePlaceholder')}
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value)}
              disabled={isAccepting}
            />
            <div className="invites__accept-row">
              <Button
                type="submit"
                variant="primary"
                size="md"
                disabled={isAccepting || !inviteCode.trim()}
              >
                {isAccepting ? t('invite.joining') : t('invite.joinHousehold')}
              </Button>
            </div>
            {acceptError && (
              <div className="invites__error-banner" role="alert">
                <strong>{t('common.error')}:</strong> {acceptError}
              </div>
            )}
          </form>
        </section>
      </div>
    </div>
  );
}
