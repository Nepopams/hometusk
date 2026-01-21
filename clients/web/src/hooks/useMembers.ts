import { useEffect, useState } from 'react';
import { getMembers } from '../lib/api';
import type { HouseholdMember } from '../types/api';

export function useMembers(householdId: string | undefined) {
  const [members, setMembers] = useState<HouseholdMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!householdId) {
      setMembers([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    getMembers(householdId)
      .then(setMembers)
      .catch((e) => setError(e instanceof Error ? e : new Error('Failed to load members')))
      .finally(() => setIsLoading(false));
  }, [householdId]);

  return { members, isLoading, error };
}
