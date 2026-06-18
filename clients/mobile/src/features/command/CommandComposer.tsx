import { Pressable, Text, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';

export function CommandComposer({ controls }: { controls: CommandChatControls }) {
  return (
    <View style={styles.commandComposer}>
      <LabeledInput
        editable={!controls.isSaving}
        label="Команда"
        onChangeText={controls.onChangeCommandText}
        placeholder="Назначь Пете вынести мусор сегодня вечером"
        value={controls.commandText}
      />
      <View style={styles.commandExamples}>
        <View style={styles.exampleChip}>
          <Text style={styles.exampleChipText}>кухня сегодня</Text>
        </View>
        <View style={styles.exampleChip}>
          <Text style={styles.exampleChipText}>покупки</Text>
        </View>
        <View style={styles.exampleChip}>
          <Text style={styles.exampleChipText}>назначить</Text>
        </View>
      </View>
      <Text style={styles.hintText}>HomeTusk покажет результат или спросит подтверждение.</Text>
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
        <Text style={styles.primaryButtonText}>{controls.isSaving ? 'Отправляю...' : 'Отправить команду'}</Text>
      </Pressable>
    </View>
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
