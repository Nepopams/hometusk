import { Text, View, type StyleProp, type ViewStyle } from 'react-native';

import type { RecentCommandHint } from '../../storage/localAppMemory';
import { formatShortDate } from '../../shared/format/dates';
import { styles } from '../../shared/ui/styles';
import type { CommandTone } from './commandOutcomeFormatting';
import { formatRecentCommandStatus, getCommandTone } from './commandOutcomeFormatting';

export function RecentCommandRow({ entry }: { entry: RecentCommandHint }) {
  const tone = getCommandTone(entry.status);

  return (
    <View style={styles.recentCommandRow}>
      <View style={[styles.checkDot, { backgroundColor: dotColor[tone] }]} />
      <View style={styles.entityCopy}>
        <Text style={styles.entityTitle}>{formatRecentCommandText(entry.text)}</Text>
        <Text style={styles.entityMeta}>{formatShortDate(entry.createdAt)}</Text>
      </View>
      <View style={[styles.statePill, statePillStyle[tone]]}>
        <Text style={styles.statePillText}>{formatRecentCommandStatus(entry.status)}</Text>
      </View>
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

const dotColor: Record<CommandTone, string> = {
  idle: '#EAD7C3',
  thinking: '#E8A055',
  success: '#6B8E5E',
  clarify: '#5B8FC4',
  confirm: '#E8A055',
  reject: '#B75B4A',
  degraded: '#9C8262',
};

const statePillStyle: Record<CommandTone, StyleProp<ViewStyle>> = {
  idle: styles.statePillIdle,
  thinking: styles.statePillConfirm,
  success: styles.statePillSuccess,
  clarify: styles.statePillClarify,
  confirm: styles.statePillConfirm,
  reject: styles.statePillReject,
  degraded: styles.statePillDegraded,
};
