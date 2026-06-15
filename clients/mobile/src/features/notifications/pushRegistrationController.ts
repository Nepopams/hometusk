import { createHomeTuskApiClient } from '../../api/client';
import type { StatusBannerMessage } from '../../app/types';
import { writeMobileDeviceRegistration } from '../../storage/localAppMemory';
import { formatPushRegistrationError } from '../../shared/errors/apiErrorFormatting';
import { getDeviceMetadata, requestExpoPushTokenAsync } from '../../notifications/pushNotifications';

export async function registerCurrentDeviceForPush(
  accessToken: string
): Promise<StatusBannerMessage> {
  const tokenOutcome = await requestExpoPushTokenAsync();

  if (tokenOutcome.status !== 'ready') {
    return {
      tone: tokenOutcome.status === 'permission_denied' ? 'error' : 'info',
      text: tokenOutcome.message,
    };
  }

  try {
    const metadata = getDeviceMetadata();
    const device = await createHomeTuskApiClient({ accessToken }).registerMobileDevice({
      platform: tokenOutcome.platform,
      pushProvider: 'expo',
      pushToken: tokenOutcome.pushToken,
      ...metadata,
    });

    await writeMobileDeviceRegistration({
      deviceId: device.id,
      platform: device.platform,
      pushProvider: device.pushProvider,
      status: device.status,
      registeredAt: new Date().toISOString(),
    });

    return { tone: 'success', text: 'Push registration is ready for this device.' };
  } catch (error) {
    return { tone: 'error', text: formatPushRegistrationError(error) };
  }
}
