import { StatusBar } from 'expo-status-bar';
import * as Linking from 'expo-linking';
import * as Notifications from 'expo-notifications';
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type ColorValue,
} from 'react-native';

import { createHomeTuskApiClient, HomeTuskApiError } from './src/api/client';
import { generateClientUuid } from './src/api/ids';
import type {
  CommandRequest,
  CommandResponse,
  HouseholdMember,
  HouseholdNotification,
  HouseholdSummary,
  ShoppingItem,
  ShoppingList,
  Task,
  UserProfile,
  Zone,
} from './src/api/types';
import {
  clearMobileDeviceRegistration,
  readMobileDeviceRegistration,
  readRecentCommands,
  readSelectedHouseholdId,
  writeMobileDeviceRegistration,
  writeRecentCommands,
  writeSelectedHouseholdId,
  type RecentCommandHint,
} from './src/storage/localAppMemory';
import {
  getDeviceMetadata,
  requestExpoPushTokenAsync,
  targetFromDeepLinkUrl,
  targetFromNotificationResponse,
  type HomeTuskLinkTarget,
} from './src/notifications/pushNotifications';
import {
  clearSecureSession,
  createSecureSession,
  isAccessTokenFresh,
  readSecureSession,
  writeSecureSession,
  type SecureSession,
} from './src/storage/secureSessionStore';

type SurfaceKey = 'home' | 'tasks' | 'shopping' | 'command';
type AuthMode = 'login' | 'register';
type AuthState = 'checking' | 'signedOut' | 'signedIn';
type ReadStatus = 'idle' | 'loading' | 'ready' | 'error';
type SavingAction = string | null;
type BannerTone = 'success' | 'info' | 'error';

type StatusBannerMessage = {
  tone: BannerTone;
  text: string;
};

type Surface = {
  key: SurfaceKey;
  label: string;
  title: string;
  summary: string;
  accent: ColorValue;
};

type OpenedSession = {
  session: SecureSession;
  profile: UserProfile;
};

type HouseholdReadModels = {
  members: HouseholdMember[];
  zones: Zone[];
  tasks: Task[];
  shoppingLists: ShoppingList[];
  shoppingItems: ShoppingItem[];
  notifications: HouseholdNotification[];
};

type MutationControls = {
  taskTitle: string;
  shoppingItemName: string;
  selectedShoppingListId: string | null;
  savingAction: SavingAction;
  onAddShoppingItem: () => void;
  onChangeShoppingItemName: (value: string) => void;
  onChangeTaskTitle: (value: string) => void;
  onCompleteTask: (taskId: string) => void;
  onCreateTask: () => void;
  onDeleteShoppingItem: (itemId: string) => void;
  onMarkPurchased: (itemId: string) => void;
  onSelectShoppingList: (listId: string) => void;
};

type CommandChatControls = {
  commandText: string;
  continuationText: string;
  error: string | null;
  isSaving: boolean;
  recentCommands: RecentCommandHint[];
  response: CommandResponse | null;
  onChangeCommandText: (value: string) => void;
  onChangeContinuationText: (value: string) => void;
  onContinueCommand: () => void;
  onSubmitCommand: () => void;
};

const surfaces: Surface[] = [
  {
    key: 'home',
    label: 'Home',
    title: 'Household home',
    summary: 'Members, zones, notifications, and the daily household pulse.',
    accent: '#1d7f68',
  },
  {
    key: 'tasks',
    label: 'Tasks',
    title: 'Tasks and zones',
    summary: 'Household-scoped work, assignees, zones, and completion state.',
    accent: '#2b66c3',
  },
  {
    key: 'shopping',
    label: 'Shopping',
    title: 'Shopping lists',
    summary: 'Shared lists, item state, purchase counts, and task links.',
    accent: '#946200',
  },
  {
    key: 'command',
    label: 'Command',
    title: 'Command chat',
    summary: 'Context-ready command surface for the selected household.',
    accent: '#7a3db8',
  },
];

function emptyReadModels(): HouseholdReadModels {
  return {
    members: [],
    zones: [],
    tasks: [],
    shoppingLists: [],
    shoppingItems: [],
    notifications: [],
  };
}

export default function App() {
  const [activeSurface, setActiveSurface] = useState<SurfaceKey>('home');
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [authState, setAuthState] = useState<AuthState>('checking');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [session, setSession] = useState<SecureSession | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [selectedHouseholdId, setSelectedHouseholdId] = useState<string | null>(null);
  const [readModels, setReadModels] = useState<HouseholdReadModels>(() => emptyReadModels());
  const [readStatus, setReadStatus] = useState<ReadStatus>('idle');
  const [readError, setReadError] = useState<string | null>(null);
  const [readReloadKey, setReadReloadKey] = useState(0);
  const [taskTitle, setTaskTitle] = useState('');
  const [shoppingItemName, setShoppingItemName] = useState('');
  const [selectedShoppingListId, setSelectedShoppingListId] = useState<string | null>(null);
  const [mutationMessage, setMutationMessage] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [savingAction, setSavingAction] = useState<SavingAction>(null);
  const [commandText, setCommandText] = useState('');
  const [continuationText, setContinuationText] = useState('');
  const [commandResponse, setCommandResponse] = useState<CommandResponse | null>(null);
  const [commandError, setCommandError] = useState<string | null>(null);
  const [commandSaving, setCommandSaving] = useState(false);
  const [recentCommands, setRecentCommands] = useState<RecentCommandHint[]>([]);
  const [pushStatus, setPushStatus] = useState<StatusBannerMessage | null>(null);
  const [linkStatus, setLinkStatus] = useState<StatusBannerMessage | null>(null);
  const [pendingLinkTarget, setPendingLinkTarget] = useState<HomeTuskLinkTarget | null>(null);
  const [highlightedTaskId, setHighlightedTaskId] = useState<string | null>(null);
  const handledLinkRef = useRef<string | null>(null);
  const handledNotificationRef = useRef<string | null>(null);
  const linkingUrl = Linking.useLinkingURL();
  const lastNotificationResponse = Notifications.useLastNotificationResponse();

  const surface = useMemo(
    () => surfaces.find((candidate) => candidate.key === activeSurface) ?? surfaces[0],
    [activeSurface]
  );

  const selectedHousehold = useMemo(
    () => profile?.households.find((household) => household.id === selectedHouseholdId) ?? null,
    [profile?.households, selectedHouseholdId]
  );

  const refreshSession = useCallback(async (refreshToken: string): Promise<SecureSession> => {
    const auth = await createHomeTuskApiClient().mobileRefresh({ refreshToken });
    const nextSession = createSecureSession(auth);
    await writeSecureSession(nextSession);
    return nextSession;
  }, []);

  const openSession = useCallback(
    async (candidate: SecureSession, allowRefresh = true): Promise<OpenedSession> => {
      let currentSession = candidate;

      if (allowRefresh && !isAccessTokenFresh(currentSession)) {
        currentSession = await refreshSession(currentSession.refreshToken);
      }

      try {
        const loadedProfile = await createHomeTuskApiClient({
          accessToken: currentSession.accessToken,
        }).getMe();
        return { session: currentSession, profile: loadedProfile };
      } catch (error) {
        if (!allowRefresh) {
          throw error;
        }

        const refreshedSession = await refreshSession(currentSession.refreshToken);
        const loadedProfile = await createHomeTuskApiClient({
          accessToken: refreshedSession.accessToken,
        }).getMe();
        return { session: refreshedSession, profile: loadedProfile };
      }
    },
    [refreshSession]
  );

  const applyOpenedSession = useCallback(async (opened: OpenedSession) => {
    const nextHouseholdId = await resolveSelectedHouseholdId(opened.profile);
    setSession(opened.session);
    setProfile(opened.profile);
    setSelectedHouseholdId(nextHouseholdId);
    setAuthState('signedIn');
    setAuthError(null);
  }, []);

  const routeLinkTarget = useCallback(
    async (target: HomeTuskLinkTarget) => {
      if (authState !== 'signedIn' || !session?.accessToken) {
        setPendingLinkTarget(target);
        setAuthError('Sign in to open this HomeTusk link.');
        return;
      }

      if (target.kind !== 'invite' && target.householdId) {
        const canOpenHousehold = profile?.households.some(
          (household) => household.id === target.householdId
        );
        if (canOpenHousehold) {
          setSelectedHouseholdId(target.householdId);
          await writeSelectedHouseholdId(target.householdId);
        }
      }

      if (target.kind === 'task') {
        if (target.householdId) {
          try {
            await createHomeTuskApiClient({ accessToken: session.accessToken }).getTask(
              target.householdId,
              target.taskId
            );
          } catch (error) {
            setActiveSurface('home');
            setHighlightedTaskId(null);
            setLinkStatus({ tone: 'error', text: formatLinkError(error) });
            return;
          }
        }
        setActiveSurface('tasks');
        setHighlightedTaskId(target.taskId);
        setLinkStatus({ tone: 'info', text: 'Opening a task from HomeTusk handoff.' });
        setReadReloadKey((value) => value + 1);
        return;
      }

      if (target.kind === 'command') {
        setActiveSurface('command');
        setHighlightedTaskId(null);
        setLinkStatus({ tone: 'info', text: 'Opening command chat from HomeTusk handoff.' });
        setReadReloadKey((value) => value + 1);
        return;
      }

      if (target.kind === 'invite') {
        setActiveSurface('home');
        setHighlightedTaskId(null);
        setLinkStatus({ tone: 'info', text: 'Accepting household invite.' });
        try {
          const client = createHomeTuskApiClient({ accessToken: session.accessToken });
          const accepted = await client.acceptInvite(target.inviteToken);
          const nextProfile = await client.getMe();
          setProfile(nextProfile);
          setSelectedHouseholdId(accepted.household.id);
          await writeSelectedHouseholdId(accepted.household.id);
          setLinkStatus({ tone: 'success', text: `Joined ${accepted.household.name}.` });
          setReadReloadKey((value) => value + 1);
        } catch (error) {
          setLinkStatus({ tone: 'error', text: formatLinkError(error) });
        }
        return;
      }

      setActiveSurface('home');
      setHighlightedTaskId(null);
      setLinkStatus({ tone: 'info', text: 'Opening notifications from HomeTusk handoff.' });
      setReadReloadKey((value) => value + 1);
    },
    [authState, profile?.households, session?.accessToken]
  );

  useEffect(() => {
    let isMounted = true;

    async function bootstrapSession() {
      try {
        const storedSession = await readSecureSession();
        if (!isMounted) {
          return;
        }

        if (!storedSession) {
          setAuthState('signedOut');
          return;
        }

        const opened = await openSession(storedSession);
        if (!isMounted) {
          return;
        }

        await applyOpenedSession(opened);
      } catch {
        await clearSecureSession();
        await clearMobileDeviceRegistration();
        if (!isMounted) {
          return;
        }
        setSession(null);
        setProfile(null);
        setSelectedHouseholdId(null);
        setReadModels(emptyReadModels());
        setReadStatus('idle');
        setAuthState('signedOut');
        setAuthError('Session expired. Sign in again.');
      }
    }

    bootstrapSession();
    return () => {
      isMounted = false;
    };
  }, [applyOpenedSession, openSession]);

  useEffect(() => {
    if (!pendingLinkTarget || authState !== 'signedIn' || !session?.accessToken) {
      return;
    }

    const target = pendingLinkTarget;
    setPendingLinkTarget(null);
    void routeLinkTarget(target);
  }, [authState, pendingLinkTarget, routeLinkTarget, session?.accessToken]);

  useEffect(() => {
    if (!linkingUrl || handledLinkRef.current === linkingUrl) {
      return;
    }
    handledLinkRef.current = linkingUrl;

    const target = targetFromDeepLinkUrl(linkingUrl);
    if (!target) {
      setLinkStatus({ tone: 'error', text: 'This HomeTusk link could not be opened safely.' });
      return;
    }

    void routeLinkTarget(target);
  }, [linkingUrl, routeLinkTarget]);

  useEffect(() => {
    const notificationId = lastNotificationResponse?.notification.request.identifier;
    if (!notificationId || handledNotificationRef.current === notificationId) {
      return;
    }
    handledNotificationRef.current = notificationId;

    const target = targetFromNotificationResponse(lastNotificationResponse);
    if (!target) {
      setLinkStatus({ tone: 'info', text: 'Opening HomeTusk from a notification.' });
      setActiveSurface('home');
      setReadReloadKey((value) => value + 1);
      return;
    }

    void routeLinkTarget(target);
  }, [lastNotificationResponse, routeLinkTarget]);

  useEffect(() => {
    if (authState !== 'signedIn') {
      return undefined;
    }

    const subscription = Notifications.addNotificationReceivedListener(() => {
      setPushStatus({ tone: 'info', text: 'Push notification received. Open it to route.' });
    });

    return () => {
      subscription.remove();
    };
  }, [authState]);

  useEffect(() => {
    let isMounted = true;

    async function registerDeviceForPush() {
      if (authState !== 'signedIn' || !session?.accessToken) {
        setPushStatus(null);
        return;
      }

      const tokenOutcome = await requestExpoPushTokenAsync();
      if (!isMounted) {
        return;
      }

      if (tokenOutcome.status !== 'ready') {
        setPushStatus({
          tone: tokenOutcome.status === 'permission_denied' ? 'error' : 'info',
          text: tokenOutcome.message,
        });
        return;
      }

      try {
        const metadata = getDeviceMetadata();
        const device = await createHomeTuskApiClient({ accessToken: session.accessToken }).registerMobileDevice({
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

        if (!isMounted) {
          return;
        }
        setPushStatus({ tone: 'success', text: 'Push registration is ready for this device.' });
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setPushStatus({ tone: 'error', text: formatPushRegistrationError(error) });
      }
    }

    registerDeviceForPush();
    return () => {
      isMounted = false;
    };
  }, [authState, session?.accessToken]);

  useEffect(() => {
    let isMounted = true;

    async function loadHouseholdReadModels() {
      if (authState !== 'signedIn' || !session?.accessToken || !selectedHouseholdId) {
        setReadModels(emptyReadModels());
        setReadStatus('idle');
        setReadError(null);
        return;
      }

      setReadStatus('loading');
      setReadError(null);

      try {
        const client = createHomeTuskApiClient({ accessToken: session.accessToken });
        const [members, zones, tasks, shoppingLists, notifications] = await Promise.all([
          client.getHouseholdMembers(selectedHouseholdId),
          client.getZones(selectedHouseholdId),
          client.getTasks(selectedHouseholdId),
          client.getShoppingLists(selectedHouseholdId),
          client.listNotifications(selectedHouseholdId, 20),
        ]);

        const shoppingItemGroups = await Promise.all(
          shoppingLists.map((list) => client.getShoppingItems(selectedHouseholdId, list.id))
        );
        const shoppingItems = shoppingItemGroups.reduce<ShoppingItem[]>(
          (items, group) => items.concat(group),
          []
        );

        if (!isMounted) {
          return;
        }
        setReadModels({ members, zones, tasks, shoppingLists, shoppingItems, notifications });
        setReadStatus('ready');
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setReadError(formatReadError(error));
        setReadStatus('error');
      }
    }

    loadHouseholdReadModels();
    return () => {
      isMounted = false;
    };
  }, [authState, readReloadKey, selectedHouseholdId, session?.accessToken]);

  useEffect(() => {
    let isMounted = true;

    async function loadRecentCommandHints() {
      if (!selectedHouseholdId) {
        setRecentCommands([]);
        return;
      }
      const stored = await readRecentCommands();
      if (!isMounted) {
        return;
      }
      setRecentCommands(stored.filter((entry) => entry.householdId === selectedHouseholdId));
    }

    loadRecentCommandHints();
    return () => {
      isMounted = false;
    };
  }, [selectedHouseholdId]);

  useEffect(() => {
    if (readModels.shoppingLists.length === 0) {
      setSelectedShoppingListId(null);
      return;
    }
    const stillAvailable = readModels.shoppingLists.some((list) => list.id === selectedShoppingListId);
    if (!stillAvailable) {
      setSelectedShoppingListId(readModels.shoppingLists[0].id);
    }
  }, [readModels.shoppingLists, selectedShoppingListId]);

  async function handleAuthSubmit() {
    const trimmedEmail = email.trim().toLowerCase();
    const trimmedName = displayName.trim();

    if (!trimmedEmail || !password) {
      setAuthError('Email and password are required.');
      return;
    }
    if (authMode === 'register' && !trimmedName) {
      setAuthError('Name is required for registration.');
      return;
    }

    setIsSubmitting(true);
    setAuthError(null);

    try {
      const client = createHomeTuskApiClient();
      const auth =
        authMode === 'login'
          ? await client.mobileLogin({ email: trimmedEmail, password })
          : await client.mobileRegister({ name: trimmedName, email: trimmedEmail, password });

      const nextSession = createSecureSession(auth);
      await writeSecureSession(nextSession);

      const opened = await openSession(nextSession, false);
      await applyOpenedSession(opened);
      setPassword('');
    } catch (error) {
      await clearSecureSession();
      await clearMobileDeviceRegistration();
      setSession(null);
      setProfile(null);
      setSelectedHouseholdId(null);
      setReadModels(emptyReadModels());
      setReadStatus('idle');
      setAuthState('signedOut');
      setAuthError(formatAuthError(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSelectHousehold(householdId: string) {
    setSelectedHouseholdId(householdId);
    setMutationMessage(null);
    setMutationError(null);
    await writeSelectedHouseholdId(householdId);
  }

  async function handleCreateTask() {
    const trimmedTitle = taskTitle.trim();
    if (!trimmedTitle || !session?.accessToken || !selectedHouseholdId) {
      setMutationError('Task title and selected household are required.');
      return;
    }

    setSavingAction('create-task');
    setMutationMessage(null);
    setMutationError(null);

    try {
      const response = await createHomeTuskApiClient({ accessToken: session.accessToken }).executeCommand(
        {
          householdId: selectedHouseholdId,
          type: 'create_task',
          payload: { title: trimmedTitle },
          source: 'mobile',
          clientTimestamp: new Date().toISOString(),
        },
        generateClientUuid()
      );
      setTaskTitle('');
      setMutationMessage(formatCommandOutcome(response.status));
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  async function handleCompleteTask(taskId: string) {
    if (!session?.accessToken || !selectedHouseholdId) {
      setMutationError('Selected household is required.');
      return;
    }

    setSavingAction(`complete-task:${taskId}`);
    setMutationMessage(null);
    setMutationError(null);

    try {
      const response = await createHomeTuskApiClient({ accessToken: session.accessToken }).executeCommand(
        {
          householdId: selectedHouseholdId,
          type: 'complete_task',
          payload: { taskId },
          source: 'mobile',
          clientTimestamp: new Date().toISOString(),
        },
        generateClientUuid()
      );
      setMutationMessage(formatCommandOutcome(response.status));
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  async function handleAddShoppingItem() {
    const trimmedName = shoppingItemName.trim();
    if (!trimmedName || !session?.accessToken || !selectedHouseholdId || !selectedShoppingListId) {
      setMutationError('Item name and shopping list are required.');
      return;
    }

    setSavingAction('add-shopping-item');
    setMutationMessage(null);
    setMutationError(null);

    try {
      await createHomeTuskApiClient({ accessToken: session.accessToken }).addShoppingItem(
        selectedHouseholdId,
        selectedShoppingListId,
        { name: trimmedName }
      );
      setShoppingItemName('');
      setMutationMessage('Shopping item added.');
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  async function handleMarkPurchased(itemId: string) {
    if (!session?.accessToken || !selectedHouseholdId) {
      setMutationError('Selected household is required.');
      return;
    }

    setSavingAction(`purchase-item:${itemId}`);
    setMutationMessage(null);
    setMutationError(null);

    try {
      await createHomeTuskApiClient({ accessToken: session.accessToken }).updateShoppingItem(
        selectedHouseholdId,
        itemId,
        { purchased: true }
      );
      setMutationMessage('Shopping item marked purchased.');
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  async function handleDeleteShoppingItem(itemId: string) {
    if (!session?.accessToken || !selectedHouseholdId) {
      setMutationError('Selected household is required.');
      return;
    }

    setSavingAction(`delete-item:${itemId}`);
    setMutationMessage(null);
    setMutationError(null);

    try {
      await createHomeTuskApiClient({ accessToken: session.accessToken }).deleteShoppingItem(
        selectedHouseholdId,
        itemId
      );
      setMutationMessage('Shopping item deleted.');
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  async function handleSubmitCommand() {
    if (!session?.accessToken || !selectedHouseholdId) {
      setCommandError('Selected household is required.');
      return;
    }

    const parsed = buildCommandRequestFromText(commandText, selectedHouseholdId, readModels.tasks);
    if ('error' in parsed) {
      setCommandError(parsed.error);
      return;
    }

    setCommandSaving(true);
    setCommandError(null);
    setMutationMessage(null);
    setMutationError(null);

    try {
      const response = await createHomeTuskApiClient({ accessToken: session.accessToken }).executeCommand(
        parsed.request,
        generateClientUuid()
      );
      setCommandResponse(response);
      setCommandText('');
      await storeRecentCommandHint(selectedHouseholdId, parsed.displayText, response, setRecentCommands);
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setCommandError(formatMutationError(error));
    } finally {
      setCommandSaving(false);
    }
  }

  async function handleContinueCommand() {
    if (!session?.accessToken || !selectedHouseholdId || commandResponse?.status !== 'needs_input') {
      setCommandError('A command waiting for input is required.');
      return;
    }

    const trimmed = continuationText.trim();
    if (!trimmed) {
      setCommandError('Additional input is required.');
      return;
    }

    setCommandSaving(true);
    setCommandError(null);

    try {
      const response = await createHomeTuskApiClient({ accessToken: session.accessToken }).continueCommand(
        commandResponse.commandId,
        { additionalInput: parseContinuationInput(trimmed) }
      );
      setCommandResponse(response);
      setContinuationText('');
      await storeRecentCommandHint(
        selectedHouseholdId,
        `Continue: ${trimmed}`,
        response,
        setRecentCommands
      );
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setCommandError(formatMutationError(error));
    } finally {
      setCommandSaving(false);
    }
  }

  async function handleLogout() {
    setIsSubmitting(true);
    setAuthError(null);

    try {
      const storedDevice = await readMobileDeviceRegistration();
      if (session?.accessToken && storedDevice?.deviceId) {
        await createHomeTuskApiClient({ accessToken: session.accessToken }).deactivateMobileDevice(
          storedDevice.deviceId
        );
      }
      if (session?.refreshToken) {
        await createHomeTuskApiClient().mobileLogout({ refreshToken: session.refreshToken });
      }
    } catch {
      // Logout is best effort: local secure session removal is authoritative for this device.
    } finally {
      await clearSecureSession();
      await clearMobileDeviceRegistration();
      setSession(null);
      setProfile(null);
      setSelectedHouseholdId(null);
      setReadModels(emptyReadModels());
      setReadStatus('idle');
      setTaskTitle('');
      setShoppingItemName('');
      setCommandText('');
      setContinuationText('');
      setCommandResponse(null);
      setCommandError(null);
      setRecentCommands([]);
      setPushStatus(null);
      setLinkStatus(null);
      setPendingLinkTarget(null);
      setHighlightedTaskId(null);
      setMutationMessage(null);
      setMutationError(null);
      setPassword('');
      setAuthState('signedOut');
      setIsSubmitting(false);
    }
  }

  if (authState === 'checking') {
    return (
      <SafeAreaView style={styles.safeArea}>
        <StatusBar style="auto" />
        <View style={styles.loadingShell}>
          <ActivityIndicator size="large" color="#1d7f68" />
          <Text style={styles.loadingText}>Opening HomeTusk</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (authState === 'signedOut' || !profile) {
    return (
      <AuthScreen
        authMode={authMode}
        email={email}
        password={password}
        displayName={displayName}
        error={authError}
        isSubmitting={isSubmitting}
        onChangeAuthMode={setAuthMode}
        onChangeDisplayName={setDisplayName}
        onChangeEmail={setEmail}
        onChangePassword={setPassword}
        onSubmit={handleAuthSubmit}
      />
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="auto" />
      <View style={styles.appShell}>
        <View style={styles.header}>
          <View style={styles.headerIdentity}>
            <Text style={styles.eyebrow}>HomeTusk</Text>
            <Text style={styles.title}>{profile.displayName}</Text>
            <Text style={styles.subtitle}>{profile.email ?? 'Signed in'}</Text>
          </View>
          <Pressable
            accessibilityRole="button"
            disabled={isSubmitting}
            onPress={handleLogout}
            style={({ pressed }) => [
              styles.logoutButton,
              pressed && styles.buttonPressed,
              isSubmitting && styles.buttonDisabled,
            ]}
          >
            <Text style={styles.logoutText}>Logout</Text>
          </Pressable>
        </View>

        <HouseholdSwitcher
          households={profile.households}
          selectedHouseholdId={selectedHouseholdId}
          onSelect={handleSelectHousehold}
        />

        <StatusBanner status={pushStatus} />
        <StatusBanner status={linkStatus} />

        <View style={styles.tabBar} accessibilityRole="tablist">
          {surfaces.map((item) => {
            const isActive = item.key === activeSurface;
            return (
              <Pressable
                key={item.key}
                accessibilityRole="tab"
                accessibilityState={{ selected: isActive }}
                onPress={() => setActiveSurface(item.key)}
                style={[styles.tab, isActive && styles.tabActive]}
              >
                <Text style={[styles.tabLabel, isActive && styles.tabLabelActive]}>
                  {item.label}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
          <View style={[styles.surfaceBand, { borderLeftColor: surface.accent }]}>
            <Text style={styles.surfaceTitle}>{surface.title}</Text>
            <Text style={styles.surfaceSummary}>
              {selectedHousehold ? selectedHousehold.name : surface.summary}
            </Text>
          </View>
          <SurfacePanel
            accent={surface.accent}
            models={readModels}
            mutationError={mutationError}
            mutationMessage={mutationMessage}
            mutationControls={{
              taskTitle,
              shoppingItemName,
              selectedShoppingListId,
              savingAction,
              onAddShoppingItem: handleAddShoppingItem,
              onChangeShoppingItemName: setShoppingItemName,
              onChangeTaskTitle: setTaskTitle,
              onCompleteTask: handleCompleteTask,
              onCreateTask: handleCreateTask,
              onDeleteShoppingItem: handleDeleteShoppingItem,
              onMarkPurchased: handleMarkPurchased,
              onSelectShoppingList: setSelectedShoppingListId,
            }}
            commandControls={{
              commandText,
              continuationText,
              error: commandError,
              isSaving: commandSaving,
              recentCommands,
              response: commandResponse,
              onChangeCommandText: setCommandText,
              onChangeContinuationText: setContinuationText,
              onContinueCommand: handleContinueCommand,
              onSubmitCommand: handleSubmitCommand,
            }}
            highlightedTaskId={highlightedTaskId}
            onRetry={() => setReadReloadKey((value) => value + 1)}
            profile={profile}
            readError={readError}
            readStatus={readStatus}
            selectedHousehold={selectedHousehold}
            surfaceKey={surface.key}
          />
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

function AuthScreen({
  authMode,
  email,
  password,
  displayName,
  error,
  isSubmitting,
  onChangeAuthMode,
  onChangeDisplayName,
  onChangeEmail,
  onChangePassword,
  onSubmit,
}: {
  authMode: AuthMode;
  email: string;
  password: string;
  displayName: string;
  error: string | null;
  isSubmitting: boolean;
  onChangeAuthMode: (mode: AuthMode) => void;
  onChangeDisplayName: (value: string) => void;
  onChangeEmail: (value: string) => void;
  onChangePassword: (value: string) => void;
  onSubmit: () => void;
}) {
  const isRegister = authMode === 'register';

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="auto" />
      <ScrollView contentContainerStyle={styles.authShell} keyboardShouldPersistTaps="handled">
        <View style={styles.authHeader}>
          <Text style={styles.eyebrow}>HomeTusk</Text>
          <Text style={styles.authTitle}>Home in your pocket</Text>
          <Text style={styles.subtitle}>Sign in to load households, tasks, shopping, and commands.</Text>
        </View>

        <View style={styles.modeSwitch}>
          <Pressable
            accessibilityRole="button"
            onPress={() => onChangeAuthMode('login')}
            style={[styles.modeButton, authMode === 'login' && styles.modeButtonActive]}
          >
            <Text style={[styles.modeText, authMode === 'login' && styles.modeTextActive]}>
              Login
            </Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            onPress={() => onChangeAuthMode('register')}
            style={[styles.modeButton, authMode === 'register' && styles.modeButtonActive]}
          >
            <Text style={[styles.modeText, authMode === 'register' && styles.modeTextActive]}>
              Register
            </Text>
          </Pressable>
        </View>

        <View style={styles.form}>
          {isRegister && (
            <LabeledInput
              label="Name"
              value={displayName}
              onChangeText={onChangeDisplayName}
              placeholder="Alice Test"
              editable={!isSubmitting}
            />
          )}
          <LabeledInput
            label="Email"
            value={email}
            onChangeText={onChangeEmail}
            placeholder="alice@example.com"
            editable={!isSubmitting}
            keyboardType="email-address"
          />
          <LabeledInput
            label="Password"
            value={password}
            onChangeText={onChangePassword}
            placeholder="Password"
            editable={!isSubmitting}
            secureTextEntry
          />

          {error && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <Pressable
            accessibilityRole="button"
            disabled={isSubmitting}
            onPress={onSubmit}
            style={({ pressed }) => [
              styles.primaryButton,
              pressed && styles.buttonPressed,
              isSubmitting && styles.buttonDisabled,
            ]}
          >
            {isSubmitting ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.primaryButtonText}>{isRegister ? 'Create account' : 'Login'}</Text>
            )}
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function LabeledInput({
  label,
  value,
  onChangeText,
  placeholder,
  editable,
  keyboardType,
  secureTextEntry,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  editable: boolean;
  keyboardType?: 'default' | 'email-address';
  secureTextEntry?: boolean;
}) {
  return (
    <View style={styles.inputGroup}>
      <Text style={styles.inputLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        editable={editable}
        keyboardType={keyboardType ?? 'default'}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#8a908d"
        secureTextEntry={secureTextEntry}
        style={styles.input}
        value={value}
      />
    </View>
  );
}

function HouseholdSwitcher({
  households,
  selectedHouseholdId,
  onSelect,
}: {
  households: HouseholdSummary[];
  selectedHouseholdId: string | null;
  onSelect: (householdId: string) => void;
}) {
  if (households.length === 0) {
    return (
      <View style={styles.householdStrip}>
        <Text style={styles.householdStripText}>No households</Text>
      </View>
    );
  }

  return (
    <ScrollView
      contentContainerStyle={styles.householdChips}
      horizontal
      showsHorizontalScrollIndicator={false}
    >
      {households.map((household) => {
        const selected = household.id === selectedHouseholdId;
        return (
          <Pressable
            accessibilityRole="button"
            key={household.id}
            onPress={() => onSelect(household.id)}
            style={[styles.householdChip, selected && styles.householdChipActive]}
          >
            <Text style={[styles.householdChipText, selected && styles.householdChipTextActive]}>
              {household.name}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

function StatusBanner({ status }: { status: StatusBannerMessage | null }) {
  if (!status) {
    return null;
  }

  return (
    <View
      style={[
        styles.statusBanner,
        status.tone === 'success' && styles.statusBannerSuccess,
        status.tone === 'error' && styles.statusBannerError,
      ]}
    >
      <Text
        style={[
          styles.statusBannerText,
          status.tone === 'error' && styles.statusBannerTextError,
        ]}
      >
        {status.text}
      </Text>
    </View>
  );
}

function SurfacePanel({
  surfaceKey,
  profile,
  selectedHousehold,
  models,
  readStatus,
  readError,
  mutationMessage,
  mutationError,
  mutationControls,
  commandControls,
  highlightedTaskId,
  accent,
  onRetry,
}: {
  surfaceKey: SurfaceKey;
  profile: UserProfile;
  selectedHousehold: HouseholdSummary | null;
  models: HouseholdReadModels;
  readStatus: ReadStatus;
  readError: string | null;
  mutationMessage: string | null;
  mutationError: string | null;
  mutationControls: MutationControls;
  commandControls: CommandChatControls;
  highlightedTaskId: string | null;
  accent: ColorValue;
  onRetry: () => void;
}) {
  if (!selectedHousehold) {
    return (
      <DataSurface
        accent={accent}
        title="No household selected"
        body="Create or join a household, then return to this app session."
      />
    );
  }

  if (readStatus === 'loading') {
    return <LoadingPanel label="Loading household" />;
  }

  if (readStatus === 'error') {
    return <ErrorPanel message={readError ?? 'Could not load household data.'} onRetry={onRetry} />;
  }

  if (surfaceKey === 'home') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <HomeSurface
          accent={accent}
          models={models}
          profile={profile}
          selectedHousehold={selectedHousehold}
        />
      </>
    );
  }
  if (surfaceKey === 'tasks') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <TasksSurface
          accent={accent}
          controls={mutationControls}
          highlightedTaskId={highlightedTaskId}
          tasks={models.tasks}
        />
      </>
    );
  }
  if (surfaceKey === 'shopping') {
    return (
      <>
        <MutationFeedback message={mutationMessage} error={mutationError} />
        <ShoppingSurface
          accent={accent}
          controls={mutationControls}
          items={models.shoppingItems}
          lists={models.shoppingLists}
        />
      </>
    );
  }
  return (
    <>
      <MutationFeedback message={mutationMessage} error={mutationError} />
      <CommandSurface
        accent={accent}
        controls={commandControls}
        models={models}
        selectedHousehold={selectedHousehold}
      />
    </>
  );
}

function HomeSurface({
  profile,
  selectedHousehold,
  models,
  accent,
}: {
  profile: UserProfile;
  selectedHousehold: HouseholdSummary;
  models: HouseholdReadModels;
  accent: ColorValue;
}) {
  const openTaskCount = models.tasks.filter((task) => task.status !== 'done').length;
  const unreadCount = models.notifications.filter((notification) => !notification.readAt).length;
  const unpurchasedCount = models.shoppingLists.reduce(
    (total, list) => total + list.unpurchasedCount,
    0
  );

  return (
    <View style={styles.section}>
      <View style={styles.statsGrid}>
        <StatTile label="Open tasks" value={String(openTaskCount)} />
        <StatTile label="Members" value={String(models.members.length)} />
        <StatTile label="To buy" value={String(unpurchasedCount)} />
        <StatTile label="Unread" value={String(unreadCount)} />
      </View>

      <View style={styles.profilePanel}>
        <Text style={styles.sectionTitle}>{selectedHousehold.name}</Text>
        <Text style={styles.profileName}>{profile.displayName}</Text>
        <Text style={styles.profileMeta}>{profile.email ?? profile.externalId}</Text>
      </View>

      <SectionList title="Members">
        {models.members.length === 0 ? (
          <EmptyRow accent={accent} title="No members loaded" />
        ) : (
          models.members.map((member) => (
            <InfoRow
              accent={accent}
              key={member.userId}
              meta={member.role}
              title={member.displayName}
            />
          ))
        )}
      </SectionList>

      <SectionList title="Zones">
        {models.zones.length === 0 ? (
          <EmptyRow accent={accent} title="No zones yet" />
        ) : (
          models.zones.map((zone) => (
            <InfoRow
              accent={accent}
              key={zone.id}
              meta={`Created ${formatShortDate(zone.createdAt)}`}
              title={zone.name}
            />
          ))
        )}
      </SectionList>

      <SectionList title="Notifications">
        {models.notifications.length === 0 ? (
          <EmptyRow accent={accent} title="No notifications" />
        ) : (
          models.notifications.slice(0, 5).map((notification) => (
            <InfoRow
              accent={accent}
              key={notification.id}
              meta={notification.readAt ? 'Read' : 'Unread'}
              title={notification.payload.summary ?? formatNotificationType(notification.type)}
            />
          ))
        )}
      </SectionList>
    </View>
  );
}

function TasksSurface({
  tasks,
  accent,
  controls,
  highlightedTaskId,
}: {
  tasks: Task[];
  accent: ColorValue;
  controls: MutationControls;
  highlightedTaskId: string | null;
}) {
  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <LabeledInput
          editable={!controls.savingAction}
          label="Task title"
          onChangeText={controls.onChangeTaskTitle}
          placeholder="Take out recycling"
          value={controls.taskTitle}
        />
        <Pressable
          accessibilityRole="button"
          disabled={Boolean(controls.savingAction)}
          onPress={controls.onCreateTask}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && styles.buttonPressed,
            controls.savingAction && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>
            {controls.savingAction === 'create-task' ? 'Creating...' : 'Create task'}
          </Text>
        </Pressable>
      </View>

      {tasks.length === 0 ? (
        <DataSurface accent={accent} title="No tasks yet" body="Create the first task above." />
      ) : (
        <SectionList title={`${tasks.length} tasks`}>
          {tasks.map((task) => {
            const isHighlighted = task.id === highlightedTaskId;
            return (
              <View
                key={task.id}
                style={[styles.entityRow, isHighlighted && styles.entityRowHighlighted]}
              >
                <View
                  style={[
                    styles.checkDot,
                    { backgroundColor: isHighlighted ? '#c78b00' : accent },
                  ]}
                />
                <View style={styles.entityCopy}>
                  <Text style={styles.entityTitle}>{task.title}</Text>
                  <Text style={styles.entityMeta}>
                    {formatTaskStatus(task.status)}
                    {task.assignee ? ` - ${task.assignee.displayName}` : ''}
                    {task.zone ? ` - ${task.zone.name}` : ''}
                  </Text>
                  {task.deadline && (
                    <Text style={styles.entityMeta}>Due {formatShortDate(task.deadline)}</Text>
                  )}
                </View>
                {task.status !== 'done' && (
                  <Pressable
                    accessibilityRole="button"
                    disabled={Boolean(controls.savingAction)}
                    onPress={() => controls.onCompleteTask(task.id)}
                    style={({ pressed }) => [
                      styles.smallButton,
                      pressed && styles.buttonPressed,
                      controls.savingAction && styles.buttonDisabled,
                    ]}
                  >
                    <Text style={styles.smallButtonText}>
                      {controls.savingAction === `complete-task:${task.id}` ? '...' : 'Done'}
                    </Text>
                  </Pressable>
                )}
              </View>
            );
          })}
        </SectionList>
      )}
    </View>
  );
}

function ShoppingSurface({
  lists,
  items,
  accent,
  controls,
}: {
  lists: ShoppingList[];
  items: ShoppingItem[];
  accent: ColorValue;
  controls: MutationControls;
}) {
  if (lists.length === 0) {
    return (
      <DataSurface
        accent={accent}
        title="No shopping lists"
        body="Create a shopping list from web or command flow, then add items here."
      />
    );
  }

  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <ScrollView
          contentContainerStyle={styles.shoppingListChips}
          horizontal
          showsHorizontalScrollIndicator={false}
        >
          {lists.map((list) => {
            const selected = list.id === controls.selectedShoppingListId;
            return (
              <Pressable
                accessibilityRole="button"
                disabled={Boolean(controls.savingAction)}
                key={list.id}
                onPress={() => controls.onSelectShoppingList(list.id)}
                style={[styles.listChip, selected && styles.listChipActive]}
              >
                <Text style={[styles.listChipText, selected && styles.listChipTextActive]}>
                  {list.name}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
        <LabeledInput
          editable={!controls.savingAction}
          label="Item"
          onChangeText={controls.onChangeShoppingItemName}
          placeholder="Milk"
          value={controls.shoppingItemName}
        />
        <Pressable
          accessibilityRole="button"
          disabled={Boolean(controls.savingAction)}
          onPress={controls.onAddShoppingItem}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && styles.buttonPressed,
            controls.savingAction && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>
            {controls.savingAction === 'add-shopping-item' ? 'Adding...' : 'Add item'}
          </Text>
        </Pressable>
      </View>

      <SectionList title={`${lists.length} shopping lists`}>
        {lists.map((list) => {
          const listItems = items.filter((item) => item.listId === list.id);
          return (
            <View key={list.id} style={styles.listBlock}>
              <View style={styles.listHeader}>
                <Text style={styles.entityTitle}>{list.name}</Text>
                <Text style={styles.entityMeta}>{list.unpurchasedCount} to buy</Text>
              </View>
              {listItems.length === 0 ? (
                <EmptyRow accent={accent} title="No items in this list" />
              ) : (
                listItems.slice(0, 6).map((item) => (
                  <View key={item.id} style={styles.entityRow}>
                    <View style={[styles.checkDot, { backgroundColor: accent }]} />
                    <View style={styles.entityCopy}>
                      <Text style={styles.entityTitle}>{item.name}</Text>
                      <Text style={styles.entityMeta}>
                        {item.purchased ? 'Purchased' : item.quantity ? `Qty ${item.quantity}` : 'To buy'}
                        {item.linkedTaskId ? ` - linked ${shortId(item.linkedTaskId)}` : ''}
                      </Text>
                    </View>
                    <View style={styles.rowActions}>
                      {!item.purchased && (
                        <Pressable
                          accessibilityRole="button"
                          disabled={Boolean(controls.savingAction)}
                          onPress={() => controls.onMarkPurchased(item.id)}
                          style={({ pressed }) => [
                            styles.smallButton,
                            pressed && styles.buttonPressed,
                            controls.savingAction && styles.buttonDisabled,
                          ]}
                        >
                          <Text style={styles.smallButtonText}>
                            {controls.savingAction === `purchase-item:${item.id}` ? '...' : 'Buy'}
                          </Text>
                        </Pressable>
                      )}
                      <Pressable
                        accessibilityRole="button"
                        disabled={Boolean(controls.savingAction)}
                        onPress={() => controls.onDeleteShoppingItem(item.id)}
                        style={({ pressed }) => [
                          styles.smallDangerButton,
                          pressed && styles.buttonPressed,
                          controls.savingAction && styles.buttonDisabled,
                        ]}
                      >
                        <Text style={styles.smallDangerText}>
                          {controls.savingAction === `delete-item:${item.id}` ? '...' : 'Delete'}
                        </Text>
                      </Pressable>
                    </View>
                  </View>
                ))
              )}
            </View>
          );
        })}
      </SectionList>
    </View>
  );
}

function MutationFeedback({ message, error }: { message: string | null; error: string | null }) {
  if (!message && !error) {
    return null;
  }

  return (
    <View style={[styles.feedbackPanel, error ? styles.feedbackPanelError : styles.feedbackPanelSuccess]}>
      <Text style={error ? styles.feedbackTextError : styles.feedbackTextSuccess}>
        {error ?? message}
      </Text>
    </View>
  );
}

function CommandSurface({
  selectedHousehold,
  models,
  accent,
  controls,
}: {
  selectedHousehold: HouseholdSummary;
  models: HouseholdReadModels;
  accent: ColorValue;
  controls: CommandChatControls;
}) {
  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <Text style={styles.sectionTitle}>{selectedHousehold.name}</Text>
        <Text style={styles.entityMeta}>
          {models.members.length} members, {models.zones.length} zones, {models.tasks.length} tasks loaded
        </Text>
        <LabeledInput
          editable={!controls.isSaving}
          label="Command"
          onChangeText={controls.onChangeCommandText}
          placeholder="Take out recycling"
          value={controls.commandText}
        />
        <Text style={styles.hintText}>Use plain text to create a task, or "done task title" to complete one.</Text>
        <Pressable
          accessibilityRole="button"
          disabled={controls.isSaving}
          onPress={controls.onSubmitCommand}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && styles.buttonPressed,
            controls.isSaving && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>{controls.isSaving ? 'Sending...' : 'Send command'}</Text>
        </Pressable>
      </View>

      {controls.error && (
        <View style={[styles.feedbackPanel, styles.feedbackPanelError]}>
          <Text style={styles.feedbackTextError}>{controls.error}</Text>
        </View>
      )}

      {controls.response && <CommandOutcomeCard accent={accent} response={controls.response} />}

      {controls.response?.status === 'needs_input' && (
        <View style={styles.formPanel}>
          <LabeledInput
            editable={!controls.isSaving}
            label="Additional input"
            onChangeText={controls.onChangeContinuationText}
            placeholder="assigneeId=..."
            value={controls.continuationText}
          />
          <Text style={styles.hintText}>Use key=value pairs when a specific field is requested.</Text>
          <Pressable
            accessibilityRole="button"
            disabled={controls.isSaving}
            onPress={controls.onContinueCommand}
            style={({ pressed }) => [
              styles.primaryButton,
              pressed && styles.buttonPressed,
              controls.isSaving && styles.buttonDisabled,
            ]}
          >
            <Text style={styles.primaryButtonText}>Continue</Text>
          </Pressable>
        </View>
      )}

      <SectionList title="Recent commands">
        {controls.recentCommands.length === 0 ? (
          <EmptyRow accent={accent} title="No recent commands" />
        ) : (
          controls.recentCommands.slice(0, 8).map((entry) => (
            <InfoRow
              accent={accent}
              key={`${entry.id}-${entry.createdAt}`}
              meta={`${entry.status} - ${formatShortDate(entry.createdAt)}`}
              title={entry.text}
            />
          ))
        )}
      </SectionList>
    </View>
  );
}

function CommandOutcomeCard({
  response,
  accent,
}: {
  response: CommandResponse;
  accent: ColorValue;
}) {
  const title = formatCommandOutcome(response.status);
  const body = getCommandOutcomeBody(response);

  return (
    <View style={styles.dataPanel}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.dataCopy}>
        <Text style={styles.dataTitle}>{title}</Text>
        <Text style={styles.dataBody}>{body}</Text>
        <Text style={styles.entityMeta}>Command {shortId(response.commandId)}</Text>
      </View>
    </View>
  );
}

function SectionList({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.statTile}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

function InfoRow({
  title,
  meta,
  accent,
}: {
  title: string;
  meta?: string | null;
  accent: ColorValue;
}) {
  return (
    <View style={styles.entityRow}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.entityCopy}>
        <Text style={styles.entityTitle}>{title}</Text>
        {meta && <Text style={styles.entityMeta}>{meta}</Text>}
      </View>
    </View>
  );
}

function EmptyRow({ title, accent }: { title: string; accent: ColorValue }) {
  return (
    <View style={styles.entityRow}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <Text style={styles.entityMeta}>{title}</Text>
    </View>
  );
}

function LoadingPanel({ label }: { label: string }) {
  return (
    <View style={styles.statePanel}>
      <ActivityIndicator size="small" color="#1d7f68" />
      <Text style={styles.stateText}>{label}</Text>
    </View>
  );
}

function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <View style={styles.statePanel}>
      <Text style={styles.errorText}>{message}</Text>
      <Pressable accessibilityRole="button" onPress={onRetry} style={styles.retryButton}>
        <Text style={styles.retryText}>Retry</Text>
      </Pressable>
    </View>
  );
}

function DataSurface({
  title,
  body,
  accent,
}: {
  title: string;
  body: string;
  accent: ColorValue;
}) {
  return (
    <View style={styles.dataPanel}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.dataCopy}>
        <Text style={styles.dataTitle}>{title}</Text>
        <Text style={styles.dataBody}>{body}</Text>
      </View>
    </View>
  );
}

async function resolveSelectedHouseholdId(profile: UserProfile): Promise<string | null> {
  const storedHouseholdId = await readSelectedHouseholdId();
  const storedIsValid = profile.households.some((household) => household.id === storedHouseholdId);
  const nextHouseholdId = storedIsValid ? storedHouseholdId : profile.households[0]?.id ?? null;
  await writeSelectedHouseholdId(nextHouseholdId);
  return nextHouseholdId;
}

function formatAuthError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.body.errorCode === 'AUTH_INVALID_CREDENTIALS') {
      return 'Email or password is incorrect.';
    }
    if (error.body.errorCode === 'AUTH_EMAIL_EXISTS') {
      return 'An account with this email already exists.';
    }
    if (error.body.errorCode === 'AUTH_PROVIDER_UNAVAILABLE') {
      return 'Authentication provider is unavailable. Try again later.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not open the session. Check the backend URL and try again.';
}

function formatReadError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Session needs a refresh. Logout and sign in again if retry fails.';
    }
    if (error.status === 403) {
      return 'This account cannot open the selected household.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not load household data. Check the backend URL and try again.';
}

function formatMutationError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 403 || error.status === 404) {
      return 'Could not save inside the selected household.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk API returned ${error.status}.`;
  }
  return 'Could not save the change. Check the backend URL and try again.';
}

function formatPushRegistrationError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Push registration needs a fresh sign-in.';
    }
    if (error.status === 409) {
      return 'This push token is already active for another device registration.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `Push registration failed with API ${error.status}.`;
  }
  return 'Could not register this device for push.';
}

function formatLinkError(error: unknown): string {
  if (error instanceof HomeTuskApiError) {
    if (error.status === 401) {
      return 'Sign in again to open this HomeTusk link.';
    }
    if (error.status === 403 || error.status === 404) {
      return 'This link target is not available for this account.';
    }
    if (error.status === 410) {
      return 'This invite is no longer available.';
    }
    if (error.body.message) {
      return error.body.message;
    }
    return `HomeTusk link failed with API ${error.status}.`;
  }
  return 'Could not open this HomeTusk link.';
}

function buildCommandRequestFromText(
  text: string,
  householdId: string,
  tasks: Task[]
): { request: CommandRequest; displayText: string } | { error: string } {
  const trimmed = text.trim();
  if (!trimmed) {
    return { error: 'Command text is required.' };
  }

  const completeMatch = /^(done|complete)\s+(.+)$/i.exec(trimmed);
  if (completeMatch) {
    const needle = completeMatch[2].trim().toLowerCase();
    const task = tasks.find((candidate) => {
      if (candidate.status === 'done') {
        return false;
      }
      return candidate.id.toLowerCase().startsWith(needle) || candidate.title.toLowerCase().includes(needle);
    });

    if (!task) {
      return { error: 'No open task matched that command.' };
    }

    return {
      request: {
        householdId,
        type: 'complete_task',
        payload: { taskId: task.id },
        source: 'mobile',
        clientTimestamp: new Date().toISOString(),
      },
      displayText: `Done: ${task.title}`,
    };
  }

  const title = trimmed.replace(/^create\s+/i, '').trim();
  if (!title) {
    return { error: 'Task title is required.' };
  }
  return {
    request: {
      householdId,
      type: 'create_task',
      payload: { title },
      source: 'mobile',
      clientTimestamp: new Date().toISOString(),
    },
    displayText: `Create task: ${title.length > 40 ? `${title.slice(0, 40)}...` : title}`,
  };
}

function parseContinuationInput(value: string): Record<string, unknown> {
  const pairs = value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.split('=').map((piece) => piece.trim()));

  if (pairs.length > 0 && pairs.every((pair) => pair.length === 2 && pair[0] && pair[1])) {
    return Object.fromEntries(pairs);
  }

  return { clarification: value };
}

async function storeRecentCommandHint(
  householdId: string,
  text: string,
  response: CommandResponse,
  updateState: (entries: RecentCommandHint[]) => void
): Promise<void> {
  const stored = await readRecentCommands();
  const entry: RecentCommandHint = {
    id: response.commandId,
    householdId,
    text,
    status: response.status,
    createdAt: new Date().toISOString(),
  };
  const next = [entry, ...stored.filter((candidate) => candidate.id !== response.commandId)];
  await writeRecentCommands(next);
  updateState(next.filter((candidate) => candidate.householdId === householdId).slice(0, 20));
}

function formatCommandOutcome(status: string): string {
  if (status === 'executed') {
    return 'Command executed.';
  }
  if (status === 'executed_degraded') {
    return 'Command executed with fallback behavior.';
  }
  if (status === 'scheduled') {
    return 'Command scheduled.';
  }
  if (status === 'needs_input') {
    return 'Command needs more input.';
  }
  if (status === 'rejected') {
    return 'Command rejected by HomeTusk rules.';
  }
  return `Command status: ${status}.`;
}

function getCommandOutcomeBody(response: CommandResponse): string {
  if (response.status === 'needs_input') {
    return response.question ?? 'HomeTusk needs more input.';
  }
  if (response.status === 'rejected') {
    return response.reason ?? response.errorCode ?? 'HomeTusk rejected this command.';
  }
  if (response.status === 'scheduled') {
    return response.scheduleAt ? `Scheduled for ${formatShortDate(response.scheduleAt)}.` : 'Scheduled.';
  }
  if (response.status === 'executed_degraded') {
    return response.degradedReason ?? response.fallbackStrategy ?? 'Fallback behavior was used.';
  }
  return 'HomeTusk recorded the decision and action.';
}

function shortId(value: string): string {
  return value.slice(0, 8);
}

function formatTaskStatus(status: string): string {
  return status.replace(/_/g, ' ');
}

function formatNotificationType(type: string): string {
  return type.replace(/_/g, ' ');
}

function formatShortDate(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.slice(0, 10);
  }
  return parsed.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#f7f5ef',
  },
  loadingShell: {
    alignItems: 'center',
    flex: 1,
    gap: 14,
    justifyContent: 'center',
    padding: 24,
  },
  loadingText: {
    color: '#33413c',
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0,
  },
  appShell: {
    flex: 1,
    paddingHorizontal: 20,
    paddingTop: 18,
  },
  authShell: {
    flexGrow: 1,
    gap: 18,
    justifyContent: 'center',
    padding: 20,
  },
  authHeader: {
    gap: 6,
  },
  header: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: 14,
    justifyContent: 'space-between',
    marginBottom: 14,
  },
  headerIdentity: {
    flex: 1,
  },
  eyebrow: {
    color: '#5a635f',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0,
    textTransform: 'uppercase',
  },
  title: {
    color: '#15211d',
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: 0,
    marginTop: 3,
  },
  authTitle: {
    color: '#15211d',
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: 0,
  },
  subtitle: {
    color: '#4d5753',
    fontSize: 15,
    lineHeight: 21,
    marginTop: 4,
  },
  logoutButton: {
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderColor: '#cfc8bb',
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: 40,
    paddingHorizontal: 14,
  },
  logoutText: {
    color: '#2b3834',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
  },
  householdStrip: {
    backgroundColor: '#e9e3d8',
    borderRadius: 8,
    marginBottom: 12,
    minHeight: 42,
    padding: 10,
  },
  householdStripText: {
    color: '#59615e',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
  },
  statusBanner: {
    backgroundColor: '#eef3f8',
    borderColor: '#b8c8d6',
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 10,
    padding: 11,
  },
  statusBannerSuccess: {
    backgroundColor: '#eef8f2',
    borderColor: '#aacdb8',
  },
  statusBannerError: {
    backgroundColor: '#fff0ed',
    borderColor: '#ebb2a5',
  },
  statusBannerText: {
    color: '#33413c',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
    lineHeight: 18,
  },
  statusBannerTextError: {
    color: '#8a2f20',
  },
  householdChips: {
    gap: 8,
    paddingBottom: 12,
  },
  householdChip: {
    backgroundColor: '#e9e3d8',
    borderColor: '#d2c9ba',
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: 40,
    paddingHorizontal: 13,
  },
  householdChipActive: {
    backgroundColor: '#ffffff',
    borderColor: '#9dbba9',
  },
  householdChipText: {
    color: '#59615e',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
  },
  householdChipTextActive: {
    color: '#15211d',
  },
  modeSwitch: {
    backgroundColor: '#e9e3d8',
    borderRadius: 8,
    flexDirection: 'row',
    gap: 4,
    padding: 4,
  },
  modeButton: {
    alignItems: 'center',
    borderRadius: 7,
    flex: 1,
    justifyContent: 'center',
    minHeight: 42,
  },
  modeButtonActive: {
    backgroundColor: '#ffffff',
  },
  modeText: {
    color: '#59615e',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0,
  },
  modeTextActive: {
    color: '#15211d',
  },
  form: {
    gap: 14,
  },
  formPanel: {
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 14,
  },
  inputGroup: {
    gap: 7,
  },
  inputLabel: {
    color: '#24312d',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
  },
  input: {
    backgroundColor: '#ffffff',
    borderColor: '#cfc8bb',
    borderRadius: 8,
    borderWidth: 1,
    color: '#15211d',
    fontSize: 16,
    minHeight: 48,
    paddingHorizontal: 14,
  },
  errorBox: {
    backgroundColor: '#fff0ed',
    borderColor: '#ebb2a5',
    borderRadius: 8,
    borderWidth: 1,
    padding: 12,
  },
  errorText: {
    color: '#8a2f20',
    fontSize: 14,
    lineHeight: 20,
  },
  primaryButton: {
    alignItems: 'center',
    backgroundColor: '#1d7f68',
    borderRadius: 8,
    justifyContent: 'center',
    minHeight: 50,
    paddingHorizontal: 16,
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0,
  },
  buttonPressed: {
    opacity: 0.82,
  },
  buttonDisabled: {
    opacity: 0.58,
  },
  smallButton: {
    alignItems: 'center',
    backgroundColor: '#1d7f68',
    borderRadius: 8,
    justifyContent: 'center',
    minHeight: 34,
    paddingHorizontal: 11,
  },
  smallButtonText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0,
  },
  smallDangerButton: {
    alignItems: 'center',
    backgroundColor: '#fff0ed',
    borderColor: '#ebb2a5',
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: 34,
    paddingHorizontal: 10,
  },
  smallDangerText: {
    color: '#8a2f20',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0,
  },
  tabBar: {
    backgroundColor: '#e9e3d8',
    borderRadius: 8,
    flexDirection: 'row',
    gap: 4,
    padding: 4,
  },
  tab: {
    alignItems: 'center',
    borderRadius: 7,
    flex: 1,
    justifyContent: 'center',
    minHeight: 40,
    paddingHorizontal: 6,
  },
  tabActive: {
    backgroundColor: '#ffffff',
  },
  tabLabel: {
    color: '#59615e',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0,
  },
  tabLabelActive: {
    color: '#15211d',
  },
  content: {
    gap: 18,
    paddingBottom: 32,
    paddingTop: 18,
  },
  surfaceBand: {
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderLeftWidth: 5,
    borderRadius: 8,
    borderWidth: 1,
    padding: 18,
  },
  surfaceTitle: {
    color: '#15211d',
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: 0,
  },
  surfaceSummary: {
    color: '#4d5753',
    fontSize: 16,
    lineHeight: 23,
    marginTop: 8,
  },
  section: {
    gap: 12,
  },
  sectionTitle: {
    color: '#24312d',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  statTile: {
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    flexBasis: '47%',
    flexGrow: 1,
    padding: 14,
  },
  statValue: {
    color: '#15211d',
    fontSize: 24,
    fontWeight: '800',
    letterSpacing: 0,
  },
  statLabel: {
    color: '#56615d',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0,
    marginTop: 2,
  },
  profilePanel: {
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    gap: 5,
    padding: 16,
  },
  profileName: {
    color: '#15211d',
    fontSize: 20,
    fontWeight: '800',
    letterSpacing: 0,
  },
  profileMeta: {
    color: '#56615d',
    fontSize: 14,
    lineHeight: 20,
  },
  listBlock: {
    gap: 10,
  },
  shoppingListChips: {
    gap: 8,
  },
  listChip: {
    backgroundColor: '#f4f0e8',
    borderColor: '#d2c9ba',
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: 36,
    paddingHorizontal: 12,
  },
  listChipActive: {
    backgroundColor: '#ffffff',
    borderColor: '#946200',
  },
  listChipText: {
    color: '#59615e',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0,
  },
  listChipTextActive: {
    color: '#15211d',
  },
  listHeader: {
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    padding: 14,
  },
  entityRow: {
    alignItems: 'flex-start',
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 11,
    padding: 14,
  },
  entityRowHighlighted: {
    borderColor: '#c78b00',
    borderWidth: 2,
  },
  entityCopy: {
    flex: 1,
    gap: 3,
  },
  rowActions: {
    alignItems: 'flex-end',
    gap: 8,
  },
  entityTitle: {
    color: '#15211d',
    fontSize: 15,
    fontWeight: '800',
    letterSpacing: 0,
  },
  entityMeta: {
    color: '#59615e',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0,
  },
  hintText: {
    color: '#6c746f',
    fontSize: 13,
    lineHeight: 18,
  },
  dataPanel: {
    alignItems: 'flex-start',
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 11,
    padding: 14,
  },
  dataCopy: {
    flex: 1,
    gap: 4,
  },
  dataTitle: {
    color: '#15211d',
    fontSize: 15,
    fontWeight: '800',
    letterSpacing: 0,
  },
  dataBody: {
    color: '#56615d',
    fontSize: 14,
    lineHeight: 20,
  },
  statePanel: {
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderColor: '#ded8cd',
    borderRadius: 8,
    borderWidth: 1,
    gap: 12,
    padding: 18,
  },
  feedbackPanel: {
    borderRadius: 8,
    borderWidth: 1,
    padding: 12,
  },
  feedbackPanelSuccess: {
    backgroundColor: '#eef8f2',
    borderColor: '#aacdb8',
  },
  feedbackPanelError: {
    backgroundColor: '#fff0ed',
    borderColor: '#ebb2a5',
  },
  feedbackTextSuccess: {
    color: '#1d553d',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0,
  },
  feedbackTextError: {
    color: '#8a2f20',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0,
  },
  stateText: {
    color: '#33413c',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0,
  },
  retryButton: {
    alignItems: 'center',
    backgroundColor: '#1d7f68',
    borderRadius: 8,
    minHeight: 40,
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  retryText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0,
  },
  checkDot: {
    borderRadius: 5,
    height: 10,
    marginTop: 5,
    width: 10,
  },
});
