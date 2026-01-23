import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvite } from '../lib/api';
import { ApiError } from '../lib/errors';
import { useAuth } from '../hooks/useAuth';
import type { AcceptInviteResponse } from '../types/api';

type AcceptStatus = 'idle' | 'loading' | 'success' | 'error';

interface ErrorInfo {
  title: string;
  message: string;
}

function getErrorInfo(err: unknown): ErrorInfo {
  if (err instanceof ApiError) {
    if (err.status === 404) {
      return {
        title: 'Invalid invite link',
        message: 'This invite link is not valid. Please ask for a new invite.',
      };
    }
    if (err.status === 410) {
      return {
        title: 'Invite expired',
        message: 'This invite has expired or was already used. Please ask for a new invite.',
      };
    }
  }
  return {
    title: 'Something went wrong',
    message: 'Failed to accept invite. Please try again.',
  };
}

export default function AcceptInvite() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { selectHousehold, refetchUser } = useAuth();

  const token = searchParams.get('token');

  const [status, setStatus] = useState<AcceptStatus>('idle');
  const [error, setError] = useState<ErrorInfo | null>(null);
  const [response, setResponse] = useState<AcceptInviteResponse | null>(null);
  const selectHouseholdRef = useRef(selectHousehold);
  const refetchUserRef = useRef(refetchUser);

  useEffect(() => {
    selectHouseholdRef.current = selectHousehold;
    refetchUserRef.current = refetchUser;
  }, [selectHousehold, refetchUser]);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setError({
        title: 'Invalid invite link',
        message: 'No invite token found in the URL.',
      });
      return;
    }

    let isMounted = true;

    async function processInvite() {
      setStatus('loading');

      try {
        const result = await acceptInvite(token!);

        if (!isMounted) return;

        setResponse(result);
        setStatus('success');

        await refetchUserRef.current();
        selectHouseholdRef.current(result.household.id);

        navigate(`/households/${result.household.id}/tasks`, { replace: true });
      } catch (err) {
        if (!isMounted) return;

        setStatus('error');
        setError(getErrorInfo(err));
      }
    }

    processInvite();

    return () => {
      isMounted = false;
    };
  }, [token, navigate]);

  return (
    <div className="page accept-invite">
      {status === 'loading' && (
        <div className="accept-invite__card">
          <h1>Accepting invite...</h1>
          <p>Please wait while we process your invite.</p>
        </div>
      )}

      {status === 'success' && response && (
        <div className="accept-invite__card accept-invite__card--success">
          <h1>Welcome!</h1>
          <p>
            You have joined <strong>{response.household.name}</strong>.
          </p>
          <p>Redirecting to your dashboard...</p>
        </div>
      )}

      {status === 'error' && error && (
        <div className="accept-invite__card accept-invite__card--error">
          <h1>{error.title}</h1>
          <p>{error.message}</p>
          <Link to="/households" className="button">
            Back to Home
          </Link>
        </div>
      )}
    </div>
  );
}
