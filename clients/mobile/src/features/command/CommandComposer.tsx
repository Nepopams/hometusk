import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';
import { VoiceRecordingSheet } from './VoiceRecordingSheet';

export function CommandComposer({ controls }: { controls: CommandChatControls }) {
  const [isVoiceSheetVisible, setIsVoiceSheetVisible] = useState(false);

  return (
    <View style={styles.commandComposer}>
      <LabeledInput
        editable={!controls.isSaving}
        label="Команда"
        multiline
        numberOfLines={3}
        onChangeText={controls.onChangeCommandText}
        placeholder="Назначь Пете вынести мусор сегодня вечером"
        value={controls.commandText}
      />
      {controls.voice.asrTraceId ? (
        <View style={styles.voiceDraftBanner}>
          <Text style={styles.voiceDraftText}>
            Черновик из голоса готов. Проверь текст и отправь команду вручную.
          </Text>
        </View>
      ) : null}
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
      <Text style={styles.hintText}>
        HomeTusk покажет результат или спросит подтверждение.
      </Text>
      <View style={styles.commandComposerActions}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Записать голосовую команду"
          disabled={controls.isSaving}
          onPress={() => setIsVoiceSheetVisible(true)}
          style={({ pressed }) => [
            styles.voiceMicButton,
            pressed && styles.buttonPressed,
            controls.isSaving && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.voiceMicButtonText}>Голос</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          disabled={controls.isSaving}
          onPress={controls.onSubmitCommand}
          style={({ pressed }) => [
            styles.primaryButton,
            styles.commandSubmitButton,
            pressed && styles.buttonPressed,
            controls.isSaving && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>
            {controls.isSaving ? 'Отправляю...' : 'Отправить команду'}
          </Text>
        </Pressable>
      </View>
      <VoiceRecordingSheet
        controls={controls.voice}
        disabled={controls.isSaving}
        onClose={() => setIsVoiceSheetVisible(false)}
        visible={isVoiceSheetVisible}
      />
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
