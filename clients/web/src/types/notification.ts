export type NotificationType =
  | 'invite_accepted'
  | 'task_assigned'
  | 'task_completed'
  | 'shopping_item_added'
  | 'shopping_item_purchased';

export interface NotificationPayload {
  actorId: string;
  actorName: string;
  entityId: string;
  entityType: string;
  summary: string;
}

export interface Notification {
  id: string;
  householdId: string;
  userId: string;
  type: NotificationType;
  payload: NotificationPayload;
  createdAt: string;
  readAt: string | null;
}
