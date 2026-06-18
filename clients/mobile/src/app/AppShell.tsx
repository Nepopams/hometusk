import { StatusBar } from 'expo-status-bar';
import * as Linking from 'expo-linking';
import * as Notifications from 'expo-notifications';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  SafeAreaView,
  ScrollView,
  Text,
  View,
} from 'react-native';

import { createHomeTuskApiClient } from '../api/client';
import { generateClientUuid } from '../api/ids';
import type { CommandResponse, ShoppingItem, UserProfile, VoiceTranscriptionFile } from '../api/types';
import type { RecentCommandHint } from '../storage/localAppMemory';
import {
  targetFromDeepLinkUrl,
  targetFromNotificationResponse,
  type HomeTuskLinkTarget,
} from '../notifications/pushNotifications';
import type { SecureSession } from '../storage/secureSessionStore';
import { AuthScreen } from '../features/auth/AuthScreen';
import {
  isMobileSessionRestoreError,
  logoutMobileSession,
  openStoredMobileSession,
  submitMobileAuth,
} from '../features/auth/authSessionController';
import { buildCommandRequestFromText, parseContinuationInput } from '../features/command/commandRequestBuilder';
import { storeRecentCommandHint, readRecentCommandHints } from '../features/command/commandHistoryStore';
import { formatCommandOutcome } from '../features/command/commandOutcomeFormatting';
import { HouseholdSwitcher } from '../features/households/HouseholdSwitcher';
import { resolveSelectedHouseholdId, storeSelectedHouseholdId } from '../features/households/selectedHouseholdStore';
import { canOpenTargetHousehold, linkStatusForTarget } from '../features/notifications/notificationRouting';
import { registerCurrentDeviceForPush } from '../features/notifications/pushRegistrationController';
import { addShoppingItem, deleteShoppingItem, markShoppingItemPurchased } from '../features/shopping/shoppingMutations';
import { completeTaskFromMobileCommand, createTaskFromMobileCommand } from '../features/tasks/taskMutations';
import {
  formatAuthError,
  formatConfirmationError,
  formatLinkError,
  formatMutationError,
  formatReadError,
} from '../shared/errors/apiErrorFormatting';
import { StatusBanner } from '../shared/ui/StatusBanner';
import { styles } from '../shared/ui/styles';
import { emptyReadModels } from './readModels';
import { SurfacePanel } from './SurfacePanel';
import { surfaces } from './surfaces';
import type {
  AuthMode,
  AuthState,
  CommandConfirmationAction,
  CommandConfirmationActionResult,
  HouseholdReadModels,
  OpenedSession,
  ReadStatus,
  SavingAction,
  StatusBannerMessage,
  SurfaceKey,
} from './types';

const RESTORE_TRANSIENT_ERROR = 'Could not restore session. Check connection and retry.';

export function AppShell() {
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
  const [confirmationAction, setConfirmationAction] = useState<CommandConfirmationAction | null>(null);
  const [confirmationResult, setConfirmationResult] =
    useState<CommandConfirmationActionResult | null>(null);
  const [commandError, setCommandError] = useState<string | null>(null);
  const [commandSaving, setCommandSaving] = useState(false);
  const [voiceAsrTraceId, setVoiceAsrTraceId] = useState<string | null>(null);
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

      if (
        target.kind !== 'invite' &&
        target.householdId &&
        canOpenTargetHousehold(profile?.households, target)
      ) {
        setSelectedHouseholdId(target.householdId);
        await storeSelectedHouseholdId(target.householdId);
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
        setLinkStatus(linkStatusForTarget(target));
        setReadReloadKey((value) => value + 1);
        return;
      }

      if (target.kind === 'command') {
        setActiveSurface('command');
        setHighlightedTaskId(null);
        setLinkStatus(linkStatusForTarget(target));
        setReadReloadKey((value) => value + 1);
        return;
      }

      if (target.kind === 'invite') {
        setActiveSurface('home');
        setHighlightedTaskId(null);
        setLinkStatus(linkStatusForTarget(target));
        try {
          const client = createHomeTuskApiClient({ accessToken: session.accessToken });
          const accepted = await client.acceptInvite(target.inviteToken);
          const nextProfile = await client.getMe();
          setProfile(nextProfile);
          setSelectedHouseholdId(accepted.household.id);
          await storeSelectedHouseholdId(accepted.household.id);
          setLinkStatus({ tone: 'success', text: `Joined ${accepted.household.name}.` });
          setReadReloadKey((value) => value + 1);
        } catch (error) {
          setLinkStatus({ tone: 'error', text: formatLinkError(error) });
        }
        return;
      }

      setActiveSurface('home');
      setHighlightedTaskId(null);
      setLinkStatus(linkStatusForTarget(target));
      setReadReloadKey((value) => value + 1);
    },
    [authState, profile?.households, session?.accessToken]
  );

  useEffect(() => {
    let isMounted = true;

    async function bootstrapSession() {
      try {
        const opened = await openStoredMobileSession();
        if (!isMounted) {
          return;
        }

        if (!opened) {
          setAuthState('signedOut');
          return;
        }

        await applyOpenedSession(opened);
      } catch (error) {
        if (!isMounted) {
          return;
        }
        setSession(null);
        setProfile(null);
        setSelectedHouseholdId(null);
        setReadModels(emptyReadModels());
        setReadStatus('idle');
        setAuthState('signedOut');
        setAuthError(formatRestoreSessionError(error));
      }
    }

    bootstrapSession();
    return () => {
      isMounted = false;
    };
  }, [applyOpenedSession]);

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

      const status = await registerCurrentDeviceForPush(session.accessToken);
      if (isMounted) {
        setPushStatus(status);
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
    setCommandResponse(null);
    setCommandError(null);
    setContinuationText('');
    setConfirmationAction(null);
    setConfirmationResult(null);
    setVoiceAsrTraceId(null);

    let isMounted = true;

    async function loadRecentCommandHints() {
      if (!selectedHouseholdId) {
        setRecentCommands([]);
        return;
      }
      const stored = await readRecentCommandHints(selectedHouseholdId);
      if (!isMounted) {
        return;
      }
      setRecentCommands(stored);
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
      const opened = await submitMobileAuth({
        authMode,
        email: trimmedEmail,
        password,
        displayName: trimmedName,
      });
      await applyOpenedSession(opened);
      setPassword('');
    } catch (error) {
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
    await storeSelectedHouseholdId(householdId);
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
      const response = await createTaskFromMobileCommand({
        accessToken: session.accessToken,
        householdId: selectedHouseholdId,
        title: trimmedTitle,
      });
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
      const response = await completeTaskFromMobileCommand({
        accessToken: session.accessToken,
        householdId: selectedHouseholdId,
        taskId,
      });
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
      await addShoppingItem({
        accessToken: session.accessToken,
        householdId: selectedHouseholdId,
        listId: selectedShoppingListId,
        name: trimmedName,
      });
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
      await markShoppingItemPurchased({
        accessToken: session.accessToken,
        householdId: selectedHouseholdId,
        itemId,
      });
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
      await deleteShoppingItem({
        accessToken: session.accessToken,
        householdId: selectedHouseholdId,
        itemId,
      });
      setMutationMessage('Shopping item deleted.');
      setReadReloadKey((value) => value + 1);
    } catch (error) {
      setMutationError(formatMutationError(error));
    } finally {
      setSavingAction(null);
    }
  }

  function handleChangeCommandText(value: string) {
    setCommandText(value);
    if (!value.trim()) {
      setVoiceAsrTraceId(null);
    }
  }

  function handleVoiceTranscriptReady(transcript: string, traceId: string) {
    setCommandText(transcript);
    setVoiceAsrTraceId(traceId);
    setCommandError(null);
    setCommandResponse(null);
    setConfirmationAction(null);
    setConfirmationResult(null);
    setContinuationText('');
  }

  async function handleTranscribeVoiceRecording(file: VoiceTranscriptionFile) {
    if (!session?.accessToken) {
      throw new Error('Sign in again before using voice entry.');
    }

    return createHomeTuskApiClient({ accessToken: session.accessToken }).createVoiceTranscription(file);
  }

  async function handleSubmitCommand() {
    if (!session?.accessToken || !selectedHouseholdId) {
      setCommandError('Selected household is required.');
      return;
    }

    const submittedVoiceTraceId = voiceAsrTraceId;
    const parsed = buildCommandRequestFromText(
      commandText,
      selectedHouseholdId,
      submittedVoiceTraceId
        ? {
            inputMode: 'voice_transcript',
            source: 'voice',
            asrTraceId: submittedVoiceTraceId,
          }
        : {}
    );
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
      setConfirmationResult(null);
      setCommandText('');
      setVoiceAsrTraceId(null);
      await storeRecentCommandHint(
        selectedHouseholdId,
        submittedVoiceTraceId ? toVoiceRecentCommandText(parsed.displayText) : parsed.displayText,
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
      setConfirmationResult(null);
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

  async function handleApproveConfirmation() {
    if (
      !session?.accessToken ||
      commandResponse?.status !== 'needs_confirmation' ||
      !commandResponse.confirmation
    ) {
      setCommandError('A pending confirmation is required.');
      return;
    }
    if (confirmationAction || confirmationResult) {
      return;
    }

    setConfirmationAction('approve');
    setCommandError(null);

    try {
      const response = await createHomeTuskApiClient({
        accessToken: session.accessToken,
      }).approveCommandConfirmation(
        commandResponse.commandId,
        commandResponse.confirmation.confirmationId
      );
      setConfirmationResult({ type: 'approve', response });
      if (response.status === 'executed') {
        setReadReloadKey((value) => value + 1);
      }
    } catch (error) {
      setCommandError(formatConfirmationError(error));
    } finally {
      setConfirmationAction(null);
    }
  }

  async function handleCancelConfirmation() {
    if (
      !session?.accessToken ||
      commandResponse?.status !== 'needs_confirmation' ||
      !commandResponse.confirmation
    ) {
      setCommandError('A pending confirmation is required.');
      return;
    }
    if (confirmationAction || confirmationResult) {
      return;
    }

    setConfirmationAction('cancel');
    setCommandError(null);

    try {
      const response = await createHomeTuskApiClient({
        accessToken: session.accessToken,
      }).cancelCommandConfirmation(commandResponse.commandId, commandResponse.confirmation.confirmationId, {
        reason: 'cancelled_from_mobile',
      });
      setConfirmationResult({ type: 'cancel', response });
    } catch (error) {
      setCommandError(formatConfirmationError(error));
    } finally {
      setConfirmationAction(null);
    }
  }

  async function handleLogout() {
    setIsSubmitting(true);
    setAuthError(null);

    try {
      await logoutMobileSession(session);
    } finally {
      setSession(null);
      setProfile(null);
      setSelectedHouseholdId(null);
      setReadModels(emptyReadModels());
      setReadStatus('idle');
      setTaskTitle('');
      setShoppingItemName('');
      setCommandText('');
      setVoiceAsrTraceId(null);
      setContinuationText('');
      setCommandResponse(null);
      setConfirmationAction(null);
      setConfirmationResult(null);
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
              confirmationAction,
              confirmationResult,
              continuationText,
              error: commandError,
              isSaving: commandSaving,
              recentCommands,
              response: commandResponse,
              onChangeCommandText: handleChangeCommandText,
              onChangeContinuationText: setContinuationText,
              onApproveConfirmation: handleApproveConfirmation,
              onCancelConfirmation: handleCancelConfirmation,
              onContinueCommand: handleContinueCommand,
              onSubmitCommand: handleSubmitCommand,
              voice: {
                asrTraceId: voiceAsrTraceId,
                onTranscriptReady: handleVoiceTranscriptReady,
                onTranscribeRecording: handleTranscribeVoiceRecording,
              },
            }}
            highlightedTaskId={highlightedTaskId}
            onNavigate={setActiveSurface}
            onRetry={() => setReadReloadKey((value) => value + 1)}
            profile={profile}
            readError={readError}
            readStatus={readStatus}
            selectedHousehold={selectedHousehold}
            surfaceKey={surface.key}
          />
        </ScrollView>

        <View style={styles.bottomNavigation}>
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
        </View>
      </View>
    </SafeAreaView>
  );
}

function formatRestoreSessionError(error: unknown): string {
  if (!isMobileSessionRestoreError(error)) {
    return RESTORE_TRANSIENT_ERROR;
  }

  if (
    error.reason === 'refresh_transient_failure' ||
    error.reason === 'profile_transient_failure'
  ) {
    return RESTORE_TRANSIENT_ERROR;
  }

  return 'Session expired. Sign in again.';
}

function toVoiceRecentCommandText(displayText: string): string {
  return `Voice: ${displayText.replace(/^Command:\s*/, '')}`;
}
