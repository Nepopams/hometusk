import { useCallback, useEffect, useState, type MouseEvent } from 'react';
import { createInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import type { CreateInviteResponse } from '../types/api';

interface InviteModalProps {
  householdId: string | null;
  isOpen: boolean;
  onClose: () => void;
}

function formatExpiry(expiresAt: string): string {
  const expiry = new Date(expiresAt);
  const now = new Date();
  const diffMs = expiry.getTime() - now.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return 'Expired';
  if (diffDays === 1) return 'Expires in 1 day';
  return `Expires in ${diffDays} days`;
}

export default function InviteModal({ householdId, isOpen, onClose }: InviteModalProps) {
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
          setError('You are not a member of this household');
        } else {
          const msg =
            typeof err.body === 'object' && err.body && 'message' in err.body
              ? (err.body as { message?: string }).message
              : undefined;
          setError(msg || 'Failed to create invite');
        }
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      setIsLoading(false);
    }
  }, [householdId]);

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
      setError('Copy not supported — please select and copy manually');
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
        <h2>Invite a Member</h2>

        {isLoading && <div className="invite-modal__loading">Creating invite...</div>}

        {error && (
          <div className="invite-modal__error" role="alert">
            <p>{error}</p>
            <button type="button" className="ghost-button" onClick={fetchInvite}>
              Retry
            </button>
          </div>
        )}

        {invite && !error && (
          <>
            <p className="invite-modal__hint">Share this link to invite someone:</p>

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
                {copied ? 'Copied!' : 'Copy Link'}
              </button>
            </div>

            <p className="invite-modal__expiry">{formatExpiry(invite.expiresAt)}</p>
          </>
        )}

        <div className="invite-modal__actions">
          <button type="button" className="button" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
