import type { ColorValue } from 'react-native';

import type {
  CommandResponse,
  HouseholdMember,
  HouseholdNotification,
  HouseholdSummary,
  ShoppingItem,
  ShoppingList,
  Task,
  UserProfile,
  Zone,
} from '../api/types';
import type { RecentCommandHint } from '../storage/localAppMemory';
import type { SecureSession } from '../storage/secureSessionStore';

export type SurfaceKey = 'home' | 'tasks' | 'shopping' | 'command';
export type AuthMode = 'login' | 'register';
export type AuthState = 'checking' | 'signedOut' | 'signedIn';
export type ReadStatus = 'idle' | 'loading' | 'ready' | 'error';
export type SavingAction = string | null;
export type BannerTone = 'success' | 'info' | 'error';

export type StatusBannerMessage = {
  tone: BannerTone;
  text: string;
};

export type Surface = {
  key: SurfaceKey;
  label: string;
  title: string;
  summary: string;
  accent: ColorValue;
};

export type OpenedSession = {
  session: SecureSession;
  profile: UserProfile;
};

export type HouseholdReadModels = {
  members: HouseholdMember[];
  zones: Zone[];
  tasks: Task[];
  shoppingLists: ShoppingList[];
  shoppingItems: ShoppingItem[];
  notifications: HouseholdNotification[];
};

export type MutationControls = {
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

export type CommandChatControls = {
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

export type SurfacePanelProps = {
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
};
