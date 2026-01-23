export const FIELD_LABELS: Record<string, string> = {
  zoneId: 'Zone',
  deadline: 'Deadline',
  assigneeId: 'Assignee',
  title: 'Task title',
  description: 'Description',
  taskId: 'Task',
};

export function getFieldLabel(field: string): string {
  return FIELD_LABELS[field] || field;
}

export const POLICY_LABELS: Record<string, string> = {
  ZONE_REQUIRED: 'Zone is required for task creation',
  DEADLINE_REQUIRED: 'Deadline must be specified',
  ASSIGNEE_AMBIGUOUS: 'Multiple possible assignees found',
  TITLE_REQUIRED: 'Task title is required',
  TASK_NOT_FOUND: 'The specified task was not found',
};

export function getPolicyLabel(policyName: string): string {
  return POLICY_LABELS[policyName] || `Policy: ${policyName}`;
}
