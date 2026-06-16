import type { CommandRequest, CommandSource, NaturalCommandInputMode } from '../../api/types';

export type CommandRequestBuildResult =
  | { request: CommandRequest; displayText: string }
  | { error: string };

export type NaturalCommandRequestOptions = {
  inputMode?: NaturalCommandInputMode;
  source?: CommandSource;
  locale?: string;
  timezone?: string;
  asrTraceId?: string | null;
  now?: () => Date;
};

export function buildCommandRequestFromText(
  text: string,
  householdId: string,
  options: NaturalCommandRequestOptions = {}
): CommandRequestBuildResult {
  const trimmed = text.trim();
  if (!trimmed) {
    return { error: 'Command text is required.' };
  }

  const timestamp = (options.now ?? (() => new Date()))().toISOString();
  const inputMode = options.inputMode ?? 'text';
  const source = options.source ?? (inputMode === 'voice_transcript' ? 'voice' : 'mobile');
  const asrTraceId = options.asrTraceId ?? null;

  return {
    request: {
      householdId,
      type: 'natural_command',
      payload: {
        text: trimmed,
        inputMode,
        locale: options.locale ?? resolveLocale(),
        timezone: options.timezone ?? resolveTimezone(),
        referenceInstant: timestamp,
        asrTraceId,
      },
      source,
      ...(source === 'voice' && asrTraceId ? { asrTraceId } : {}),
      clientTimestamp: timestamp,
    },
    displayText: `Command: ${trimmed.length > 60 ? `${trimmed.slice(0, 60)}...` : trimmed}`,
  };
}

export function parseContinuationInput(value: string): Record<string, unknown> {
  const pairs = value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.split('=').map((piece) => piece.trim()));

  if (pairs.length > 0 && pairs.every((pair) => pair.length === 2 && pair[0] && pair[1])) {
    return Object.fromEntries(pairs);
  }

  return { clarification: value };
}

function resolveLocale(): string {
  return Intl.DateTimeFormat().resolvedOptions().locale || 'en-US';
}

function resolveTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
}
