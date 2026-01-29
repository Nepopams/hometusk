import type { RecurrenceRule } from '../../types/api';

const DAYS_SHORT: Record<string, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

function getOrdinalSuffix(n: number): string {
  const s = ['th', 'st', 'nd', 'rd'];
  const v = n % 100;
  return s[(v - 20) % 10] || s[v] || s[0];
}

export function formatFrequency(rule: RecurrenceRule): string {
  switch (rule.type) {
    case 'DAILY':
      return 'Daily';
    case 'WEEKLY':
      if (!rule.daysOfWeek || rule.daysOfWeek.length === 0) return 'Weekly';
      if (rule.daysOfWeek.length === 7) return 'Every day';
      return rule.daysOfWeek.map((d) => DAYS_SHORT[d] || d).join(', ');
    case 'MONTHLY': {
      const day = rule.dayOfMonth || 1;
      return `${day}${getOrdinalSuffix(day)} of month`;
    }
    case 'EVERY_N_DAYS':
      return `Every ${rule.interval || 2} days`;
    default:
      return 'Custom';
  }
}

interface Props {
  rule: RecurrenceRule;
}

export default function FrequencyDisplay({ rule }: Props) {
  return <span>{formatFrequency(rule)}</span>;
}
