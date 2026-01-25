import { useEffect, useState, useCallback } from 'react';
import { getMembers } from '../lib/api';
import type { HouseholdMember } from '../types/api';

export function useMembers(householdId: string | null | undefined) {
  const [members, setMembers] = useState<HouseholdMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchMembers = useCallback(() => {
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

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const refetch = useCallback(() => {
    fetchMembers();
  }, [fetchMembers]);

  return { members, isLoading, error, refetch };
}
