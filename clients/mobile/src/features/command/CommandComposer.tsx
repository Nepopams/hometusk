import { Pressable, Text, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';

export function CommandComposer({ controls }: { controls: CommandChatControls }) {
  return (
    <>
      <LabeledInput
        editable={!controls.isSaving}
        label="Command"
        onChangeText={controls.onChangeCommandText}
        placeholder="Buy milk and move laundry tonight"
        value={controls.commandText}
      />
      <Text style={styles.hintText}>
        Send natural household text. HomeTusk will execute, clarify, reject, or ask for confirmation.
      </Text>
      <Pressable
        accessibilityRole="button"
        disabled={controls.isSaving}
        onPress={controls.onSubmitCommand}
        style={({ pressed }) => [
          styles.primaryButton,
          pressed && styles.buttonPressed,
          controls.isSaving && styles.buttonDisabled,
        ]}
      >
        <Text style={styles.primaryButtonText}>{controls.isSaving ? 'Sending...' : 'Send command'}</Text>
      </Pressable>
    </>
  );
}

export function CommandError({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }

  return (
    <View style={[styles.feedbackPanel, styles.feedbackPanelError]}>
      <Text style={styles.feedbackTextError}>{message}</Text>
    </View>
  );
}
