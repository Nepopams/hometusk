import { Text, View, type StyleProp, type ViewStyle } from 'react-native';

import { styles } from './styles';

export type MascotMood =
  | 'idle'
  | 'hello'
  | 'thinking'
  | 'success'
  | 'confirm'
  | 'confused'
  | 'reject'
  | 'degraded';

type MascotSize = 'small' | 'medium' | 'large';

export const mascotAssetNames: Record<MascotMood, string> = {
  idle: 'mascot_idle.png',
  hello: 'mascot_hello.png',
  thinking: 'mascot_thinking.png',
  success: 'mascot_success.png',
  confirm: 'mascot_confirm.png',
  confused: 'mascot_confused.png',
  reject: 'mascot_reject.png',
  degraded: 'mascot_degraded.png',
};

type MascotProps = {
  mood: MascotMood;
  size?: MascotSize;
  label?: string;
};

export function Mascot({ mood, size = 'medium', label }: MascotProps) {
  return (
    <View
      accessibilityLabel={label ?? `Mascot asset placeholder: ${mascotAssetNames[mood]}`}
      accessible
      style={[
        styles.mascotFrame,
        size === 'small' && styles.mascotFrameSmall,
        size === 'medium' && styles.mascotFrameMedium,
        size === 'large' && styles.mascotFrameLarge,
        mascotMoodStyle[mood],
      ]}
    >
      <Text style={styles.mascotFallbackLabel}>ASSET</Text>
      <Text style={styles.mascotFallbackLabel}>{mascotAssetNames[mood].replace('.png', '')}</Text>
    </View>
  );
}

const mascotMoodStyle: Record<MascotMood, StyleProp<ViewStyle>> = {
  idle: styles.mascotMoodIdle,
  hello: styles.mascotMoodHello,
  thinking: styles.mascotMoodThinking,
  success: styles.mascotMoodSuccess,
  confirm: styles.mascotMoodConfirm,
  confused: styles.mascotMoodConfused,
  reject: styles.mascotMoodReject,
  degraded: styles.mascotMoodDegraded,
};
