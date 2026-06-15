import { Pressable, Text, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';

export function CommandContinuationCard({ controls }: { controls: CommandChatControls }) {
  if (controls.response?.status !== 'needs_input') {
    return null;
  }

  return (
    <View style={styles.formPanel}>
      <LabeledInput
        editable={!controls.isSaving}
        label="Additional input"
        onChangeText={controls.onChangeContinuationText}
        placeholder="assigneeId=..."
        value={controls.continuationText}
      />
      <Text style={styles.hintText}>Use key=value pairs when a specific field is requested.</Text>
      <Pressable
        accessibilityRole="button"
        disabled={controls.isSaving}
        onPress={controls.onContinueCommand}
        style={({ pressed }) => [
          styles.primaryButton,
          pressed && styles.buttonPressed,
          controls.isSaving && styles.buttonDisabled,
        ]}
      >
        <Text style={styles.primaryButtonText}>Continue</Text>
      </Pressable>
    </View>
  );
}
