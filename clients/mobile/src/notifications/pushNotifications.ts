import Constants from 'expo-constants';
import * as Linking from 'expo-linking';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import type { MobilePlatform } from '../api/types';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: false,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

export type PushTokenOutcome =
  | { status: 'ready'; pushToken: string; platform: MobilePlatform }
  | { status: 'unsupported' | 'missing_project_id' | 'permission_denied' | 'error'; message: string };

export type HomeTuskLinkTarget =
  | {
      kind: 'task';
      taskId: string;
      householdId?: string;
      source: 'link' | 'notification';
    }
  | {
      kind: 'command';
      commandId?: string;
      householdId?: string;
      source: 'link' | 'notification';
    }
  | {
      kind: 'invite';
      inviteToken: string;
      source: 'link' | 'notification';
    }
  | {
      kind: 'notification';
      notificationId?: string;
      householdId?: string;
      source: 'link' | 'notification';
    };

type ExpoConstantsWithProjectId = typeof Constants & {
  easConfig?: {
    projectId?: string;
  };
};

export async function requestExpoPushTokenAsync(): Promise<PushTokenOutcome> {
  const platform = getMobilePlatform();
  if (!platform) {
    return { status: 'unsupported', message: 'Push registration is available on iOS and Android builds.' };
  }

  const projectId = getExpoProjectId();
  if (!projectId) {
    return {
      status: 'missing_project_id',
      message: 'Push registration needs an Expo/EAS project id in a development build.',
    };
  }

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'HomeTusk',
      importance: Notifications.AndroidImportance.MAX,
    });
  }

  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;
  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    return {
      status: 'permission_denied',
      message: 'Push permission was not granted on this device.',
    };
  }

  try {
    const token = await Notifications.getExpoPushTokenAsync({ projectId });
    return { status: 'ready', pushToken: token.data, platform };
  } catch {
    return {
      status: 'error',
      message: 'Could not obtain an Expo push token for this build.',
    };
  }
}

export function getMobilePlatform(): MobilePlatform | null {
  if (Platform.OS === 'ios' || Platform.OS === 'android') {
    return Platform.OS;
  }
  return null;
}

export function getDeviceMetadata() {
  const options = Intl.DateTimeFormat().resolvedOptions();
  return {
    appVersion: Constants.expoConfig?.version ?? null,
    deviceName: Platform.OS === 'ios' ? 'iOS device' : Platform.OS === 'android' ? 'Android device' : null,
    locale: options.locale ?? null,
    timezone: options.timeZone ?? null,
  };
}

export function targetFromNotificationResponse(
  response: Notifications.NotificationResponse
): HomeTuskLinkTarget | null {
  const data = response.notification.request.content.data;
  return targetFromPayload(data, 'notification');
}

export function targetFromDeepLinkUrl(url: string): HomeTuskLinkTarget | null {
  const parsed = Linking.parse(url);
  const segments = [parsed.hostname, ...(parsed.path?.split('/') ?? [])]
    .filter((segment): segment is string => Boolean(segment))
    .map((segment) => segment.toLowerCase());
  const target = segments[0];
  const queryParams = parsed.queryParams ?? {};

  if (target === 'task' || target === 'tasks') {
    const taskId = firstString(queryParams.taskId) ?? segments[1];
    if (!taskId) {
      return null;
    }
    return {
      kind: 'task',
      taskId,
      householdId: firstString(queryParams.householdId),
      source: 'link',
    };
  }

  if (target === 'command' || target === 'commands') {
    return {
      kind: 'command',
      commandId: firstString(queryParams.commandId) ?? segments[1],
      householdId: firstString(queryParams.householdId),
      source: 'link',
    };
  }

  if (target === 'invite' || target === 'accept-invite') {
    const inviteToken = firstString(queryParams.token) ?? firstString(queryParams.inviteToken) ?? segments[1];
    if (!inviteToken) {
      return null;
    }
    return { kind: 'invite', inviteToken, source: 'link' };
  }

  if (target === 'notification' || target === 'notifications') {
    return {
      kind: 'notification',
      notificationId: firstString(queryParams.notificationId) ?? segments[1],
      householdId: firstString(queryParams.householdId),
      source: 'link',
    };
  }

  return null;
}

function targetFromPayload(
  data: Notifications.NotificationContent['data'],
  source: 'link' | 'notification'
): HomeTuskLinkTarget | null {
  const payload = data ?? {};
  const deepLink = firstString(payload.deepLink) ?? firstString(payload.url);
  if (deepLink) {
    const target = targetFromDeepLinkUrl(deepLink);
    return target ? { ...target, source } : null;
  }

  const route = (firstString(payload.route) ?? firstString(payload.target) ?? firstString(payload.type))?.toLowerCase();
  if (route === 'task' || route === 'tasks') {
    const taskId = firstString(payload.taskId) ?? firstString(payload.entityId);
    if (!taskId) {
      return null;
    }
    return {
      kind: 'task',
      taskId,
      householdId: firstString(payload.householdId),
      source,
    };
  }

  if (route === 'command' || route === 'commands') {
    return {
      kind: 'command',
      commandId: firstString(payload.commandId) ?? firstString(payload.entityId),
      householdId: firstString(payload.householdId),
      source,
    };
  }

  if (route === 'invite' || route === 'accept-invite') {
    const inviteToken = firstString(payload.inviteToken) ?? firstString(payload.token);
    if (!inviteToken) {
      return null;
    }
    return { kind: 'invite', inviteToken, source };
  }

  return {
    kind: 'notification',
    notificationId: firstString(payload.notificationId) ?? firstString(payload.entityId),
    householdId: firstString(payload.householdId),
    source,
  };
}

function getExpoProjectId(): string | null {
  const constants = Constants as ExpoConstantsWithProjectId;
  const extra = constants.expoConfig?.extra as
    | {
        eas?: {
          projectId?: string;
        };
      }
    | undefined;

  return extra?.eas?.projectId ?? constants.easConfig?.projectId ?? null;
}

function firstString(value: unknown): string | undefined {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }
  if (Array.isArray(value)) {
    return firstString(value[0]);
  }
  return undefined;
}
