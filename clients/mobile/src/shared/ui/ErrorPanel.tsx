import { Pressable, Text, View } from 'react-native';

import { styles } from './styles';

export function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <View style={styles.statePanel}>
      <Text style={styles.errorText}>{message}</Text>
      <Pressable accessibilityRole="button" onPress={onRetry} style={styles.retryButton}>
        <Text style={styles.retryText}>Retry</Text>
      </Pressable>
    </View>
  );
}
