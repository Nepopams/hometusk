import type { HouseholdSummary } from '../../api/types';
import type { HomeTuskLinkTarget } from '../../notifications/pushNotifications';

export function canOpenTargetHousehold(
  households: HouseholdSummary[] | undefined,
  target: HomeTuskLinkTarget
): boolean {
  if (target.kind === 'invite' || !target.householdId) {
    return false;
  }
  return Boolean(households?.some((household) => household.id === target.householdId));
}

export function linkStatusForTarget(target: HomeTuskLinkTarget) {
  if (target.kind === 'task') {
    return { tone: 'info' as const, text: 'Opening a task from HomeTusk handoff.' };
  }
  if (target.kind === 'command') {
    return { tone: 'info' as const, text: 'Opening command chat from HomeTusk handoff.' };
  }
  if (target.kind === 'invite') {
    return { tone: 'info' as const, text: 'Accepting household invite.' };
  }
  return { tone: 'info' as const, text: 'Opening notifications from HomeTusk handoff.' };
}
