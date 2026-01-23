import { useCallback, useRef, useState } from 'react';
import { executeCommand, generateIdempotencyKey } from '../lib/api';
import {
  addToHistory,
  createDisplayText,
  dispatchHistoryUpdate,
  generateEntryId,
} from '../lib/commandHistory';
import { ApiError } from '../lib/errors';
import type { CommandRequest, CommandResponse } from '../types/api';

interface UseCommandState {
  isLoading: boolean;
  response: CommandResponse | null;
  error: string | null;
  errorStatus: number | null;
}

interface UseCommandReturn extends UseCommandState {
  execute: (request: CommandRequest) => Promise<CommandResponse | null>;
  reset: () => void;
}

const ERROR_MESSAGES: Record<number, string> = {
  400: 'Invalid request. Please check your input.',
  403: 'Access denied. You are not a member of this household.',
  409: 'Command already submitted.',
};

const getApiErrorMessage = (err: ApiError): string | undefined => {
  if (typeof err.body === 'object' && err.body !== null && 'message' in err.body) {
    return (err.body as { message?: string }).message;
  }
  return undefined;
};

export function useCommand(): UseCommandReturn {
  const [state, setState] = useState<UseCommandState>({
    isLoading: false,
    response: null,
    error: null,
    errorStatus: null,
  });

  const idempotencyKeyRef = useRef<string>(generateIdempotencyKey());

  const execute = useCallback(async (request: CommandRequest): Promise<CommandResponse | null> => {
    setState({ isLoading: true, response: null, error: null, errorStatus: null });

    try {
      const response = await executeCommand(request, idempotencyKeyRef.current);
      setState({ isLoading: false, response, error: null, errorStatus: null });
      addToHistory({
        id: generateEntryId(),
        displayText: createDisplayText(request),
        commandType: request.type,
        status: response.status,
        timestamp: new Date().toISOString(),
        correlationId: response.correlationId,
        commandId: response.commandId,
        householdId: request.householdId,
        request,
        response,
      });
      dispatchHistoryUpdate(request.householdId);
      idempotencyKeyRef.current = generateIdempotencyKey();
      return response;
    } catch (err) {
      let errorMessage = 'An unexpected error occurred.';
      let errorStatus: number | null = null;

      if (err instanceof ApiError) {
        errorStatus = err.status;
        errorMessage = ERROR_MESSAGES[err.status] || getApiErrorMessage(err) || errorMessage;
        if (err.status === 409) {
          idempotencyKeyRef.current = generateIdempotencyKey();
        }
      }

      setState({ isLoading: false, response: null, error: errorMessage, errorStatus });
      return null;
    }
  }, []);

  const reset = useCallback(() => {
    setState({ isLoading: false, response: null, error: null, errorStatus: null });
    idempotencyKeyRef.current = generateIdempotencyKey();
  }, []);

  return {
    ...state,
    execute,
    reset,
  };
}
