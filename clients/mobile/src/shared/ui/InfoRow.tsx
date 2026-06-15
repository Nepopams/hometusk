import { Text, View, type ColorValue } from 'react-native';

import { styles } from './styles';

export function InfoRow({
  title,
  meta,
  accent,
}: {
  title: string;
  meta?: string | null;
  accent: ColorValue;
}) {
  return (
    <View style={styles.entityRow}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <View style={styles.entityCopy}>
        <Text style={styles.entityTitle}>{title}</Text>
        {meta && <Text style={styles.entityMeta}>{meta}</Text>}
      </View>
    </View>
  );
}

export function EmptyRow({ title, accent }: { title: string; accent: ColorValue }) {
  return (
    <View style={styles.entityRow}>
      <View style={[styles.checkDot, { backgroundColor: accent }]} />
      <Text style={styles.entityMeta}>{title}</Text>
    </View>
  );
}
