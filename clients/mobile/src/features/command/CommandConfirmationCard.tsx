import { Pressable, Text, View, type ColorValue } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import type { CommandConfirmationProposedAction, CommandResponse } from '../../api/types';
import { formatShortDate } from '../../shared/format/dates';
import { shortId } from '../../shared/format/ids';
import { styles } from '../../shared/ui/styles';

type CommandConfirmationCardProps = {
  accent: ColorValue;
  controls: CommandChatControls;
  response: CommandResponse;
};

export function CommandConfirmationCard({
  accent,
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
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.dataCopy}>
        <Text style={styles.dataTitle}>Confirmation required.</Text>
        <Text style={styles.dataBody}>
          No action has happened yet. Approve explicitly to let HomeTusk revalidate and execute through
          the backend.
        </Text>
        <Text style={styles.dataBody}>{confirmation.summary}</Text>

        {confirmation.reasons.length > 0 && (
          <View style={styles.confirmationBlock}>
            <Text style={styles.entityMeta}>Reasons</Text>
            {confirmation.reasons.map((reason) => (
              <Text key={reason} style={styles.dataBody}>
                - {reason}
              </Text>
            ))}
          </View>
        )}

        {confirmation.riskLabels.length > 0 && (
          <View style={styles.confirmationBlock}>
            <Text style={styles.entityMeta}>Risk labels</Text>
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
          <Text style={styles.entityMeta}>Proposed actions</Text>
          {confirmation.proposedActions.length === 0 ? (
            <Text style={styles.dataBody}>No displayable proposed actions were returned.</Text>
          ) : (
            confirmation.proposedActions.map((action, index) => (
              <Text key={`${action.type}-${index}`} style={styles.dataBody}>
                - {formatProposedAction(action)}
              </Text>
            ))
          )}
        </View>

        <Text style={styles.entityMeta}>Expires {formatShortDate(confirmation.expiresAt)}</Text>
        <Text style={styles.entityMeta}>
          Command {shortId(response.commandId)} / Confirmation {shortId(confirmation.confirmationId)}
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
              {controls.confirmationAction === 'approve' ? 'Approving...' : 'Approve'}
            </Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            disabled={actionDisabled}
            onPress={controls.onCancelConfirmation}
            style={({ pressed }) => [
              styles.smallDangerButton,
              styles.confirmationButton,
              pressed && styles.buttonPressed,
              actionDisabled && styles.buttonDisabled,
            ]}
          >
            <Text style={styles.smallDangerText}>
              {controls.confirmationAction === 'cancel' ? 'Cancelling...' : 'Cancel'}
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
    return title ? `Create task "${title}"` : 'Create a task';
  }
  if (action.type === 'complete_task') {
    return taskId ? `Complete task ${shortId(taskId)}` : 'Complete a task';
  }
  if (action.type === 'add_shopping_item') {
    return name ? `Add shopping item "${name}"` : 'Add a shopping item';
  }
  return 'Review proposed action';
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
      ? 'Cancellation already recorded. No domain action was executed.'
      : 'Confirmation cancelled. No domain action was executed.';
  }

  if (result.response.status === 'executed') {
    return result.response.idempotentReplay
      ? 'Approval was already recorded. HomeTusk returned the stored terminal result.'
      : 'Approved and executed by HomeTusk.';
  }

  return result.response.reason ?? result.response.errorCode ?? 'Approval ended without execution.';
}
