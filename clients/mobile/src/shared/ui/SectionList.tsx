import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import { styles } from './styles';

export function SectionList({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}
