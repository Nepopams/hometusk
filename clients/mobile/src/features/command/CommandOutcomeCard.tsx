import { Text, View, type StyleProp, type ViewStyle } from 'react-native';

import { shortId } from '../../shared/format/ids';
import { styles } from '../../shared/ui/styles';
import type { CommandOutcomeCardProps } from './commandTypes';
import type { CommandTone } from './commandOutcomeFormatting';
import {
  formatCommandOutcome,
  getCommandOutcomeBody,
  getCommandTone,
} from './commandOutcomeFormatting';

export function CommandOutcomeCard({ response }: CommandOutcomeCardProps) {
  const title = formatCommandOutcome(response.status);
  const body = getCommandOutcomeBody(response);
  const tone = getCommandTone(response.status);

  return (
    <View style={[styles.dataPanel, dataPanelStyle[tone]]}>
      <View style={[styles.checkDot, { backgroundColor: dotColor[tone] }]} />
      <View style={styles.dataCopy}>
        <View style={[styles.statePill, statePillStyle[tone]]}>
          <Text style={styles.statePillText}>{title.replace(/\.$/, '')}</Text>
        </View>
        <Text style={styles.dataTitle}>{getOutcomeTitle(response.status)}</Text>
        <Text style={styles.dataBody}>{body}</Text>
        {response.status === 'needs_input' && response.requiredFields?.length ? (
          <View style={styles.confirmationChipRow}>
            {response.requiredFields.map((field) => (
              <Text key={field} style={styles.entityChip}>
                {field}
              </Text>
            ))}
          </View>
        ) : null}
        <Text style={styles.entityMeta}>Команда {shortId(response.commandId)}</Text>
      </View>
    </View>
  );
}

function getOutcomeTitle(status: string): string {
  if (status === 'executed') {
    return 'Изменение применено';
  }
  if (status === 'scheduled') {
    return 'Действие поставлено в расписание';
  }
  if (status === 'needs_input') {
    return 'Ответь на уточнение';
  }
  if (status === 'rejected') {
    return 'Действие не применено';
  }
  if (status === 'executed_degraded') {
    return 'Сделано только безопасное';
  }
  return 'Результат команды';
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

const dataPanelStyle: Record<CommandTone, StyleProp<ViewStyle>> = {
  idle: undefined,
  thinking: styles.dataPanelConfirm,
  success: styles.dataPanelSuccess,
  clarify: styles.dataPanelClarify,
  confirm: styles.dataPanelConfirm,
  reject: styles.dataPanelReject,
  degraded: styles.dataPanelDegraded,
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
