import { Text, View } from 'react-native';

import { EmptyState } from '../../shared/ui/EmptyState';
import { SectionList } from '../../shared/ui/SectionList';
import { styles } from '../../shared/ui/styles';
import { CommandComposer, CommandError } from './CommandComposer';
import { CommandConfirmationCard } from './CommandConfirmationCard';
import { CommandContinuationCard } from './CommandContinuationCard';
import { CommandHeroCard } from './CommandHeroCard';
import { CommandOutcomeCard } from './CommandOutcomeCard';
import { RecentCommandRow } from './RecentCommandRow';
import type { CommandSurfaceProps } from './commandTypes';

export function CommandSurface({
  selectedHousehold,
  models,
  accent,
  controls,
}: CommandSurfaceProps) {
  return (
    <View style={styles.section}>
      <CommandHeroCard isSaving={controls.isSaving} status={controls.response?.status} />

      <View style={styles.softCard}>
        <Text style={styles.sectionTitle}>{selectedHousehold.name}</Text>
        <Text style={styles.hintText}>
          Загружено: {models.members.length} участников, {models.zones.length} зон, {models.tasks.length} задач.
        </Text>
      </View>

      <CommandComposer controls={controls} />
      <CommandError message={controls.error} />

      {controls.response?.status === 'needs_confirmation' ? (
        <CommandConfirmationCard accent={accent} controls={controls} response={controls.response} />
      ) : (
        controls.response && <CommandOutcomeCard accent={accent} response={controls.response} />
      )}

      <CommandContinuationCard controls={controls} />

      <SectionList title="Недавние команды">
        {controls.recentCommands.length === 0 ? (
          <EmptyState
            body="После первой команды здесь появится короткая история для этого дома."
            mascotMood="idle"
            title="Команд пока нет"
          />
        ) : (
          controls.recentCommands
            .slice(0, 8)
            .map((entry) => <RecentCommandRow entry={entry} key={`${entry.id}-${entry.createdAt}`} />)
        )}
      </SectionList>
    </View>
  );
}
