import type { TranslationKey } from '../i18n';

export const FIELD_LABELS: Record<string, string> = {
  zoneId: 'Zone',
  deadline: 'Deadline',
  assigneeId: 'Assignee',
  title: 'Task title',
  description: 'Description',
  taskId: 'Task',
};

const FIELD_LABEL_KEYS: Record<string, TranslationKey> = {
  zoneId: 'fields.zoneId',
  deadline: 'fields.deadline',
  assigneeId: 'fields.assigneeId',
  title: 'fields.title',
  description: 'fields.description',
  taskId: 'fields.taskId',
};

type TFunction = (key: TranslationKey, params?: Record<string, string | number | boolean | null | undefined>) => string;

export function getFieldLabel(field: string, t?: TFunction): string {
  const key = FIELD_LABEL_KEYS[field];
  return t && key ? t(key) : FIELD_LABELS[field] || field;
}

export const POLICY_LABELS: Record<string, string> = {
  ZONE_REQUIRED: 'Zone is required for task creation',
  DEADLINE_REQUIRED: 'Deadline must be specified',
  ASSIGNEE_AMBIGUOUS: 'Multiple possible assignees found',
  TITLE_REQUIRED: 'Task title is required',
  TASK_NOT_FOUND: 'The specified task was not found',
};

const POLICY_LABEL_KEYS: Record<string, TranslationKey> = {
  ZONE_REQUIRED: 'policy.ZONE_REQUIRED',
  DEADLINE_REQUIRED: 'policy.DEADLINE_REQUIRED',
  ASSIGNEE_AMBIGUOUS: 'policy.ASSIGNEE_AMBIGUOUS',
  TITLE_REQUIRED: 'policy.TITLE_REQUIRED',
  TASK_NOT_FOUND: 'policy.TASK_NOT_FOUND',
};

export function getPolicyLabel(policyName: string, t?: TFunction): string {
  const key = POLICY_LABEL_KEYS[policyName];
  return t && key
    ? t(key)
    : POLICY_LABELS[policyName] || (t ? t('policy.fallback', { name: policyName }) : `Policy: ${policyName}`);
}
