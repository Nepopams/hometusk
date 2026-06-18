import { useState } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';

import type { CommandChatControls } from '../../app/types';
import { styles } from '../../shared/ui/styles';
import { VoiceRecordingSheet } from './VoiceRecordingSheet';

export function CommandComposer({ controls }: { controls: CommandChatControls }) {
  const [isVoiceSheetVisible, setIsVoiceSheetVisible] = useState(false);

  return (
    <View style={styles.commandComposer}>
      <View style={styles.commandInputGroup}>
        <Text style={styles.inputLabel}>Команда</Text>
        <View style={styles.commandInputRow}>
          <TextInput
            autoCapitalize="sentences"
            editable={!controls.isSaving}
            onChangeText={controls.onChangeCommandText}
            placeholder="Например: купи молоко и хлеб"
            placeholderTextColor="#8a908d"
            style={styles.commandTextInput}
            value={controls.commandText}
          />
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
            <View style={styles.voiceMicIcon}>
              <View style={styles.voiceMicIconCapsule} />
              <View style={styles.voiceMicIconStem} />
              <View style={styles.voiceMicIconBase} />
            </View>
          </Pressable>
        </View>
      </View>
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
