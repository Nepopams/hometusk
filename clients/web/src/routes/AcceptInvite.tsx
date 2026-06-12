import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import { useI18n } from '../i18n';
import { Button } from '../components/ui';
import type { AcceptInviteResponse } from '../types/api';
import './AcceptInvite.css';

type PageState =
  | 'input' // Manual code entry
  | 'validating' // Processing invite
  | 'success' // Joined successfully
  | 'already_member' // User is already a member
  | 'invalid' // Invalid or expired code
  | 'network_error'; // Network/server error

interface ResultInfo {
  title: string;
  message: string;
}

/**
 * Accept Invite page.
 * Handles both deep links (?token=xxx) and manual code entry.
 *
 * States:
 * - input: Form for manual code entry
 * - validating: Processing the invite (non-scary progress)
 * - success: Joined household, auto-redirect
 * - already_member: User was already a member
 * - invalid: Code invalid or expired
 * - network_error: Connection or server error
 *
 * Design follows auth screen patterns (Login, Register).
 */
export default function AcceptInvite() {
  const { t } = useI18n();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const urlToken = searchParams.get('token');

  const [state, setState] = useState<PageState>(urlToken ? 'validating' : 'input');
  const [code, setCode] = useState(urlToken ?? '');
  const [inputError, setInputError] = useState<string | null>(null);
  const [result, setResult] = useState<ResultInfo | null>(null);
  const [household, setHousehold] = useState<{ id: string; name: string } | null>(null);

  // Refs to avoid stale closures
  const selectHouseholdRef = useRef(selectHousehold);
  const refetchUserRef = useRef(refetchUser);

  useEffect(() => {
    selectHouseholdRef.current = selectHousehold;
    refetchUserRef.current = refetchUser;
  }, [selectHousehold, refetchUser]);

  // Process invite code
  const processInvite = async (inviteCode: string) => {
    setState('validating');
    setInputError(null);

    try {
      const response: AcceptInviteResponse = await acceptInvite(inviteCode.trim());

      setHousehold({ id: response.household.id, name: response.household.name });

      setState('success');
      setResult({
        title: t('invite.welcomeTeam'),
        message: t('invite.joinedHousehold'),
      });

      // Refetch user data and select household
      await refetchUserRef.current();
      selectHouseholdRef.current(response.household.id);

      // Auto-redirect after short delay
      setTimeout(() => {
        navigate(`/households/${response.household.id}/tasks`, { replace: true });
      }, 2000);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 404 || err.status === 410) {
          setState('invalid');
          setResult({
            title: t('invite.noLongerValid'),
            message: t('invite.noLongerValidMsg'),
          });
        } else if (err.status === 409) {
          // Already a member (conflict)
          setState('already_member');
          setResult({
            title: t('invite.alreadyMemberTitle'),
            message: t('invite.alreadyMemberMsg'),
          });
          // Try to get household info from error response
          const body = err.body as { household?: { id: string; name: string } } | undefined;
          if (body?.household) {
            setHousehold(body.household);
          }
        } else {
          setState('network_error');
          setResult({
            title: t('invite.processErrorTitle'),
            message: t('invite.processErrorMsg'),
          });
        }
      } else {
        setState('network_error');
        setResult({
          title: t('invite.connectionProblem'),
          message: t('invite.connectionProblemMsg'),
        });
      }
    }
  };

  // Auto-process URL token on mount
  useEffect(() => {
    if (urlToken) {
      processInvite(urlToken);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Handle form submission
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    const trimmedCode = code.trim();
    if (!trimmedCode) {
      setInputError(t('invite.enterCode'));
      return;
    }

    processInvite(trimmedCode);
  };

  // Handle retry
  const handleRetry = () => {
    if (code.trim()) {
      processInvite(code.trim());
    } else {
      setState('input');
      setResult(null);
    }
  };

  // Handle "Open household" for already_member state
  const handleOpenHousehold = async () => {
    if (household) {
      await refetchUserRef.current();
      selectHouseholdRef.current(household.id);
      navigate(`/households/${household.id}/tasks`, { replace: true });
    } else {
      navigate('/households', { replace: true });
    }
  };

  // Render input state
  const renderInput = () => (
    <>
      <div className="accept-invite__header">
        <div className="accept-invite__icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
          </svg>
        </div>
        <h1 className="accept-invite__title">{t('invite.joinHouseholdInputTitle')}</h1>
        <p className="accept-invite__subtitle">
          {t('invite.enterCodeToJoin')}
        </p>
      </div>

      <form className="accept-invite__form" onSubmit={handleSubmit}>
        <div className="accept-invite__field">
          <label htmlFor="invite-code" className="accept-invite__label">
            {t('invite.code')}
          </label>
          <input
            id="invite-code"
            type="text"
            className={`accept-invite__input ${inputError ? 'accept-invite__input--error' : ''}`}
            placeholder={t('invite.codePlaceholder')}
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              setInputError(null);
            }}
            autoFocus
            autoComplete="off"
            spellCheck="false"
          />
          {inputError ? (
            <span className="accept-invite__error-text">{inputError}</span>
          ) : (
            <span className="accept-invite__hint">{t('invite.codeHint')}</span>
          )}
        </div>

        <div className="accept-invite__actions">
          <Button type="submit" variant="primary" size="lg" fullWidth>
            {t('common.continue')}
          </Button>
        </div>
      </form>

      <p className="accept-invite__link">
        {t('invite.noInvitePrompt')} <Link to="/households">{t('invite.createOwnHousehold')}</Link>
      </p>
    </>
  );

  // Render validating state
  const renderValidating = () => (
    <div className="accept-invite__validating">
      <div className="accept-invite__spinner" aria-hidden="true" />
      <p className="accept-invite__validating-text">{t('invite.checking')}</p>
    </div>
  );

  // Render success state
  const renderSuccess = () => (
    <div className="accept-invite__result">
      <div className="accept-invite__icon accept-invite__icon--success">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
      <h2 className="accept-invite__result-title">{result?.title}</h2>
      <p className="accept-invite__result-message">
        {result?.message}{' '}
        {household && <span className="accept-invite__household-name">&quot;{household.name}&quot;</span>}
      </p>
      <p className="accept-invite__redirect-hint">{t('invite.redirecting')}</p>
    </div>
  );

  // Render already_member state
  const renderAlreadyMember = () => (
    <div className="accept-invite__result">
      <div className="accept-invite__icon accept-invite__icon--info">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
      </div>
      <h2 className="accept-invite__result-title">{result?.title}</h2>
      <p className="accept-invite__result-message">
        {result?.message}{' '}
        {household && <span className="accept-invite__household-name">&quot;{household.name}&quot;</span>}
      </p>
      <div className="accept-invite__actions">
        <Button variant="primary" size="lg" fullWidth onClick={handleOpenHousehold}>
          {t('invite.openHousehold')}
        </Button>
      </div>
    </div>
  );

  // Render invalid/expired state
  const renderInvalid = () => (
    <div className="accept-invite__result">
      <div className="accept-invite__icon accept-invite__icon--error">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
      </div>
      <h2 className="accept-invite__result-title">{result?.title}</h2>
      <p className="accept-invite__result-message">{result?.message}</p>
      <div className="accept-invite__actions">
        <Button
          variant="secondary"
          size="lg"
          fullWidth
          onClick={() => {
            setState('input');
            setCode('');
            setResult(null);
          }}
        >
          {t('invite.tryDifferent')}
        </Button>
        <Button variant="ghost" size="md" fullWidth onClick={() => navigate('/households')}>
          {t('household.goToMyHouseholds')}
        </Button>
      </div>
    </div>
  );

  // Render network error state
  const renderNetworkError = () => (
    <div className="accept-invite__result">
      <div className="accept-invite__icon accept-invite__icon--error">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      </div>
      <h2 className="accept-invite__result-title">{result?.title}</h2>
      <p className="accept-invite__result-message">{result?.message}</p>
      <div className="accept-invite__actions">
        <Button variant="primary" size="lg" fullWidth onClick={handleRetry}>
          {t('common.tryAgain')}
        </Button>
        <Button variant="ghost" size="md" fullWidth onClick={() => navigate('/households')}>
          {t('household.goToMyHouseholds')}
        </Button>
      </div>
    </div>
  );

  return (
    <div className="accept-invite">
      <div className="accept-invite__card">
        {state === 'input' && renderInput()}
        {state === 'validating' && renderValidating()}
        {state === 'success' && renderSuccess()}
        {state === 'already_member' && renderAlreadyMember()}
        {state === 'invalid' && renderInvalid()}
        {state === 'network_error' && renderNetworkError()}
      </div>
    </div>
  );
}
