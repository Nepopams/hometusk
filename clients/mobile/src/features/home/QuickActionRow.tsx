import { Pressable, Text, View } from 'react-native';

import type { SurfaceKey } from '../../app/types';
import { styles } from '../../shared/ui/styles';

type QuickActionRowProps = {
  onNavigate: (surface: SurfaceKey) => void;
};

export function QuickActionRow({ onNavigate }: QuickActionRowProps) {
  return (
    <View style={styles.quickActionsRow}>
      <Pressable
        accessibilityRole="button"
        onPress={() => onNavigate('command')}
        style={({ pressed }) => [
          styles.primaryButton,
          styles.quickActionButton,
          pressed && styles.buttonPressed,
        ]}
      >
        <Text style={styles.primaryButtonText}>Открыть команды</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        onPress={() => onNavigate('tasks')}
        style={({ pressed }) => [
          styles.secondaryButton,
          styles.quickActionButton,
          pressed && styles.buttonPressed,
        ]}
      >
        <Text style={styles.secondaryButtonText}>Задачи</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        onPress={() => onNavigate('shopping')}
        style={({ pressed }) => [
          styles.secondaryButton,
          styles.quickActionButton,
          pressed && styles.buttonPressed,
        ]}
      >
        <Text style={styles.secondaryButtonText}>Покупки</Text>
      </Pressable>
    </View>
  );
}
