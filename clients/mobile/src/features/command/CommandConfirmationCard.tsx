import { Pressable, Text, View, type ColorValue } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import type { CommandConfirmationProposedAction, CommandResponse } from '../../api/types';
import { formatShortDate } from '../../shared/format/dates';
import { shortId } from '../../shared/format/ids';
import { Mascot } from '../../shared/ui/Mascot';
import { styles } from '../../shared/ui/styles';

type CommandConfirmationCardProps = {
  accent: ColorValue;
  controls: CommandChatControls;
  response: CommandResponse;
};

export function CommandConfirmationCard({
  controls,
  response,
}: CommandConfirmationCardProps) {
  const confirmation = response.confirmation;

  if (response.status !== 'needs_confirmation' || !confirmation) {
    return null;
  }

  const actionDisabled = Boolean(controls.confirmationAction || controls.confirmationResult);

  return (
    <View style={styles.confirmationPanel}>
      <Mascot mood="confirm" size="small" />
      <View style={styles.dataCopy}>
        <View style={[styles.statePill, styles.statePillConfirm]}>
          <Text style={styles.statePillText}>Нужно подтверждение</Text>
        </View>
        <Text style={styles.dataTitle}>Пока ничего не изменилось</Text>
        <Text style={styles.dataBody}>
          Проверь, что будет сделано. HomeTusk выполнит действие только после явного подтверждения.
        </Text>
        <Text style={styles.dataBody}>{confirmation.summary}</Text>

        {confirmation.reasons.length > 0 && (
          <View style={styles.confirmationBlock}>
            <Text style={styles.entityMeta}>Почему спрашиваем</Text>
            {confirmation.reasons.map((reason) => (
              <Text key={reason} style={styles.dataBody}>
                {reason}
              </Text>
            ))}
          </View>
        )}

        {confirmation.riskLabels.length > 0 && (
          <View style={styles.confirmationBlock}>
            <Text style={styles.entityMeta}>На что обратить внимание</Text>
            <View style={styles.confirmationChipRow}>
              {confirmation.riskLabels.map((label) => (
                <Text key={label} style={styles.confirmationChip}>
                  {label}
                </Text>
              ))}
            </View>
          </View>
        )}

        <View style={styles.confirmationBlock}>
          <Text style={styles.entityMeta}>После подтверждения</Text>
          {confirmation.proposedActions.length === 0 ? (
            <Text style={styles.dataBody}>Нет действий для показа.</Text>
          ) : (
            confirmation.proposedActions.map((action, index) => (
              <Text key={`${action.type}-${index}`} style={styles.dataBody}>
                {formatProposedAction(action)}
              </Text>
            ))
          )}
        </View>

        <Text style={styles.entityMeta}>Действует до {formatShortDate(confirmation.expiresAt)}</Text>
        <Text style={styles.entityMeta}>
          Команда {shortId(response.commandId)} / подтверждение {shortId(confirmation.confirmationId)}
        </Text>

        {controls.confirmationResult && (
          <View style={[styles.feedbackPanel, styles.feedbackPanelSuccess]}>
            <Text style={styles.feedbackTextSuccess}>
              {formatConfirmationResult(controls.confirmationResult)}
            </Text>
          </View>
        )}

        <View style={styles.confirmationActions}>
          <Pressable
            accessibilityRole="button"
            disabled={actionDisabled}
            onPress={controls.onApproveConfirmation}
            style={({ pressed }) => [
              styles.primaryButton,
              styles.confirmationButton,
              pressed && styles.buttonPressed,
              actionDisabled && styles.buttonDisabled,
            ]}
          >
            <Text style={styles.primaryButtonText}>
              {controls.confirmationAction === 'approve' ? 'Подтверждаю...' : 'Подтвердить'}
            </Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            disabled={actionDisabled}
            onPress={controls.onCancelConfirmation}
            style={({ pressed }) => [
              styles.secondaryButton,
              styles.confirmationButton,
              pressed && styles.buttonPressed,
              actionDisabled && styles.buttonDisabled,
            ]}
          >
            <Text style={styles.secondaryButtonText}>
              {controls.confirmationAction === 'cancel' ? 'Отменяю...' : 'Отменить'}
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}

function formatProposedAction(action: CommandConfirmationProposedAction): string {
  const title = readString(action.parameters.title);
  const name = readString(action.parameters.name);
  const taskId = readString(action.parameters.taskId);

  if (action.type === 'create_task') {
    return title ? `Создать задачу "${title}"` : 'Создать задачу';
  }
  if (action.type === 'complete_task') {
    return taskId ? `Закрыть задачу ${shortId(taskId)}` : 'Закрыть задачу';
  }
  if (action.type === 'add_shopping_item') {
    return name ? `Добавить покупку "${name}"` : 'Добавить покупку';
  }
  return 'Проверить действие';
}

function readString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function formatConfirmationResult(result: CommandChatControls['confirmationResult']): string {
  if (!result) {
    return '';
  }
  if (result.type === 'cancel') {
    return result.response.idempotentReplay
      ? 'Отмена уже была записана. Действий по дому не было.'
      : 'Подтверждение отменено. Действий по дому не было.';
  }

  if (result.response.status === 'executed') {
    return result.response.idempotentReplay
      ? 'Подтверждение уже было записано. HomeTusk вернул сохраненный результат.'
      : 'Подтверждено и выполнено через HomeTusk.';
  }

  return result.response.reason ?? result.response.errorCode ?? 'Подтверждение завершилось без действия.';
}
