import { Pressable, Text, View } from 'react-native';

import { Mascot, type MascotMood } from './Mascot';
import { styles } from './styles';

type EmptyStateAction = {
  label: string;
  onPress: () => void;
};

type EmptyStateProps = {
  title: string;
  body: string;
  mascotMood?: MascotMood;
  action?: EmptyStateAction;
};

export function EmptyState({ title, body, mascotMood = 'idle', action }: EmptyStateProps) {
  return (
    <View style={styles.emptyStateCard}>
      <Mascot mood={mascotMood} size="large" />
      <View style={styles.emptyStateCopy}>
        <Text style={styles.emptyStateTitle}>{title}</Text>
        <Text style={styles.emptyStateBody}>{body}</Text>
      </View>
      {action && (
        <Pressable
          accessibilityRole="button"
          onPress={action.onPress}
          style={({ pressed }) => [styles.primaryButton, pressed && styles.buttonPressed]}
        >
          <Text style={styles.primaryButtonText}>{action.label}</Text>
        </Pressable>
      )}
    </View>
  );
}
