import type { CommandResponse } from '../../types/api';
import { DegradedResult } from './DegradedResult';
import { ExecutedResult } from './ExecutedResult';
import { NeedsInputResult } from './NeedsInputResult';
import { RejectedResult } from './RejectedResult';

interface CommandResultProps {
  response: CommandResponse;
  onNewCommand: () => void;
  onRetry: () => void;
}

export function CommandResult({ response, onNewCommand, onRetry }: CommandResultProps) {
  switch (response.status) {
    case 'executed':
      return <ExecutedResult data={response} onNewCommand={onNewCommand} />;
    case 'needs_input':
      return <NeedsInputResult data={response} onRetry={onRetry} />;
    case 'rejected':
      return <RejectedResult data={response} onRetry={onRetry} onNewCommand={onNewCommand} />;
    case 'executed_degraded':
      return <DegradedResult data={response} onNewCommand={onNewCommand} />;
    default:
      return null;
  }
}
