import { ActivityIndicator, Text, View } from 'react-native';

import { styles } from './styles';

export function LoadingPanel({ label }: { label: string }) {
  return (
    <View style={styles.statePanel}>
      <ActivityIndicator size="small" color="#1d7f68" />
      <Text style={styles.stateText}>{label}</Text>
    </View>
  );
}
