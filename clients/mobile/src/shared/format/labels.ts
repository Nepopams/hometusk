export function formatTaskStatus(status: string): string {
  return status.replace(/_/g, ' ');
}

export function formatNotificationType(type: string): string {
  return type.replace(/_/g, ' ');
}
