import { useCallback, useEffect, useState, type MouseEvent } from 'react';
import { createInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useI18n, type TranslationKey } from '../i18n';
import type { CreateInviteResponse } from '../types/api';

interface InviteModalProps {
  householdId: string | null;
  isOpen: boolean;
  onClose: () => void;
}

type TFunction = (
  key: TranslationKey,
  params?: Record<string, string | number | boolean | null | undefined>
) => string;

function formatExpiry(expiresAt: string, t: TFunction): string {
  const expiry = new Date(expiresAt);
  const now = new Date();
  const diffMs = expiry.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return t('common.expired');
  if (diffDays === 1) return t('invite.expiresOneDay');
  return t('invite.expiresDays', { count: diffDays });
}

export default function InviteModal({ householdId, isOpen, onClose }: InviteModalProps) {
  const { t } = useI18n();
  const [isLoading, setIsLoading] = useState(false);
  const [invite, setInvite] = useState<CreateInviteResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const inviteLink = invite
    ? invite.inviteLink ?? `${window.location.origin}/invite?token=${invite.inviteToken}`
    : '';

  const fetchInvite = useCallback(async () => {
    if (!householdId) return;

    setIsLoading(true);
    setError(null);
    setInvite(null);
    setCopied(false);

    try {
      const response = await createInvite(householdId);
      setInvite(response);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setError(t('invite.notMember'));
        } else {
          const msg =
            typeof err.body === 'object' && err.body && 'message' in err.body
              ? (err.body as { message?: string }).message
              : undefined;
          setError(msg || t('invite.failedCreate'));
        }
      } else {
        setError(t('household.unexpectedError'));
      }
    } finally {
      setIsLoading(false);
    }
  }, [householdId, t]);

  useEffect(() => {
    if (isOpen && householdId) {
      fetchInvite();
    }
  }, [isOpen, householdId, fetchInvite]);

  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };

    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);

  const handleCopy = async () => {
    if (!inviteLink || copied) return;

    try {
      await navigator.clipboard.writeText(inviteLink);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setError(t('invite.copyUnsupported'));
    }
  };

  const handleOverlayClick = (e: MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="invite-modal__overlay" onClick={handleOverlayClick}>
      <div className="invite-modal__panel" role="dialog" aria-modal="true">
        <h2>{t('invite.memberTitle')}</h2>

        {isLoading && <div className="invite-modal__loading">{t('invite.creating')}</div>}

        {error && (
          <div className="invite-modal__error" role="alert">
            <p>{error}</p>
            <button type="button" className="ghost-button" onClick={fetchInvite}>
              {t('common.retry')}
            </button>
          </div>
        )}

        {invite && !error && (
          <>
            <p className="invite-modal__hint">{t('invite.shareLink')}</p>

            <div className="invite-modal__field">
              <input
                type="text"
                value={inviteLink}
                readOnly
                onClick={(e) => (e.target as HTMLInputElement).select()}
              />
              <button
                type="button"
                className={`button invite-modal__copy ${copied ? 'is-copied' : ''}`}
                onClick={handleCopy}
              >
                {copied ? t('common.copiedBang') : t('invite.copyLink')}
              </button>
            </div>

            <p className="invite-modal__expiry">{formatExpiry(invite.expiresAt, t)}</p>
          </>
        )}

        <div className="invite-modal__actions">
          <button type="button" className="button" onClick={onClose}>
            {t('invite.done')}
          </button>
        </div>
      </div>
    </div>
  );
}
