import type { CommandResponse } from '../../api/types';
import { formatShortDate } from '../../shared/format/dates';
import type { MascotMood } from '../../shared/ui/Mascot';

export type CommandTone =
  | 'idle'
  | 'thinking'
  | 'success'
  | 'clarify'
  | 'confirm'
  | 'reject'
  | 'degraded';

export function formatCommandOutcome(status: string): string {
  if (status === 'executed') {
    return 'Сделано.';
  }
  if (status === 'executed_degraded') {
    return 'Сделано в ограниченном режиме.';
  }
  if (status === 'scheduled') {
    return 'Запланировано.';
  }
  if (status === 'needs_input') {
    return 'Нужна деталь.';
  }
  if (status === 'needs_confirmation') {
    return 'Нужно подтверждение.';
  }
  if (status === 'rejected') {
    return 'Команда остановлена.';
  }
  return `Статус команды: ${status}.`;
}

export function getCommandOutcomeBody(response: CommandResponse): string {
  if (response.status === 'needs_input') {
    return response.question ?? 'HomeTusk нужен еще один ответ, прежде чем продолжить.';
  }
  if (response.status === 'needs_confirmation') {
    return response.confirmation?.summary ?? 'Проверь действие и подтверди его явно.';
  }
  if (response.status === 'rejected') {
    return response.reason ?? response.errorCode ?? 'HomeTusk не применил команду по правилам дома.';
  }
  if (response.status === 'scheduled') {
    return response.scheduleAt ? `Запланировано на ${formatShortDate(response.scheduleAt)}.` : 'Команда запланирована.';
  }
  if (response.status === 'executed_degraded') {
    return 'HomeTusk применил только безопасную часть действия. Небезопасных изменений не было.';
  }
  return 'HomeTusk применил действие через правила выбранного дома.';
}

export function getCommandTone(status: string | null | undefined, isSaving = false): CommandTone {
  if (isSaving) {
    return 'thinking';
  }
  if (status === 'executed' || status === 'scheduled') {
    return 'success';
  }
  if (status === 'executed_degraded') {
    return 'degraded';
  }
  if (status === 'needs_input') {
    return 'clarify';
  }
  if (status === 'needs_confirmation') {
    return 'confirm';
  }
  if (status === 'rejected') {
    return 'reject';
  }
  return 'idle';
}

export function getCommandMascotMood(tone: CommandTone): MascotMood {
  if (tone === 'thinking') {
    return 'thinking';
  }
  if (tone === 'success') {
    return 'success';
  }
  if (tone === 'clarify') {
    return 'confused';
  }
  if (tone === 'confirm') {
    return 'confirm';
  }
  if (tone === 'reject') {
    return 'reject';
  }
  if (tone === 'degraded') {
    return 'degraded';
  }
  return 'idle';
}

export function formatRecentCommandStatus(status: string): string {
  return formatCommandOutcome(status).replace(/\.$/, '');
}
