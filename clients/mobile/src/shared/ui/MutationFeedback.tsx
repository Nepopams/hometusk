import { Text, View } from 'react-native';

import { styles } from './styles';

export function MutationFeedback({ message, error }: { message: string | null; error: string | null }) {
  if (!message && !error) {
    return null;
  }

  return (
    <View style={[styles.feedbackPanel, error ? styles.feedbackPanelError : styles.feedbackPanelSuccess]}>
      <Text style={error ? styles.feedbackTextError : styles.feedbackTextSuccess}>
        {error ?? message}
      </Text>
    </View>
  );
}
