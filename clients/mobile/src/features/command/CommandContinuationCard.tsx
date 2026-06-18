import { Pressable, Text, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';

export function CommandContinuationCard({ controls }: { controls: CommandChatControls }) {
  if (controls.response?.status !== 'needs_input') {
    return null;
  }

  return (
    <View style={[styles.formPanel, styles.dataPanelClarify]}>
      <LabeledInput
        editable={!controls.isSaving}
        label="Ответ на уточнение"
        onChangeText={controls.onChangeContinuationText}
        placeholder="Например: Петя или кухня"
        value={controls.continuationText}
      />
      <Text style={styles.hintText}>Ответ уйдет как уточнение к той же команде.</Text>
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
        <Text style={styles.primaryButtonText}>{controls.isSaving ? 'Отправляю...' : 'Ответить'}</Text>
      </Pressable>
    </View>
  );
}
