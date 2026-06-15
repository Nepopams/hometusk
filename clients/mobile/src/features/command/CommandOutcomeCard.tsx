import { Text, View } from 'react-native';

import { shortId } from '../../shared/format/ids';
import { styles } from '../../shared/ui/styles';
import type { CommandOutcomeCardProps } from './commandTypes';
import { formatCommandOutcome, getCommandOutcomeBody } from './commandOutcomeFormatting';

export function CommandOutcomeCard({ response, accent }: CommandOutcomeCardProps) {
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
