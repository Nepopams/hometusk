import { Pressable, Text, View, type ColorValue } from 'react-native';

import type { HouseholdSummary, UserProfile } from '../../api/types';
import type { CommandChatControls, HouseholdReadModels, SurfaceKey } from '../../app/types';
import { formatShortDate } from '../../shared/format/dates';
import { formatNotificationType, formatTaskStatus } from '../../shared/format/labels';
import { EmptyState } from '../../shared/ui/EmptyState';
import { Mascot } from '../../shared/ui/Mascot';
import { SectionList } from '../../shared/ui/SectionList';
import { styles } from '../../shared/ui/styles';
import { formatRecentCommandStatus } from '../command/commandOutcomeFormatting';
import { HomeSummaryCard } from './HomeSummaryCard';
import { QuickActionRow } from './QuickActionRow';

export function HomeSurface({
  profile,
  selectedHousehold,
  models,
  accent,
  commandControls,
  onNavigate,
}: {
  profile: UserProfile;
  selectedHousehold: HouseholdSummary;
  models: HouseholdReadModels;
  accent: ColorValue;
  commandControls: CommandChatControls;
  onNavigate: (surface: SurfaceKey) => void;
}) {
  const openTasks = models.tasks.filter((task) => task.status !== 'done');
  const openTaskCount = openTasks.length;
  const unpurchasedItems = models.shoppingItems.filter((item) => !item.purchased);
  const unpurchasedCount = models.shoppingLists.reduce(
    (total, list) => total + list.unpurchasedCount,
    0
  );
  const pendingConfirmation =
    commandControls.response?.status === 'needs_confirmation' && !commandControls.confirmationResult
      ? commandControls.response.confirmation
      : null;
  const latestCommand = commandControls.recentCommands[0] ?? null;
  const latestNotification = models.notifications[0] ?? null;
  const emptyHousehold =
    openTaskCount === 0 &&
    unpurchasedCount === 0 &&
    models.zones.length === 0 &&
    models.shoppingLists.length === 0 &&
    !latestCommand;

  return (
    <View style={styles.section}>
      <View style={styles.homeWelcomeCard}>
        <View style={styles.homeWelcomeCopy}>
          <Text style={styles.homeWelcomeTitle}>Дом сегодня</Text>
          <Text style={styles.homeWelcomeBody}>
            {profile.displayName}, посмотрим, что важно в {selectedHousehold.name}.
          </Text>
        </View>
        <Mascot mood="hello" size="medium" />
      </View>

      <HomeSummaryCard
        openTaskCount={openTaskCount}
        unpurchasedCount={unpurchasedCount}
        zoneCount={models.zones.length}
      />

      {pendingConfirmation ? (
        <View style={styles.pendingSummaryCard}>
          <View style={styles.pendingSummaryHeader}>
            <View style={styles.homeWelcomeCopy}>
              <Text style={styles.homeWelcomeTitle}>Ждет подтверждения</Text>
              <Text style={styles.homeWelcomeBody}>Пока ничего не изменилось.</Text>
            </View>
            <Mascot mood="confirm" size="small" />
          </View>
          <Text style={styles.dataBody}>{pendingConfirmation.summary}</Text>
          <Pressable
            accessibilityRole="button"
            onPress={() => onNavigate('command')}
            style={({ pressed }) => [styles.primaryButton, pressed && styles.buttonPressed]}
          >
            <Text style={styles.primaryButtonText}>Открыть подтверждение</Text>
          </Pressable>
        </View>
      ) : (
        <View style={styles.latestChangeCard}>
          <Text style={styles.entityTitle}>Подтверждений пока нет</Text>
          <Text style={styles.entityMeta}>Если команда потребует решения, она появится здесь в этой сессии.</Text>
        </View>
      )}

      {emptyHousehold ? (
        <EmptyState
          action={{ label: 'Открыть команды', onPress: () => onNavigate('command') }}
          body="Начни с первой команды, задачи или списка покупок."
          mascotMood="idle"
          title="Пока тихо"
        />
      ) : (
        <SectionList title="Сейчас важно">
          {openTasks.slice(0, 2).map((task) => (
            <View key={task.id} style={styles.homePriorityRow}>
              <View style={[styles.checkDot, { backgroundColor: accent }]} />
              <View style={styles.entityCopy}>
                <Text style={styles.entityTitle}>{task.title}</Text>
                <Text style={styles.entityMeta}>
                  {formatTaskStatus(task.status)}
                  {task.assignee ? ` - ${task.assignee.displayName}` : ''}
                  {task.zone ? ` - ${task.zone.name}` : ''}
                </Text>
              </View>
            </View>
          ))}
          {unpurchasedItems.slice(0, 2).map((item) => (
            <View key={item.id} style={styles.homePriorityRow}>
              <View style={[styles.checkDot, { backgroundColor: '#6B8E5E' }]} />
              <View style={styles.entityCopy}>
                <Text style={styles.entityTitle}>{item.name}</Text>
                <Text style={styles.entityMeta}>
                  {item.quantity ? `Количество ${item.quantity}` : 'Купить'}
                </Text>
              </View>
            </View>
          ))}
        </SectionList>
      )}

      <View style={styles.latestChangeCard}>
        <Text style={styles.entityTitle}>
          {latestCommand ? 'Последняя команда' : latestNotification ? 'Последнее изменение' : 'Последнее изменение'}
        </Text>
        <Text style={styles.dataBody}>
          {latestCommand
            ? `${formatRecentCommandText(latestCommand.text)} - ${formatRecentCommandStatus(latestCommand.status)}`
            : latestNotification
              ? latestNotification.payload.summary ?? formatNotificationType(latestNotification.type)
              : 'Пока ничего нового.'}
        </Text>
        {latestCommand && <Text style={styles.entityMeta}>{formatShortDate(latestCommand.createdAt)}</Text>}
      </View>

      <QuickActionRow onNavigate={onNavigate} />
    </View>
  );
}

function formatRecentCommandText(text: string): string {
  if (text.startsWith('Command: ')) {
    return text.replace(/^Command:\s*/, '');
  }
  if (text.startsWith('Continue: ')) {
    return `Уточнение: ${text.replace(/^Continue:\s*/, '')}`;
  }
  return text;
}
