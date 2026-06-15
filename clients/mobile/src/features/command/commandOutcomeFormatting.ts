import type { CommandResponse } from '../../api/types';
import { formatShortDate } from '../../shared/format/dates';

export function formatCommandOutcome(status: string): string {
  if (status === 'executed') {
    return 'Command executed.';
  }
  if (status === 'executed_degraded') {
    return 'Command executed with fallback behavior.';
  }
  if (status === 'scheduled') {
    return 'Command scheduled.';
  }
  if (status === 'needs_input') {
    return 'Command needs more input.';
  }
  if (status === 'rejected') {
    return 'Command rejected by HomeTusk rules.';
  }
  return `Command status: ${status}.`;
}

export function getCommandOutcomeBody(response: CommandResponse): string {
  if (response.status === 'needs_input') {
    return response.question ?? 'HomeTusk needs more input.';
  }
  if (response.status === 'rejected') {
    return response.reason ?? response.errorCode ?? 'HomeTusk rejected this command.';
  }
  if (response.status === 'scheduled') {
    return response.scheduleAt ? `Scheduled for ${formatShortDate(response.scheduleAt)}.` : 'Scheduled.';
  }
  if (response.status === 'executed_degraded') {
    return response.degradedReason ?? response.fallbackStrategy ?? 'Fallback behavior was used.';
  }
  return 'HomeTusk recorded the decision and action.';
}
