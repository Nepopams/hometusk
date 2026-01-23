import type { CommandRequest, CommandResponse } from '../../types/api';
import { DegradedResult } from './DegradedResult';
import { ExecutedResult } from './ExecutedResult';
import { NeedsInputResult } from './NeedsInputResult';
import { RejectedResult } from './RejectedResult';

interface CommandResultProps {
  response: CommandResponse;
  request?: CommandRequest | null;
  onNewCommand: () => void;
  onRetry: () => void;
}

export function CommandResult({ response, request, onNewCommand, onRetry }: CommandResultProps) {
  switch (response.status) {
    case 'executed':
      return <ExecutedResult data={response} onNewCommand={onNewCommand} />;
    case 'needs_input':
      return <NeedsInputResult data={response} request={request} onRetry={onRetry} />;
    case 'rejected':
      return <RejectedResult data={response} onRetry={onRetry} onNewCommand={onNewCommand} />;
    case 'executed_degraded':
      return <DegradedResult data={response} onNewCommand={onNewCommand} />;
    default:
      return null;
  }
}
