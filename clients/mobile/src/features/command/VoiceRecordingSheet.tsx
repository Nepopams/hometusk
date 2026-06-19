import {
  RecordingPresets,
  type RecordingOptions,
  requestRecordingPermissionsAsync,
  useAudioRecorder,
  useAudioRecorderState,
} from 'expo-audio';
import { useEffect, useRef, useState } from 'react';
import { Modal, Platform, Pressable, Text, View } from 'react-native';

import { HomeTuskApiError } from '../../api/client';
import type { VoiceTranscriptionFile } from '../../api/types';
import type { CommandVoiceControls } from '../../app/types';
import { formatVoiceTranscriptionError } from '../../shared/errors/apiErrorFormatting';
import { styles } from '../../shared/ui/styles';

type VoiceRecordingPhase =
  | 'ready'
  | 'starting'
  | 'recording'
  | 'processing'
  | 'permission_denied'
  | 'recording_failed'
  | 'upload_failed'
  | 'unsupported_media'
  | 'rate_limited'
  | 'timeout'
  | 'unclear_speech';

type VoiceRecordingSheetProps = {
  controls: CommandVoiceControls;
  disabled: boolean;
  onClose: () => void;
  visible: boolean;
};

const voiceRecordingOptions: RecordingOptions = {
  ...RecordingPresets.HIGH_QUALITY,
  extension: '.mp4',
  android: {
    ...RecordingPresets.HIGH_QUALITY.android,
    extension: '.mp4',
  },
  ios: {
    ...RecordingPresets.HIGH_QUALITY.ios,
    extension: '.mp4',
  },
};

export function VoiceRecordingSheet({
  controls,
  disabled,
  onClose,
  visible,
}: VoiceRecordingSheetProps) {
  const audioRecorder = useAudioRecorder(voiceRecordingOptions);
  const recorderState = useAudioRecorderState(audioRecorder, 250);
  const hasAutoStartedRef = useRef(false);
  const [phase, setPhase] = useState<VoiceRecordingPhase>('ready');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!visible) {
      hasAutoStartedRef.current = false;
      setPhase('ready');
      setErrorMessage(null);
      return;
    }

    if (!hasAutoStartedRef.current) {
      hasAutoStartedRef.current = true;
      void startRecording();
    }
  }, [visible]);

  useEffect(
    () => () => {
      if (audioRecorder.isRecording) {
        void audioRecorder.stop().catch(() => undefined);
      }
    },
    [audioRecorder]
  );

  const copy = getVoiceCopy(phase, errorMessage);

  async function startRecording() {
    if (disabled || phase === 'starting' || phase === 'processing') {
      return;
    }

    setErrorMessage(null);
    setPhase('starting');

    try {
      const permission = await requestRecordingPermissionsAsync();
      if (!permission.granted) {
        setPhase('permission_denied');
        return;
      }

      await audioRecorder.prepareToRecordAsync();
      audioRecorder.record();
      setPhase('recording');
    } catch {
      setPhase('recording_failed');
      setErrorMessage('Не удалось начать запись. Попробуй еще раз или введи команду текстом.');
    }
  }

  async function stopRecording() {
    if (phase !== 'recording') {
      return;
    }

    try {
      await audioRecorder.stop();
      const uri = audioRecorder.uri ?? recorderState.url;
      if (!uri) {
        setPhase('recording_failed');
        setErrorMessage('Запись не сохранила аудио. Попробуй еще раз или введи команду текстом.');
        return;
      }

      await transcribeRecording(createVoiceRecordingFile(uri));
    } catch {
      setPhase('recording_failed');
      setErrorMessage('Не удалось завершить запись. Попробуй еще раз или введи команду текстом.');
    }
  }

  async function transcribeRecording(recordingFile: VoiceTranscriptionFile) {
    setPhase('processing');
    setErrorMessage(null);

    try {
      const response = await controls.onTranscribeRecording(recordingFile);
      const transcript = response.transcript.trim();
      if (!transcript) {
        setPhase('unclear_speech');
        setErrorMessage('Не получилось разобрать короткую команду. Попробуй еще раз или введи ее текстом.');
        return;
      }

      controls.onTranscriptReady(transcript, response.traceId);
      setPhase('ready');
      onClose();
    } catch (error) {
      setPhase(resolveTranscriptionFailurePhase(error));
      setErrorMessage(formatVoiceTranscriptionError(error));
    }
  }

  async function cancelAndClose() {
    if (audioRecorder.isRecording) {
      await audioRecorder.stop().catch(() => undefined);
    }
    setErrorMessage(null);
    setPhase('ready');
    onClose();
  }

  return (
    <Modal
      animationType="slide"
      onRequestClose={cancelAndClose}
      transparent
      visible={visible}
    >
      <View style={styles.voiceSheetBackdrop}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Закрыть голосовой ввод"
          onPress={cancelAndClose}
          style={styles.voiceSheetScrim}
        />
        <View style={styles.voiceSheet}>
          <View style={styles.voiceSheetHandle} />
          <View style={styles.voiceSheetHeader}>
            <Text style={styles.voiceSheetTitle}>{copy.title}</Text>
            <Text style={styles.voiceSheetBody}>{copy.body}</Text>
          </View>

          {phase === 'starting' && (
            <View style={styles.voiceProcessingCard}>
              <Text style={styles.voiceStatusText}>Открываю микрофон...</Text>
            </View>
          )}

          {phase === 'recording' && (
            <View style={styles.voiceRecordingPanel}>
              <View style={styles.voicePulse} />
              <Text style={styles.voiceTimer}>{formatDuration(recorderState.durationMillis)}</Text>
              <Text style={styles.hintText}>Скажи одну короткую бытовую команду.</Text>
            </View>
          )}

          {phase === 'processing' && (
            <View style={styles.voiceProcessingCard}>
              <Text style={styles.voiceStatusText}>Готовлю текстовый черновик...</Text>
            </View>
          )}

          {isErrorPhase(phase) && (
            <View style={styles.voiceErrorCard}>
              <Text style={styles.feedbackTextError}>{copy.body}</Text>
            </View>
          )}

          <View style={styles.voiceSheetActions}>
            {phase === 'recording' ? (
              <>
                <Pressable
                  accessibilityRole="button"
                  onPress={cancelAndClose}
                  style={({ pressed }) => [
                    styles.secondaryButton,
                    styles.voiceSheetActionButton,
                    pressed && styles.buttonPressed,
                  ]}
                >
                  <Text style={styles.secondaryButtonText}>Отмена</Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  onPress={stopRecording}
                  style={({ pressed }) => [
                    styles.primaryButton,
                    styles.voiceSheetActionButton,
                    pressed && styles.buttonPressed,
                  ]}
                >
                  <Text style={styles.primaryButtonText}>Остановить</Text>
                </Pressable>
              </>
            ) : null}

            {(phase === 'ready' || isErrorPhase(phase)) && (
              <>
                <Pressable
                  accessibilityRole="button"
                  onPress={cancelAndClose}
                  style={({ pressed }) => [
                    styles.secondaryButton,
                    styles.voiceSheetActionButton,
                    pressed && styles.buttonPressed,
                  ]}
                >
                  <Text style={styles.secondaryButtonText}>Закрыть</Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  disabled={disabled}
                  onPress={startRecording}
                  style={({ pressed }) => [
                    styles.primaryButton,
                    styles.voiceSheetActionButton,
                    pressed && styles.buttonPressed,
                    disabled && styles.buttonDisabled,
                  ]}
                >
                  <Text style={styles.primaryButtonText}>Попробовать снова</Text>
                </Pressable>
              </>
            )}

            {(phase === 'starting' || phase === 'processing') && (
              <Pressable
                accessibilityRole="button"
                disabled
                style={[styles.primaryButton, styles.voiceSheetActionButton, styles.buttonDisabled]}
              >
                <Text style={styles.primaryButtonText}>
                  {phase === 'starting' ? 'Слушаю...' : 'Готовлю...'}
                </Text>
              </Pressable>
            )}
          </View>
        </View>
      </View>
    </Modal>
  );
}

function createVoiceRecordingFile(uri: string): VoiceTranscriptionFile {
  const extension = getRecordingExtension(uri);
  return {
    uri,
    name: `voice-command-${Date.now()}${extension}`,
    type: getRecordingMediaType(extension),
  };
}

function getRecordingExtension(uri: string): string {
  const normalized = uri.split(/[?#]/)[0]?.toLowerCase() ?? '';
  if (normalized.endsWith('.wav')) {
    return '.wav';
  }
  if (normalized.endsWith('.webm')) {
    return '.webm';
  }
  if (normalized.endsWith('.ogg')) {
    return '.ogg';
  }
  if (normalized.endsWith('.mp3')) {
    return '.mp3';
  }
  if (normalized.endsWith('.mp4')) {
    return '.mp4';
  }
  if (normalized.endsWith('.m4a')) {
    return '.m4a';
  }
  return Platform.OS === 'web' ? '.webm' : '.m4a';
}

function getRecordingMediaType(extension: string): string {
  switch (extension) {
    case '.wav':
      return 'audio/wav';
    case '.webm':
      return 'audio/webm';
    case '.ogg':
      return 'audio/ogg';
    case '.mp3':
      return 'audio/mp3';
    case '.mp4':
      return 'audio/mp4';
    case '.m4a':
    default:
      return 'audio/mp4';
  }
}

function resolveTranscriptionFailurePhase(error: unknown): VoiceRecordingPhase {
  if (error instanceof HomeTuskApiError) {
    const code = error.body.code ?? error.body.errorCode;
    if (error.status === 415 || code === 'unsupported_media') {
      return 'unsupported_media';
    }
    if (error.status === 429 || code === 'local_rate_limit') {
      return 'rate_limited';
    }
    if (error.status === 504 || code === 'timeout') {
      return 'timeout';
    }
  }
  return 'upload_failed';
}

function isErrorPhase(phase: VoiceRecordingPhase): boolean {
  return (
    phase === 'permission_denied' ||
    phase === 'recording_failed' ||
    phase === 'upload_failed' ||
    phase === 'unsupported_media' ||
    phase === 'rate_limited' ||
    phase === 'timeout' ||
    phase === 'unclear_speech'
  );
}

function getVoiceCopy(phase: VoiceRecordingPhase, errorMessage: string | null) {
  if (phase === 'starting') {
    return {
      title: 'Готовлю микрофон',
      body: 'Запись начнется сразу после разрешения доступа.',
    };
  }
  if (phase === 'recording') {
    return {
      title: 'Идет запись',
      body: 'Останови запись, когда команда закончена.',
    };
  }
  if (phase === 'processing') {
    return {
      title: 'Готовлю черновик',
      body: 'Команда не будет отправлена, пока ты сам не нажмешь отправку.',
    };
  }
  if (phase === 'permission_denied') {
    return {
      title: 'Микрофон закрыт',
      body: 'Разреши доступ к микрофону или введи команду текстом.',
    };
  }
  if (phase === 'recording_failed') {
    return {
      title: 'Запись не удалась',
      body: errorMessage ?? 'Попробуй записать еще раз или введи команду текстом.',
    };
  }
  if (phase === 'unsupported_media') {
    return {
      title: 'Формат не поддержан',
      body: errorMessage ?? 'Попробуй записать еще раз или введи команду текстом.',
    };
  }
  if (phase === 'rate_limited') {
    return {
      title: 'Голос позже',
      body: errorMessage ?? 'Введи команду текстом или попробуй голос позже.',
    };
  }
  if (phase === 'timeout') {
    return {
      title: 'Слишком долго',
      body: errorMessage ?? 'Попробуй еще раз или введи команду текстом.',
    };
  }
  if (phase === 'upload_failed') {
    return {
      title: 'Черновик не готов',
      body: errorMessage ?? 'Попробуй еще раз или введи команду текстом.',
    };
  }
  if (phase === 'unclear_speech') {
    return {
      title: 'Команда не разобрана',
      body: errorMessage ?? 'Попробуй еще раз или введи команду текстом.',
    };
  }
  return {
    title: 'Голосовая команда',
    body: 'Запиши короткую команду. Перед отправкой появится текстовый черновик.',
  };
}

function formatDuration(durationMillis: number): string {
  const totalSeconds = Math.max(0, Math.floor(durationMillis / 1000));
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, '0');
  const seconds = (totalSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}
