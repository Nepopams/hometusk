import { useI18n, type TranslationKey } from '../../i18n';
import type { RecurrenceRule } from '../../types/api';

type TFunction = (
  key: TranslationKey,
  params?: Record<string, string | number | boolean | null | undefined>
) => string;

const DAY_KEYS: Record<string, TranslationKey> = {
  MONDAY: 'day.mon',
  TUESDAY: 'day.tue',
  WEDNESDAY: 'day.wed',
  THURSDAY: 'day.thu',
  FRIDAY: 'day.fri',
  SATURDAY: 'day.sat',
  SUNDAY: 'day.sun',
};

export function formatFrequency(rule: RecurrenceRule, t: TFunction): string {
  switch (rule.type) {
    case 'DAILY':
      return t('routines.daily');
    case 'WEEKLY':
      if (!rule.daysOfWeek || rule.daysOfWeek.length === 0) return t('routines.weekly');
      if (rule.daysOfWeek.length === 7) return t('routines.everyDay');
      return rule.daysOfWeek.map((day) => (DAY_KEYS[day] ? t(DAY_KEYS[day]) : day)).join(', ');
    case 'MONTHLY':
      return t('routines.monthDay', { day: rule.dayOfMonth || 1 });
    case 'EVERY_N_DAYS':
      return t('routines.everyDays', { count: rule.interval || 2 });
    default:
      return t('routines.custom');
  }
}

interface Props {
  rule: RecurrenceRule;
}

export default function FrequencyDisplay({ rule }: Props) {
  const { t } = useI18n();
  return <span>{formatFrequency(rule, t)}</span>;
}
