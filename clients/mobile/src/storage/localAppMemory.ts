import AsyncStorage from '@react-native-async-storage/async-storage';

const SELECTED_HOUSEHOLD_KEY = 'hometusk.local.selectedHousehold.v1';
const COMMAND_DRAFT_KEY = 'hometusk.local.commandDraft.v1';
const RECENT_COMMANDS_KEY = 'hometusk.local.recentCommands.v1';
const MOBILE_DEVICE_REGISTRATION_KEY = 'hometusk.local.mobileDeviceRegistration.v1';

export interface RecentCommandHint {
  id: string;
  householdId: string;
  text: string;
  status: string;
  createdAt: string;
}

export interface MobileDeviceRegistrationMemory {
  deviceId: string;
  platform: 'ios' | 'android';
  pushProvider: 'expo';
  status: 'active' | 'inactive';
  registeredAt: string;
}

export async function readSelectedHouseholdId(): Promise<string | null> {
  return AsyncStorage.getItem(SELECTED_HOUSEHOLD_KEY);
}

export async function writeSelectedHouseholdId(householdId: string | null): Promise<void> {
  if (!householdId) {
    await AsyncStorage.removeItem(SELECTED_HOUSEHOLD_KEY);
    return;
  }
  await AsyncStorage.setItem(SELECTED_HOUSEHOLD_KEY, householdId);
}

export async function readCommandDraft(): Promise<string> {
  return (await AsyncStorage.getItem(COMMAND_DRAFT_KEY)) ?? '';
}

export async function writeCommandDraft(value: string): Promise<void> {
  if (!value.trim()) {
    await AsyncStorage.removeItem(COMMAND_DRAFT_KEY);
    return;
  }
  await AsyncStorage.setItem(COMMAND_DRAFT_KEY, value);
}

export async function readRecentCommands(): Promise<RecentCommandHint[]> {
  const rawValue = await AsyncStorage.getItem(RECENT_COMMANDS_KEY);
  if (!rawValue) {
    return [];
  }

  try {
    return JSON.parse(rawValue) as RecentCommandHint[];
  } catch {
    await AsyncStorage.removeItem(RECENT_COMMANDS_KEY);
    return [];
  }
}

export async function writeRecentCommands(commands: RecentCommandHint[]): Promise<void> {
  await AsyncStorage.setItem(RECENT_COMMANDS_KEY, JSON.stringify(commands.slice(0, 20)));
}

export async function readMobileDeviceRegistration(): Promise<MobileDeviceRegistrationMemory | null> {
  const rawValue = await AsyncStorage.getItem(MOBILE_DEVICE_REGISTRATION_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as MobileDeviceRegistrationMemory;
    return parsed.deviceId ? parsed : null;
  } catch {
    await AsyncStorage.removeItem(MOBILE_DEVICE_REGISTRATION_KEY);
    return null;
  }
}

export async function writeMobileDeviceRegistration(
  registration: MobileDeviceRegistrationMemory
): Promise<void> {
  await AsyncStorage.setItem(MOBILE_DEVICE_REGISTRATION_KEY, JSON.stringify(registration));
}

export async function clearMobileDeviceRegistration(): Promise<void> {
  await AsyncStorage.removeItem(MOBILE_DEVICE_REGISTRATION_KEY);
}
