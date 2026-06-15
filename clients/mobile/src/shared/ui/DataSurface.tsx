import { Text, View, type ColorValue } from 'react-native';

import { styles } from './styles';

export function DataSurface({
  title,
  body,
  accent,
}: {
  title: string;
  body: string;
  accent: ColorValue;
}) {
  return (
    <View style={styles.dataPanel}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.dataCopy}>
        <Text style={styles.dataTitle}>{title}</Text>
        <Text style={styles.dataBody}>{body}</Text>
      </View>
    </View>
  );
}
