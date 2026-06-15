import { Text, View } from 'react-native';

import { formatShortDate } from '../../shared/format/dates';
import { EmptyRow, InfoRow } from '../../shared/ui/InfoRow';
import { SectionList } from '../../shared/ui/SectionList';
import { styles } from '../../shared/ui/styles';
import { CommandComposer, CommandError } from './CommandComposer';
import { CommandContinuationCard } from './CommandContinuationCard';
import { CommandOutcomeCard } from './CommandOutcomeCard';
import type { CommandSurfaceProps } from './commandTypes';

export function CommandSurface({
  selectedHousehold,
  models,
  accent,
  controls,
}: CommandSurfaceProps) {
  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <Text style={styles.sectionTitle}>{selectedHousehold.name}</Text>
        <Text style={styles.entityMeta}>
          {models.members.length} members, {models.zones.length} zones, {models.tasks.length} tasks loaded
        </Text>
        <CommandComposer controls={controls} />
      </View>

      <CommandError message={controls.error} />

      {controls.response && <CommandOutcomeCard accent={accent} response={controls.response} />}

      <CommandContinuationCard controls={controls} />

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
